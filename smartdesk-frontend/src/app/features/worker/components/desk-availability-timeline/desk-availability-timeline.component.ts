import { Component, DestroyRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BookingService, SlotStatus } from '../../../../core/services/booking.service';
import { Booking } from '../../../../core/models';
import { localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
import { normalizeSlotTimeHm } from '../../../../core/utils/time.util';
import { Subscription, catchError, forkJoin, of } from 'rxjs';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
export type TimelineSlotStatus = 'free' | 'occupied' | 'selected' | 'conflict' | 'closed' | 'own-booking';
@Component({
    selector: 'app-desk-availability-timeline',
    standalone: true,
    imports: [CommonModule, SdIconComponent],
    templateUrl: './desk-availability-timeline.component.html',
    styleUrl: './desk-availability-timeline.component.scss'
})
export class DeskAvailabilityTimelineComponent implements OnChanges {
    @Input({ required: true })
    public deskId!: number;
    @Input({ required: true })
    public date!: string;
    @Input()
    public closedMask: boolean[] = [];
    @Input()
    public slotLabels: string[] = [];
    @Input()
    public initialRange: {
        start: string;
        end: string;
    } | null = null;
    @Input()
    public refreshTrigger = 0;
    @Output()
    public readonly onBook = new EventEmitter<{
        start: string;
        end: string;
    }>();
    private readonly bookingService = inject(BookingService);
    private readonly destroyRef = inject(DestroyRef);
    private availabilitySub?: Subscription;
    private activePointerId: number | null = null;
    private initialRangeSyncKey: string | null = null;
    protected readonly slots = signal<SlotStatus[]>([]);
    protected readonly slotsLoading = signal(false);
    protected readonly ownBookingMask = signal<boolean[]>([]);
    protected readonly selecting = signal(false);
    protected readonly startIndex = signal<number | null>(null);
    protected readonly endIndex = signal<number | null>(null);
    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.deskId || !this.date) {
            return;
        }
        const deskOrDateChanged = !!(changes['deskId'] || changes['date']);
        const refreshBump = !!changes['refreshTrigger'] &&
            changes['refreshTrigger'].previousValue !== changes['refreshTrigger'].currentValue;
        const initialRangeChanged = !!changes['initialRange'];
        if (!deskOrDateChanged && !refreshBump && !initialRangeChanged) {
            return;
        }
        if (initialRangeChanged && !deskOrDateChanged && !refreshBump) {
            this.clearSelectionIfInvalid();
            this.syncInitialRange(this.slots());
            return;
        }
        if (deskOrDateChanged) {
            this.initialRangeSyncKey = null;
            this.startIndex.set(null);
            this.endIndex.set(null);
            this.selecting.set(false);
            this.activePointerId = null;
            this.slots.set([]);
            this.ownBookingMask.set([]);
        }
        this.availabilitySub?.unsubscribe();
        this.slotsLoading.set(true);
        this.availabilitySub = forkJoin({
            slotList: this.bookingService.getSlotAvailability(this.deskId, this.date).pipe(catchError(() => of<SlotStatus[]>([]))),
            bookings: this.bookingService.getMyBookings().pipe(catchError(() => of<Booking[]>([])))
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ slotList, bookings }) => {
            this.slots.set(slotList);
            this.applyOwnBookingMask(slotList, bookings);
            this.slotsLoading.set(false);
            const syncKey = `${this.deskId}|${this.date}`;
            if (this.initialRange && this.initialRangeSyncKey !== syncKey) {
                this.syncInitialRange(slotList);
                this.initialRangeSyncKey = syncKey;
            }
        });
    }
    private applyOwnBookingMask(slotList: SlotStatus[], bookings: Booking[]): void {
        this.ownBookingMask.set(this.buildOwnBookingMask(slotList, bookings));
    }
    private bookingMatchesTimelineDate(booking: Booking): boolean {
        const booked = (booking.bookedDay || '').trim();
        if (booked === this.date) {
            return true;
        }
        if (booking.startTime) {
            const d = new Date(booking.startTime);
            if (!Number.isNaN(d.getTime())) {
                return localCalendarDateIsoFromDate(d) === this.date;
            }
        }
        return false;
    }
    private activeBookingsForDay(bookings: Booking[]): Booking[] {
        return bookings.filter((b) => {
            const status = (b.status || '').trim().toUpperCase();
            if (status === 'CANCELLED') {
                return false;
            }
            return this.bookingMatchesTimelineDate(b);
        });
    }
    private buildOwnBookingMask(slotList: SlotStatus[], bookings: Booking[]): boolean[] {
        const dayBookings = this.activeBookingsForDay(bookings);
        return slotList.map((slot) => {
            const slotStartHm = this.normalizeSlotTimeLabel(slot.time);
            const slotEndHm = this.addThirtyMinutes(slotStartHm);
            const slotStartMs = this.hmOnDateToMs(this.date, slotStartHm);
            const slotEndMs = this.hmOnDateToMs(this.date, slotEndHm);
            if (!Number.isFinite(slotStartMs) || !Number.isFinite(slotEndMs)) {
                return false;
            }
            return dayBookings.some((booking) => {
                const bookingStartMs = new Date(booking.startTime).getTime();
                const bookingEndMs = new Date(booking.endTime).getTime();
                if (!Number.isFinite(bookingStartMs) || !Number.isFinite(bookingEndMs)) {
                    return false;
                }
                return slotStartMs < bookingEndMs && slotEndMs > bookingStartMs;
            });
        });
    }
    private bookingStartHm(booking: Booking): string {
        const iso = (booking.startTime ?? '').trim();
        if (iso.includes('T')) {
            const hm = iso.split('T')[1]?.slice(0, 5) ?? '';
            if (hm) {
                return this.normalizeSlotTimeLabel(hm);
            }
        }
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) {
            return '';
        }
        return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    }
    private hmOnDateToMs(dayIso: string, hm: string): number {
        const [h, m] = hm.split(':').map(Number);
        if (!Number.isFinite(h) || !Number.isFinite(m)) {
            return Number.NaN;
        }
        return new Date(`${dayIso}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`).getTime();
    }
    protected isOwnBookingAtTimelineIndex(i: number): boolean {
        return !!this.ownBookingMask()[i];
    }
    private initialRangeStartHm(): string | null {
        const raw = this.initialRange?.start?.trim();
        if (!raw) {
            return null;
        }
        const hm = raw.includes('T') ? (raw.split('T')[1]?.slice(0, 5) ?? '') : raw.slice(0, 5);
        return hm ? this.normalizeSlotTimeLabel(hm) : null;
    }
    private initialRangeStartIndex(): number | null {
        const hm = this.initialRangeStartHm();
        if (!hm) {
            return null;
        }
        const idx = this.slots().findIndex((s) => this.slotTimeLabel(s.time) === hm);
        return idx >= 0 ? idx : null;
    }
    private minSelectableIndex(): number {
        return this.initialRangeStartIndex() ?? 0;
    }
    private clampSelectionIndices(startIdx: number, endIdx: number): {
        start: number;
        end: number;
    } {
        const floor = this.minSelectableIndex();
        const start = Math.max(startIdx, floor);
        const end = Math.max(endIdx, floor);
        return start <= end ? { start, end } : { start: end, end: start };
    }
    private isBackwardExtensionBlocked(anchorIndex: number, targetIndex: number): boolean {
        const rangeStartIdx = this.initialRangeStartIndex();
        if (rangeStartIdx === null) {
            return false;
        }
        return anchorIndex >= rangeStartIdx && targetIndex < rangeStartIdx;
    }
    private applyRangeExtension(anchorIndex: number, targetIndex: number): boolean {
        if (this.isBackwardExtensionBlocked(anchorIndex, targetIndex)) {
            return false;
        }
        const min = Math.max(Math.min(anchorIndex, targetIndex), this.minSelectableIndex());
        const max = Math.max(anchorIndex, targetIndex);
        for (let i = min; i <= max; i++) {
            if (this.isUnavailableAtTimelineIndex(i)) {
                return false;
            }
        }
        const clamped = this.clampSelectionIndices(anchorIndex, targetIndex);
        this.startIndex.set(clamped.start);
        this.endIndex.set(clamped.end);
        return true;
    }
    private clearSelectionIfInvalid(): void {
        const b = this.selectionBounds();
        if (!b) {
            return;
        }
        for (let i = b.min; i <= b.max; i++) {
            if (this.isUnavailableAtTimelineIndex(i)) {
                this.startIndex.set(null);
                this.endIndex.set(null);
                this.selecting.set(false);
                return;
            }
        }
    }
    private syncInitialRange(slotList: SlotStatus[]): void {
        if (!this.initialRange)
            return;
        const startT = this.initialRange.start.split('T')[1]?.slice(0, 5);
        const endT = this.initialRange.end.split('T')[1]?.slice(0, 5);
        if (!startT || !endT)
            return;
        let startIdx = slotList.findIndex((s) => this.slotTimeLabel(s.time) === startT);
        const lastIncludedTime = this.shiftHmBy(endT, -30);
        let endIdx = slotList.findIndex((s) => this.slotTimeLabel(s.time) === lastIncludedTime);
        const floor = this.minSelectableIndex();
        if (startIdx >= 0 && startIdx < floor) {
            startIdx = floor;
        }
        if (endIdx >= 0 && endIdx < floor) {
            endIdx = floor;
        }
        if (startIdx >= 0 && endIdx >= startIdx) {
            let spansBlocked = false;
            for (let i = startIdx; i <= endIdx; i++) {
                if (this.isUnavailableAtTimelineIndex(i)) {
                    spansBlocked = true;
                    break;
                }
            }
            if (!spansBlocked) {
                const clamped = this.clampSelectionIndices(startIdx, endIdx);
                this.startIndex.set(clamped.start);
                this.endIndex.set(clamped.end);
            }
        }
    }
    private shiftHmBy(hm: string, delta: number): string {
        const [h, m] = hm.split(':').map(Number);
        const total = h * 60 + m + delta;
        const nh = Math.floor(total / 60);
        const nm = total % 60;
        return `${String(nh).padStart(2, '0')}:${String(nm).padStart(2, '0')}`;
    }
    private closedMaskIndexForTimelineIndex(i: number): number {
        const slotList = this.slots();
        const slot = slotList[i];
        if (!slot) {
            return i;
        }
        const hm = this.normalizeSlotTimeLabel(slot.time);
        if (this.slotLabels?.length) {
            const idx = this.slotLabels.indexOf(hm);
            if (idx >= 0) {
                return idx;
            }
        }
        return i;
    }
    protected isOfficeClosedAtTimelineIndex(i: number): boolean {
        const mi = this.closedMaskIndexForTimelineIndex(i);
        return !!this.closedMask[mi];
    }
    private isTimelineDateToday(): boolean {
        return this.date === localCalendarDateIsoFromDate(new Date());
    }
    private isPastSlotAtTimelineIndex(i: number): boolean {
        if (!this.isTimelineDateToday()) {
            return false;
        }
        const slot = this.slots()[i];
        if (!slot) {
            return false;
        }
        const slotStartMs = this.hmOnDateToMs(this.date, this.normalizeSlotTimeLabel(slot.time));
        return Number.isFinite(slotStartMs) && slotStartMs <= Date.now();
    }
    protected isUnavailableAtTimelineIndex(i: number): boolean {
        return (this.isOfficeClosedAtTimelineIndex(i) ||
            this.isPastSlotAtTimelineIndex(i) ||
            this.isOwnBookingAtTimelineIndex(i));
    }
    protected isSlotBusyAtTimelineIndex(i: number): boolean {
        const raw = this.slots()[i]?.status;
        if (raw == null) {
            return false;
        }
        const s = String(raw).trim().toLowerCase();
        return s === 'busy' || s === 'occupied';
    }
    protected isSlotApiFreeAtTimelineIndex(i: number): boolean {
        const raw = String(this.slots()[i]?.status ?? '').trim().toLowerCase();
        return raw === 'free' || raw === '';
    }
    protected availabilityDaySummary(): {
        free: number;
        occupied: number;
        unavailable: number;
        ownBooking: number;
    } {
        const rows = this.visibleSlotRows();
        let free = 0;
        let occupied = 0;
        let unavailable = 0;
        let ownBooking = 0;
        for (const { index: i } of rows) {
            if (this.isOwnBookingAtTimelineIndex(i)) {
                ownBooking++;
            }
            else if (this.isUnavailableAtTimelineIndex(i)) {
                unavailable++;
            }
            else if (this.isSlotBusyAtTimelineIndex(i)) {
                occupied++;
            }
            else {
                free++;
            }
        }
        return { free, occupied, unavailable, ownBooking };
    }
    protected visibleSlotRows(): Array<{
        slot: SlotStatus;
        index: number;
    }> {
        return this.slots()
            .map((slot, index) => ({ slot, index }))
            .filter(({ index }) => !this.isOfficeClosedAtTimelineIndex(index));
    }
    protected summaryIsBlocking(): boolean {
        return (this.startIndex() !== null &&
            this.endIndex() !== null &&
            !this.bookingReady());
    }
    protected handleSlotActivate(index: number): void {
        if (index < this.minSelectableIndex() || this.isUnavailableAtTimelineIndex(index))
            return;
        const start = this.startIndex();
        const end = this.endIndex();
        if (start !== null && start === end && index !== start) {
            if (this.applyRangeExtension(start, index)) {
                this.selecting.set(false);
            }
            return;
        }
        this.selecting.set(true);
        this.startIndex.set(index);
        this.endIndex.set(index);
    }
    protected extendSelection(index: number): void {
        if (!this.selecting() || index < this.minSelectableIndex() || this.isUnavailableAtTimelineIndex(index))
            return;
        const start = this.startIndex();
        if (start === null) {
            return;
        }
        this.applyRangeExtension(start, index);
    }
    protected endSelection(): void {
        this.selecting.set(false);
    }
    protected onSlotPointerDown(index: number, event: PointerEvent): void {
        if (event.pointerType === 'mouse' && event.button !== 0)
            return;
        if (this.isUnavailableAtTimelineIndex(index))
            return;
        this.activePointerId = event.pointerId;
        this.handleSlotActivate(index);
    }
    protected onSlotPointerEnter(index: number): void {
        if (this.activePointerId === null || !this.selecting())
            return;
        this.extendSelection(index);
    }
    @HostListener('document:pointerup', ['$event'])
    protected onDocumentPointerUp(event: PointerEvent): void {
        if (this.activePointerId !== null && event.pointerId === this.activePointerId) {
            this.activePointerId = null;
        }
        this.endSelection();
    }
    @HostListener('document:pointercancel', ['$event'])
    protected onDocumentPointerCancel(event: PointerEvent): void {
        if (this.activePointerId !== null && event.pointerId === this.activePointerId) {
            this.activePointerId = null;
        }
        this.endSelection();
    }
    protected onSlotKeydown(event: KeyboardEvent, index: number): void {
        if (event.key !== 'Enter' && event.key !== ' ') {
            return;
        }
        event.preventDefault();
        this.handleSlotActivate(index);
        this.endSelection();
    }
    protected getSlotStatus(index: number): TimelineSlotStatus {
        if (this.isOfficeClosedAtTimelineIndex(index) || this.isPastSlotAtTimelineIndex(index)) {
            return 'closed';
        }
        if (this.isOwnBookingAtTimelineIndex(index)) {
            return 'own-booking';
        }
        const isBusy = this.isSlotBusyAtTimelineIndex(index);
        const isSelected = this.isInSelection(index);
        if (isSelected) {
            return isBusy ? 'conflict' : 'selected';
        }
        return isBusy ? 'occupied' : 'free';
    }
    protected slotStatusBarLabel(status: TimelineSlotStatus): string {
        const map: Record<TimelineSlotStatus, string> = {
            free: 'Libero',
            occupied: 'Occupato',
            selected: 'Selezionato',
            conflict: 'Conflitto',
            closed: 'Non disponibile',
            'own-booking': 'Prenotazione Già Attiva'
        };
        return map[status];
    }
    protected slotStatusItalian(index: number): string {
        const map: Record<TimelineSlotStatus, string> = {
            free: 'libero',
            occupied: 'occupato',
            selected: 'selezionato',
            conflict: 'conflitto',
            closed: 'non disponibile',
            'own-booking': 'tua prenotazione'
        };
        return map[this.getSlotStatus(index)];
    }
    protected isInSelection(index: number): boolean {
        const start = this.startIndex();
        const end = this.endIndex();
        if (start === null || end === null)
            return false;
        const min = Math.max(Math.min(start, end), this.minSelectableIndex());
        const max = Math.max(start, end);
        return index >= min && index <= max;
    }
    private selectionBounds(): {
        min: number;
        max: number;
    } | null {
        const start = this.startIndex();
        const end = this.endIndex();
        if (start === null || end === null)
            return null;
        const min = Math.max(Math.min(start, end), this.minSelectableIndex());
        const max = Math.max(start, end);
        return min <= max ? { min, max } : null;
    }
    private selectionSpansUnavailable(): boolean {
        const b = this.selectionBounds();
        if (!b)
            return false;
        for (let i = b.min; i <= b.max; i++) {
            if (this.isUnavailableAtTimelineIndex(i))
                return true;
        }
        return false;
    }
    private selectionSpansBusy(): boolean {
        const b = this.selectionBounds();
        if (!b)
            return false;
        for (let i = b.min; i <= b.max; i++) {
            if (this.isSlotBusyAtTimelineIndex(i))
                return true;
        }
        return false;
    }
    private selectionSpansOwnBooking(): boolean {
        const b = this.selectionBounds();
        if (!b)
            return false;
        for (let i = b.min; i <= b.max; i++) {
            if (this.isOwnBookingAtTimelineIndex(i))
                return true;
        }
        return false;
    }
    protected bookingReady(): boolean {
        const b = this.selectionBounds();
        const slotList = this.slots();
        if (!b || !slotList.length) {
            return false;
        }
        for (let i = b.min; i <= b.max; i++) {
            if (this.isUnavailableAtTimelineIndex(i))
                return false;
            if (!this.isSlotApiFreeAtTimelineIndex(i))
                return false;
        }
        return true;
    }
    protected ownBookingConflictMessage(): string {
        return 'Prenotazione Già Attiva.';
    }
    protected showOwnBookingConflictAlert(): boolean {
        return !this.selectionSpansUnavailable() && this.selectionSpansOwnBooking();
    }
    protected deskOccupancyConflictMessage(): string {
        return 'Impossibile prenotare: la postazione è occupata in parte della fascia selezionata.';
    }
    protected showDeskOccupancyConflictAlert(): boolean {
        return !this.selectionSpansUnavailable() && this.selectionSpansBusy();
    }
    protected bookButtonAriaDescribedBy(): string {
        const ids = ['sd-timeline-summary-text'];
        if (this.showOwnBookingConflictAlert()) {
            ids.push('sd-timeline-own-booking-alert');
        }
        if (this.showDeskOccupancyConflictAlert()) {
            ids.push('sd-timeline-conflict-alert');
        }
        return ids.join(' ');
    }
    protected timelineFooterIntervalText(): string {
        if (this.slotsLoading()) {
            return '';
        }
        const slotList = this.slots();
        if (!slotList.length) {
            return 'Impossibile caricare la disponibilità per questa data. Riprova più tardi.';
        }
        const start = this.startIndex();
        const end = this.endIndex();
        if (start === null || end === null) {
            return 'Primo clic: inizio fascia. Secondo clic: fine fascia. Puoi anche tenere premuto e trascinare.';
        }
        const selMin = Math.max(Math.min(start, end), this.minSelectableIndex());
        const selMax = Math.max(start, end);
        const fromSel = slotList[selMin]?.time;
        const toSel = slotList[selMax + 1]?.time ?? this.addThirtyMinutes(slotList[selMax]?.time ?? '');
        const selLine = fromSel && toSel
            ? `Intervallo selezionato (righe evidenziate): ${this.slotTimeLabel(fromSel)} – ${this.slotTimeLabel(toSel)}`
            : '';
        if (this.selectionSpansUnavailable()) {
            return 'Questa fascia non e prenotabile. Scegli un orario disponibile.';
        }
        if (this.showOwnBookingConflictAlert()) {
            return selLine;
        }
        if (this.showDeskOccupancyConflictAlert()) {
            return selLine;
        }
        if (!this.bookingReady()) {
            return selLine;
        }
        const tfL = this.slotTimeLabel(fromSel ?? '');
        const ttL = this.slotTimeLabel(toSel ?? '');
        return `${selLine}`;
    }
    protected selectedRangeLabel(): string {
        const interval = this.timelineFooterIntervalText();
        if (this.showOwnBookingConflictAlert()) {
            return `${interval} ${this.ownBookingConflictMessage()}`.trim();
        }
        if (this.showDeskOccupancyConflictAlert()) {
            return `${interval} ${this.deskOccupancyConflictMessage()}`.trim();
        }
        return interval;
    }
    protected bookSelection(): void {
        if (!this.bookingReady()) {
            return;
        }
        const b = this.selectionBounds()!;
        const slotList = this.slots();
        const startSlot = slotList[b.min]?.time;
        const endSlot = slotList[b.max + 1]?.time ?? this.addThirtyMinutes(slotList[b.max]?.time ?? '');
        if (!startSlot || !endSlot) {
            return;
        }
        const startHm = this.slotTimeLabel(startSlot);
        const endHm = this.slotTimeLabel(endSlot);
        this.onBook.emit({
            start: `${this.date}T${startHm}:00`,
            end: `${this.date}T${endHm}:00`
        });
    }
    protected slotTimeLabel(raw: string): string {
        return this.normalizeSlotTimeLabel(raw);
    }
    protected slotRangeDisplayLabel(index: number): string {
        const list = this.slots();
        const row = list[index];
        if (!row) {
            return '';
        }
        const start = this.slotTimeLabel(row.time);
        const nextRaw = list[index + 1]?.time;
        const endLabel = nextRaw
            ? this.slotTimeLabel(nextRaw)
            : this.slotTimeLabel(this.addThirtyMinutes(row.time));
        return `${start}\u2013${endLabel}`;
    }
    private normalizeSlotTimeLabel(raw: string): string {
        try {
            return normalizeSlotTimeHm(raw ?? '');
        }
        catch {
            return '';
        }
    }
    private addThirtyMinutes(time: string): string {
        const [hh, mm] = time.split(':').map((n) => Number(n));
        if (!Number.isFinite(hh) || !Number.isFinite(mm)) {
            return '';
        }
        const minutes = hh * 60 + mm + 30;
        const nextHours = Math.floor(minutes / 60);
        const nextMinutes = minutes % 60;
        return `${String(nextHours).padStart(2, '0')}:${String(nextMinutes).padStart(2, '0')}`;
    }
}
