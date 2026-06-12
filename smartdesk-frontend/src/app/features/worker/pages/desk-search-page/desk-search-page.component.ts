import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, inject, NgZone, OnInit, signal, computed, TemplateRef, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, of } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Booking, BookingRequest, Desk, SearchCriteria } from '../../../../core/models';
import { BookingService, SlotStatus, WorkerSpace } from '../../../../core/services/booking.service';
import { ToastService } from '../../../../core/services/toast.service';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { DeskAvailabilityTimelineComponent } from '../../components/desk-availability-timeline/desk-availability-timeline.component';
import { DeskCardPresentation } from '../desk-card-presentation';
import { formatAmenityTag, sortAmenityPreviewTags } from '../desk-amenity-labels.util';
import { AmenityFilterComponent } from './components/amenity-filter/amenity-filter.component';
import { computeAvailableAmenityTags, pruneSelectedAmenityTags as pruneSelectedAmenityTagsUtil } from './components/amenity-filter/amenity-filter.util';
import { buildCalendarDays, type CalendarDay } from '../desk-search-calendar.util';
import {
    SLOT_COUNT,
    SLOT_LABELS,
    addMinutesToHm,
    endHmAfterSlot,
    formatDuration,
    minutesToHm,
    parseHmToMins,
    rowToFreeMask,
    slotsRangeHasBusyApi,
    snapEndHmExclusive,
    snapStartHm
} from '../desk-slot-grid.util';
import { classifyBookingError } from '../booking-error-message.util';
import {
    bookingHoursBlockedNotice as computeBookingHoursBlockedNotice,
    computeOfficeClosedMask as buildOfficeClosedMask,
    effectiveBookingBoundsForTargetDate as computeEffectiveBookingBounds,
    officeHoursBanner as computeOfficeHoursBanner,
    officeOpeningBoundsForTargetDate as computeOfficeOpeningBounds
} from '../office-hours.util';
import { localCalendarDateIso } from '../../../../core/utils/date.util';
import { DeskSearchSessionStore, type DeskViewModel } from '../desk-search-session.store';
import { ScopeSelectorComponent } from './components/scope-selector/scope-selector.component';
import { TimeRangePickerComponent, type TimeRangePickerChange } from './components/time-range-picker/time-range-picker.component';
export type { DeskViewModel } from '../desk-search-session.store';
export type CalendarDayVm = CalendarDay;
export type DeskSlotUiKind = 'occupied' | 'free' | 'selected' | 'closed';
@Component({
    selector: 'app-desk-search-page',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    providers: [DeskSearchSessionStore],
    imports: [
        CommonModule,
        MdbCollapseModule,
        MdbModalModule,
        MdbRippleModule,
        SdIconComponent,
        SdModalHeaderComponent,
        DeskAvailabilityTimelineComponent,
        TimeRangePickerComponent,
        AmenityFilterComponent,
        ScopeSelectorComponent,
        RouterLink
    ],
    templateUrl: './desk-search-page.component.html',
    styleUrl: './desk-search-page.component.scss'
})
export class DeskSearchPageComponent implements OnInit {
    @ViewChild('availabilityModal')
    availabilityModal!: TemplateRef<unknown>;
    @ViewChild(TimeRangePickerComponent)
    private timeRangePicker?: TimeRangePickerComponent;
    @ViewChild(AmenityFilterComponent)
    private amenityFilter?: AmenityFilterComponent;
    protected modalRef: MdbModalRef<unknown> | null = null;
    private readonly modalService = inject(MdbModalService);
    private readonly ngZone = inject(NgZone);
    protected readonly deskCard = DeskCardPresentation;
    protected readonly slotLabels = SLOT_LABELS;
    private readonly bookingService = inject(BookingService);
    private readonly toast = inject(ToastService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly destroyRef = inject(DestroyRef);
    private readonly session = inject(DeskSearchSessionStore);
    protected readonly deskResultsLoading = this.session.deskResultsLoading;
    protected targetDate = localCalendarDateIso();
    protected slotStart = '';
    protected slotEnd = '';
    protected selectedTags: string[] = [];
    protected includeMaintenance = false;
    protected errorMsg = '';
    protected warningMsg = '';
    protected isLoading = false;
    protected readonly bookingInProgressId = signal<number | null>(null);
    protected spaces: WorkerSpace[] = [];
    protected cities: string[] = [];
    protected readonly selectedCity = signal('');
    protected readonly selectedRoomKey = signal<number | null>(null);
    protected readonly selectedSpaceId = signal<number | null>(null);
    protected selectedDeskForTimeline: DeskViewModel | null = null;
    protected availabilityRefreshCounter = 0;
    protected showWaitlistInline = false;
    protected timelineSelfOverlapHint: string | null = null;
    protected timelineGenericBookError: string | null = null;
    protected calendarDays: CalendarDayVm[] = [];
    protected rangeStartIdx: number | null = null;
    protected rangeEndIdx: number | null = null;
    protected slotKinds: DeskSlotUiKind[] = Array.from({ length: SLOT_COUNT }, () => 'free' as DeskSlotUiKind);
    protected readonly slotsLoading = this.session.slotsLoading;
    protected readonly waitlistDeskId = this.session.waitlistDeskId;
    protected readonly queueJoined = this.session.queueJoined;
    protected readonly myBookings = signal<Booking[]>([]);
    protected readonly availabilityShown = this.session.availabilityShown;
    protected readonly searchOutdated = this.session.searchOutdated;
    protected get desks(): DeskViewModel[] {
        return this.session.desks();
    }
    protected get officeDesks(): Desk[] {
        return this.session.officeDesks();
    }
    protected get selectedClosure() {
        return this.session.selectedClosure();
    }
    protected get deskSlotMaskById(): Map<number, boolean[]> {
        return this.session.deskSlotMaskById();
    }
    protected get aggregateSlotFree(): boolean[] {
        return this.session.aggregateSlotFree();
    }
    protected get officeClosedSlotMask(): boolean[] {
        return this.session.officeClosedSlotMask();
    }
    private lastSearchedSlotStart = '';
    private lastSearchedSlotEnd = '';
    public ngOnInit(): void {
        this.session.bindHost({
            slotRangeOk: () => this.slotRangeOk(),
            orderedRange: () => this.orderedRange(),
            hasCompleteSlotRange: () => this.hasCompleteSlotRange(),
            isSearchSlotUnavailable: (idx) => this.isSearchSlotUnavailable(idx),
            computeOfficeClosedMask: () => this.computeOfficeClosedMask(),
            buildDeskSearchCriteria: () => this.buildDeskSearchCriteria(),
            reapplySlotRangeAfterOfficeContextChange: () => this.reapplySlotRangeAfterOfficeContextChange(),
            pruneSelectedAmenityTags: () => this.pruneSelectedAmenityTags(),
            onDeskSearchError: (message) => {
                this.errorMsg = message;
            },
            onDeskSearchToast: (showToast) => {
                if (showToast) {
                    this.toast.success('Nuova ricerca effettuata.');
                }
            },
            rebuildSlotKinds: () => this.rebuildSlotKinds(),
            rememberLastSearchSlotRange: () => this.rememberLastSearchSlotRange(),
            loadMyBookings: () => this.loadMyBookings(),
            markForCheck: () => this.cdr.markForCheck(),
            getLoadDesksParams: () => ({
                selectedTags: this.selectedTags,
                includeMaintenance: this.includeMaintenance
            }),
            onBeforeLoadDesks: () => {
                this.errorMsg = '';
            }
        });
        this.rebuildCalendar();
        this.loadSpaces();
        this.loadMyBookings();
        this.rebuildSlotKinds();
    }
    private loadMyBookings(): void {
        this.bookingService
            .getMyBookings()
            .pipe(catchError(() => of([] as Booking[])), takeUntilDestroyed(this.destroyRef))
            .subscribe((list) => {
            this.myBookings.set(list);
            this.cdr.markForCheck();
        });
    }
    private rebuildSlotKinds(): void {
        this.slotKinds = SLOT_LABELS.map((_, i) => this.computeSlotUiKind(i));
    }
    private loadSpaces(): void {
        this.isLoading = true;
        this.bookingService
            .getWorkerSpaces()
            .pipe(finalize(() => {
            this.isLoading = false;
            this.cdr.markForCheck();
        }), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (spaces) => {
                this.spaces = spaces;
                this.cities = Array.from(new Set(spaces.map((s) => s.city).filter(Boolean))).sort((a, b) => a.localeCompare(b));
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.errorMsg = err.message || 'Impossibile caricare gli spazi.';
                this.cdr.markForCheck();
            }
        });
    }
    protected rebuildCalendar(): void {
        const now = new Date();
        const todayIso = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
        this.calendarDays = buildCalendarDays(todayIso);
    }
    protected hasSpaceReviewAverage(rating: number | null | undefined): boolean {
        return rating != null && Number.isFinite(rating);
    }
    protected formatSpaceReviewAverage(rating: number): string {
        return rating.toFixed(1);
    }
    protected spaceReviewRatingCaption(rating: number): string {
        return `${this.formatSpaceReviewAverage(rating)} / 5 - Media Recensioni`;
    }
    protected deskRoomKey(desk: Desk): number {
        return desk.roomID ?? 0;
    }
    protected desksFilteredByRoom(): DeskViewModel[] {
        if (this.selectedRoomKey() === null)
            return this.desks;
        return this.desks.filter((d) => this.deskRoomKey(d) === this.selectedRoomKey());
    }
    protected editCity(): void {
        this.selectedCity.set('');
        this.selectedSpaceId.set(null);
        this.resetBookingUi();
    }
    protected editOffice(): void {
        this.selectedSpaceId.set(null);
        this.resetBookingUi();
    }
    protected onCitySelected(): void {
        this.selectedSpaceId.set(null);
        this.resetBookingUi();
    }
    private refreshUiAfterModalSelection(): void {
        queueMicrotask(() => {
            this.ngZone.run(() => this.cdr.detectChanges());
        });
    }
    protected onScopeCityChosen(city: string): void {
        this.selectedCity.set(city);
        this.onCitySelected();
        this.refreshUiAfterModalSelection();
    }
    protected onScopeOfficeChosen(spaceId: number): void {
        this.selectedSpaceId.set(spaceId);
        this.onOfficeSelected();
        this.refreshUiAfterModalSelection();
    }
    protected onScopeRoomChosen(roomKey: number | null): void {
        this.selectedRoomKey.set(roomKey);
        this.refreshUiAfterModalSelection();
    }
    protected closeTimePicker(): void {
        this.timeRangePicker?.closeTimePicker();
    }
    protected onTimeRangePickerChange(change: TimeRangePickerChange): void {
        this.slotStart = change.slotStart;
        this.slotEnd = change.slotEnd;
        this.syncManualTimeToRange();
        this.markSearchCriteriaChanged();
        this.refreshUiAfterModalSelection();
    }
    protected bookingBoundsForTimePicker(): [
        number,
        number
    ] | null {
        return this.effectiveBookingBoundsForTargetDate();
    }
    private rememberLastSearchSlotRange(): void {
        this.lastSearchedSlotStart = this.slotStart;
        this.lastSearchedSlotEnd = this.slotEnd;
    }
    private clearLastSearchSlotRange(): void {
        this.lastSearchedSlotStart = '';
        this.lastSearchedSlotEnd = '';
    }
    private slotRangeDiffersFromLastSearch(): boolean {
        if (!this.lastSearchedSlotStart || !this.lastSearchedSlotEnd) {
            return true;
        }
        return this.slotStart !== this.lastSearchedSlotStart || this.slotEnd !== this.lastSearchedSlotEnd;
    }
    private markSearchCriteriaChanged(): void {
        if (!this.availabilityShown() || !this.slotRangeDiffersFromLastSearch()) {
            return;
        }
        this.searchOutdated.set(true);
        this.session.clearDesks();
        this.cdr.markForCheck();
    }
    protected bookingHoursAvailable(): boolean {
        return this.step2Done() && this.effectiveBookingBoundsForTargetDate() !== null;
    }
    private commitSlotRangeFromHm(startHm: string, endExclusiveHm: string): boolean {
        const startIdx = SLOT_LABELS.indexOf(startHm);
        const lastIncluded = addMinutesToHm(endExclusiveHm, -30);
        const endIdx = lastIncluded ? SLOT_LABELS.indexOf(lastIncluded) : -1;
        if (startIdx < 0 || endIdx < 0 || endIdx < startIdx) {
            return false;
        }
        this.slotStart = startHm;
        this.slotEnd = endExclusiveHm;
        this.rangeStartIdx = startIdx;
        this.rangeEndIdx = endIdx;
        return true;
    }
    private clearSlotRange(): void {
        this.slotStart = '';
        this.slotEnd = '';
        this.rangeStartIdx = null;
        this.rangeEndIdx = null;
        this.rebuildSlotKinds();
    }
    private applyDefaultRangeFromOfficeHours(): void {
        const bounds = this.effectiveBookingBoundsForTargetDate();
        if (!bounds) {
            this.clearSlotRange();
            return;
        }
        const [open, close] = bounds;
        const startHm = snapStartHm(minutesToHm(open));
        let endExclusiveHm = snapEndHmExclusive(minutesToHm(close));
        const startM = parseHmToMins(startHm);
        let endM = parseHmToMins(endExclusiveHm);
        if (!Number.isFinite(startM) || !Number.isFinite(endM) || endM <= startM) {
            endM = Math.min(close, startM + 30);
            endExclusiveHm = snapEndHmExclusive(minutesToHm(endM));
        }
        if (!this.commitSlotRangeFromHm(startHm, endExclusiveHm)) {
            this.clearSlotRange();
            return;
        }
        this.rebuildSlotKinds();
    }
    private reapplySlotRangeAfterOfficeContextChange(): void {
        if (!this.effectiveBookingBoundsForTargetDate()) {
            this.clearSlotRange();
            if (!this.session.isRefreshSearchAfterSlotsLoad()) {
                this.availabilityShown.set(false);
            }
            this.session.clearDesks();
            return;
        }
        const hadRange = !!(this.slotStart?.trim() && this.slotEnd?.trim());
        if (!hadRange) {
            this.applyDefaultRangeFromOfficeHours();
        }
        else {
            this.syncManualTimeToRange();
        }
        if (!this.hasCompleteSlotRange()) {
            this.applyDefaultRangeFromOfficeHours();
        }
    }
    private buildDeskSearchCriteria(): SearchCriteria | null {
        if (!this.slotRangeOk() || this.selectedSpaceId() === null) {
            return null;
        }
        return new SearchCriteria(this.targetDate, this.selectedTags, this.includeMaintenance, this.slotStart, this.slotEnd, this.selectedSpaceId());
    }
    private runAvailabilitySearch(showToast: boolean): void {
        if (!this.step2Done() || this.selectedSpaceId() === null || !this.bookingHoursAvailable()) {
            return;
        }
        if (!this.hasCompleteSlotRange() || this.selectionIncludesPastSlot()) {
            return;
        }
        this.warningMsg = this.selectionSpansUnavailableSlot()
            ? 'Parte dell’intervallo coincide con orari non disponibili (già trascorsi o fuori orario sede). Puoi comunque vedere le postazioni per il resto della fascia.'
            : '';
        this.session.runAvailabilitySearch(showToast);
    }
    private resetSearchSessionState(): void {
        this.closeTimePicker();
        this.session.setRefreshSearchAfterSlotsLoad(false);
        this.session.resetSessionFlags();
        this.clearLastSearchSlotRange();
        this.selectedRoomKey.set(null);
    }
    protected onOfficeSelected(): void {
        this.resetSearchSessionState();
        this.session.resetOfficeContext();
        if (this.selectedSpaceId() === null) {
            this.session.clearDesks();
            this.resetSlotSelection(false);
            return;
        }
        this.applyDefaultRangeFromOfficeHours();
        this.session.clearDesks();
        this.loadAggregateSlots();
    }
    private resetBookingUi(): void {
        this.resetSearchSessionState();
        this.errorMsg = '';
        this.warningMsg = '';
        this.session.resetBookingData();
        this.resetSlotSelection(false);
    }
    protected readonly step1Done = computed(() => !!this.selectedCity().trim());
    protected readonly step2Done = computed(() => this.selectedSpaceId() !== null);
    protected step3Active(): boolean {
        return this.step2Done();
    }
    protected loadAggregateSlots(): void {
        this.session.loadAggregateSlots({
            selectedSpaceId: this.selectedSpaceId(),
            targetDate: this.targetDate
        });
    }
    protected selectDate(day: CalendarDayVm): void {
        if (!day.selectable || !this.step2Done())
            return;
        if (day.isoString === this.targetDate) {
            return;
        }
        this.ngZone.run(() => {
            this.closeTimePicker();
            const hadResults = this.availabilityShown();
            this.session.setRefreshSearchAfterSlotsLoad(false);
            this.targetDate = day.isoString;
            this.queueJoined.set(false);
            this.session.clearDesks();
            this.searchOutdated.set(hadResults);
            if (hadResults) {
                this.clearLastSearchSlotRange();
            }
            this.applyDefaultRangeFromOfficeHours();
            if (!hadResults) {
                this.availabilityShown.set(false);
            }
            this.rebuildSlotKinds();
            if (this.selectedSpaceId() !== null) {
                this.session.setOfficeClosedSlotMask(this.computeOfficeClosedMask());
                this.bookingService
                    .getSpaceClosure(this.selectedSpaceId()!, day.isoString)
                    .pipe(catchError(() => of(null)), takeUntilDestroyed(this.destroyRef))
                    .subscribe((closure) => {
                    this.session.setSelectedClosure(closure);
                    this.cdr.markForCheck();
                });
                this.loadAggregateSlots();
            }
            this.cdr.markForCheck();
        });
    }
    protected showAvailabilityResults(): void {
        this.errorMsg = '';
        this.syncManualTimeToRange();
        if (!this.hasCompleteSlotRange()) {
            this.warningMsg = 'Imposta orario di inizio e fine (es. 09:30–12:00).';
            return;
        }
        if (this.selectionIncludesPastSlot()) {
            return;
        }
        this.runAvailabilitySearch(true);
    }
    protected onManualTimeChange(): void {
        this.syncManualTimeToRange();
        this.markSearchCriteriaChanged();
    }
    protected dayButtonClasses(day: CalendarDayVm): Record<string, boolean> {
        const sel = day.isoString === this.targetDate && day.selectable;
        const bookable = day.selectable && !day.isPast && !day.isBeyondHorizon;
        return {
            'sd-calendar-day--bookable': bookable,
            'sd-calendar-day--selected': sel,
            'border-primary': day.isToday && !sel,
            'bg-info bg-opacity-10 text-info': bookable && !sel,
            'bg-primary text-white shadow-sm': sel,
            'bg-light text-muted opacity-50': !bookable
        };
    }
    protected resetSlotSelection(clearDesks: boolean): void {
        this.rangeStartIdx = null;
        this.rangeEndIdx = null;
        if (clearDesks) {
            this.session.clearDesks();
        }
        this.warningMsg = '';
        this.rebuildSlotKinds();
    }
    private selectedSpaceOpeningHours() {
        return this.spaces.find((s) => s.spaceID === this.selectedSpaceId())?.openingHours;
    }
    private slotStartMsForSearchSlot(dayIso: string, slotIdx: number): number {
        const startHm = SLOT_LABELS[slotIdx];
        const [h, m] = startHm.split(':').map((x) => Number(x));
        return new Date(`${dayIso}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`).getTime();
    }
    private isSearchSlotPast(idx: number): boolean {
        if (this.targetDate !== localCalendarDateIso()) {
            return false;
        }
        return this.slotStartMsForSearchSlot(this.targetDate, idx) <= Date.now();
    }
    protected selectionIncludesPastSlot(): boolean {
        if (this.targetDate !== localCalendarDateIso()) {
            return false;
        }
        const span = this.orderedRange();
        if (!span) {
            return false;
        }
        const [from, to] = span;
        for (let i = from; i <= to; i++) {
            if (this.isSearchSlotPast(i)) {
                return true;
            }
        }
        return false;
    }
    private isSearchSlotUnavailable(idx: number): boolean {
        return this.officeClosedSlotMask[idx] || this.isSearchSlotPast(idx);
    }
    protected orderedRange(): [
        number,
        number
    ] | null {
        if (this.rangeStartIdx === null || this.rangeEndIdx === null)
            return null;
        const a = this.rangeStartIdx;
        const b = this.rangeEndIdx;
        return a <= b ? [a, b] : [b, a];
    }
    protected selectionSpansUnavailableSlot(): boolean {
        const span = this.orderedRange();
        if (!span)
            return false;
        const [from, to] = span;
        for (let i = from; i <= to; i++) {
            if (this.isSearchSlotUnavailable(i)) {
                return true;
            }
        }
        return false;
    }
    protected hasCompleteSlotRange(): boolean {
        return this.rangeStartIdx !== null && this.rangeEndIdx !== null;
    }
    protected syncSlotsToCriteria(): void {
        if (this.rangeStartIdx === null) {
            return;
        }
        if (this.rangeEndIdx === null) {
            this.slotStart = SLOT_LABELS[this.rangeStartIdx];
            this.slotEnd = endHmAfterSlot(this.rangeStartIdx);
            this.rebuildSlotKinds();
            return;
        }
        const span = this.orderedRange()!;
        this.slotStart = SLOT_LABELS[span[0]];
        this.slotEnd = endHmAfterSlot(span[1]);
        this.rebuildSlotKinds();
    }
    protected onSlotChipClick(idx: number): void {
        if (this.isSearchSlotUnavailable(idx)) {
            return;
        }
        const span = this.orderedRange();
        if (span && idx >= span[0] && idx <= span[1]) {
            this.resetSlotSelection(true);
            this.markSearchCriteriaChanged();
            return;
        }
        if (this.rangeStartIdx === null || idx < this.rangeStartIdx) {
            this.rangeStartIdx = idx;
            this.rangeEndIdx = null;
            this.syncSlotsToCriteria();
            this.markSearchCriteriaChanged();
            return;
        }
        if (this.rangeEndIdx === null && idx >= this.rangeStartIdx) {
            this.rangeEndIdx = idx;
            this.syncSlotsToCriteria();
            this.markSearchCriteriaChanged();
            return;
        }
        this.rangeStartIdx = idx;
        this.rangeEndIdx = null;
        this.syncSlotsToCriteria();
        this.markSearchCriteriaChanged();
    }
    protected onSlotChipKeydown(ev: KeyboardEvent, idx: number): void {
        if (ev.key === 'Enter' || ev.key === ' ') {
            ev.preventDefault();
            this.onSlotChipClick(idx);
        }
    }
    protected slotUiKind(idx: number): DeskSlotUiKind {
        return this.slotKinds[idx] ?? 'free';
    }
    private computeSlotUiKind(idx: number): DeskSlotUiKind {
        if (this.isSearchSlotUnavailable(idx)) {
            return 'closed';
        }
        const span = this.orderedRange();
        const onlyStart = this.rangeStartIdx !== null && this.rangeEndIdx === null;
        if (onlyStart && idx === this.rangeStartIdx) {
            return 'selected';
        }
        if (span !== null && this.rangeEndIdx !== null) {
            const [from, to] = span;
            const inside = idx >= from && idx <= to;
            if (inside) {
                return this.aggregateSlotFree[idx] ? 'selected' : 'occupied';
            }
            return this.aggregateSlotFree[idx] ? 'free' : 'occupied';
        }
        return this.aggregateSlotFree[idx] ? 'free' : 'occupied';
    }
    protected slotIndexInSelectedRange(idx: number): boolean {
        const span = this.orderedRange();
        if (!span || this.rangeEndIdx === null) {
            return false;
        }
        const [from, to] = span;
        return idx >= from && idx <= to;
    }
    protected slotAriaLabel(idx: number): string {
        const t = SLOT_LABELS[idx];
        if (this.isSearchSlotUnavailable(idx)) {
            return `Non disponibile — slot ${t}`;
        }
        const span = this.orderedRange();
        const inside = span && this.rangeEndIdx !== null && idx >= span[0] && idx <= span[1];
        if (!this.aggregateSlotFree[idx]) {
            return inside
                ? `Ufficio: tutte le postazioni occupate in ${t} — resta nel tuo intervallo`
                : `Tutte le postazioni occupate — slot ${t}`;
        }
        return `Imposta estremo intervallo a ${t}`;
    }
    protected rangeSummaryLine(): string {
        if (!this.hasCompleteSlotRange()) {
            return '';
        }
        const span = this.orderedRange()!;
        const start = SLOT_LABELS[span[0]];
        const end = endHmAfterSlot(span[1]);
        const mins = parseHmToMins(end) -
            parseHmToMins(start);
        if (mins <= 0) {
            return '';
        }
        return `Orario selezionato: ${start} – ${end} (${formatDuration(mins)})`;
    }
    private officeOpeningBoundsForTargetDate(): [
        number,
        number
    ] | null {
        return computeOfficeOpeningBounds(this.selectedSpaceOpeningHours(), this.targetDate);
    }
    private effectiveBookingBoundsForTargetDate(): [
        number,
        number
    ] | null {
        return computeEffectiveBookingBounds(this.selectedSpaceOpeningHours(), this.targetDate, new Date());
    }
    protected bookingHoursBlockedNotice(): {
        show: boolean;
        title: string;
        detail: string;
    } {
        return computeBookingHoursBlockedNotice({
            step2Done: this.step2Done(),
            selectedClosure: this.selectedClosure,
            openingHours: this.selectedSpaceOpeningHours(),
            targetDate: this.targetDate,
            now: new Date(),
            bookingHoursAvailable: this.bookingHoursAvailable()
        });
    }
    private syncManualTimeToRange(): void {
        const rawStart = (this.slotStart ?? '').trim();
        const rawEnd = (this.slotEnd ?? '').trim();
        const bounds = this.effectiveBookingBoundsForTargetDate();
        if (!rawStart || !rawEnd || !bounds) {
            this.applyDefaultRangeFromOfficeHours();
            if (!this.hasCompleteSlotRange()) {
                this.resetSlotSelection(true);
                this.availabilityShown.set(false);
                this.session.clearDesks();
            }
            this.cdr.markForCheck();
            return;
        }
        let ss = snapStartHm(rawStart);
        let se = snapEndHmExclusive(rawEnd);
        const [openBound, closeBound] = bounds;
        let startM = Math.min(closeBound - 30, Math.max(openBound, parseHmToMins(ss)));
        let endM = Math.min(closeBound, Math.max(openBound + 30, parseHmToMins(se)));
        ss = snapStartHm(minutesToHm(startM));
        se = snapEndHmExclusive(minutesToHm(endM));
        if (endM <= startM) {
            endM = Math.min(closeBound, startM + 30);
            se = snapEndHmExclusive(minutesToHm(endM));
        }
        if (!this.commitSlotRangeFromHm(ss, se)) {
            this.applyDefaultRangeFromOfficeHours();
            if (!this.hasCompleteSlotRange()) {
                this.resetSlotSelection(true);
                this.availabilityShown.set(false);
                this.session.clearDesks();
                this.cdr.markForCheck();
                return;
            }
        }
        this.rebuildSlotKinds();
        this.pruneSelectedAmenityTags();
    }
    private computeOfficeClosedMask(): boolean[] {
        return buildOfficeClosedMask(this.selectedSpaceOpeningHours(), this.targetDate);
    }
    protected officeHoursBanner(): {
        show: boolean;
        variant: 'open' | 'closed' | 'default';
        kicker: string;
        range: string;
        sub: string;
    } {
        const selected = this.spaces.find((s) => s.spaceID === this.selectedSpaceId());
        return computeOfficeHoursBanner({
            step2Done: this.step2Done(),
            hasSelectedSpace: selected !== undefined,
            openingHours: selected?.openingHours,
            targetDate: this.targetDate
        });
    }
    private refreshDesksIfReady(): void {
        if (this.selectedSpaceId() === null) {
            return;
        }
        this.session.refreshDesksIfReady({
            selectedTags: this.selectedTags,
            includeMaintenance: this.includeMaintenance
        });
    }
    protected deskSlotMasksReady(): boolean {
        return !this.session.aggregateSlotMasksPending();
    }
    private slotRangeOk(): boolean {
        if (!this.hasCompleteSlotRange())
            return false;
        const [sh, sm] = this.slotStart.split(':').map((x) => Number(x));
        const [eh, em] = this.slotEnd.split(':').map((x) => Number(x));
        if ([sh, sm, eh, em].some((n) => Number.isNaN(n))) {
            return false;
        }
        const ok = eh * 60 + em > sh * 60 + sm;
        return ok;
    }
    public loadDesks(): void {
        this.session.loadDesks({
            selectedTags: this.selectedTags,
            includeMaintenance: this.includeMaintenance
        });
    }
    private applyLocalBookingToDeskMask(deskId: number, range: {
        start: string;
        end: string;
    }): void {
        this.session.applyLocalBookingToDeskMask(deskId, range);
    }
    private refreshDeskCardAvailability(deskId: number): void {
        this.session.refreshDeskCardAvailability(deskId);
    }
    protected isMaintenanceDesk(desk: Desk): boolean {
        return desk.state.code === 'MAINTENANCE';
    }
    protected onIncludeMaintenanceChange(): void {
        this.pruneSelectedAmenityTags();
        if (this.availabilityShown()) {
            this.refreshDesksIfReady();
        }
    }
    protected toggleMaintenanceFilter(): void {
        this.includeMaintenance = !this.includeMaintenance;
        this.onIncludeMaintenanceChange();
    }
    protected clearFilters(): void {
        this.selectedTags = [];
        this.includeMaintenance = false;
        this.warningMsg = '';
        this.errorMsg = '';
        if (this.availabilityShown()) {
            this.refreshDesksIfReady();
        }
    }
    private pruneSelectedAmenityTags(): void {
        const available = computeAvailableAmenityTags(
            this.step2Done(),
            this.hasCompleteSlotRange(),
            this.officeDesks,
            this.includeMaintenance
        );
        const next = pruneSelectedAmenityTagsUtil(this.selectedTags, available);
        if (next.length !== this.selectedTags.length) {
            this.selectedTags = next;
        }
    }
    protected toggleAmenityTag(tag: string): void {
        if (this.isLoading)
            return;
        const next = new Set(this.selectedTags);
        if (next.has(tag))
            next.delete(tag);
        else
            next.add(tag);
        this.selectedTags = Array.from(next);
        if (this.availabilityShown()) {
            this.refreshDesksIfReady();
        }
    }
    protected isDeskBlocked(desk: DeskViewModel): boolean {
        return desk.availabilityKind !== 'available' || !desk.isBookable() || this.isMaintenanceDesk(desk);
    }
    protected isDeskPartial(desk: DeskViewModel): boolean {
        return desk.availabilityKind === 'partial';
    }
    protected isDeskOccupied(desk: DeskViewModel): boolean {
        return desk.availabilityKind === 'occupied';
    }
    protected isRequestedRangeCompletelyOutsideHours(): boolean {
        const span = this.orderedRange();
        if (!span)
            return false;
        const [from, to] = span;
        for (let i = from; i <= to; i++) {
            if (!this.officeClosedSlotMask[i]) {
                return false;
            }
        }
        return true;
    }
    protected selectedSpaceHasNoDesks(): boolean {
        return this.selectedSpaceId() !== null && !this.slotsLoading() && this.officeDesks.length === 0;
    }
    protected outcomeKind(): 'none' | 'A' | 'C' | 'D' {
        if (!this.availabilityShown() ||
            this.searchOutdated() ||
            !this.hasCompleteSlotRange() ||
            this.isLoading ||
            this.deskResultsLoading() ||
            this.desks.length === 0 ||
            this.slotsLoading() ||
            this.session.aggregateSlotMasksPending()) {
            return 'none';
        }
        if (this.isRequestedRangeCompletelyOutsideHours()) {
            return 'D';
        }
        const visible = this.desksFilteredByRoom();
        const actionable = visible.filter((d) => !this.isMaintenanceDesk(d) &&
            d.isBookable() &&
            (d.availabilityKind === 'available' || d.availabilityKind === 'partial'));
        if (actionable.length > 0)
            return 'A';
        return 'C';
    }
    protected workerOwnsOfficeSlotInSearchRange(): boolean {
        if (!this.hasCompleteSlotRange() || !this.targetDate?.trim()) {
            return false;
        }
        const startHm = this.slotStart?.trim();
        const endHm = this.slotEnd?.trim();
        if (!startHm || !endHm) {
            return false;
        }
        const deskIds = new Set(this.officeDesks.map((d) => d.id));
        if (!deskIds.size) {
            return false;
        }
        const rangeStartMs = new Date(`${this.targetDate}T${startHm}:00`).getTime();
        const rangeEndMs = new Date(`${this.targetDate}T${endHm}:00`).getTime();
        if (!Number.isFinite(rangeStartMs) || !Number.isFinite(rangeEndMs) || rangeEndMs <= rangeStartMs) {
            return false;
        }
        return this.myBookings().some((b) => {
            const status = (b.status ?? '').trim().toUpperCase();
            if (status === 'CANCELLED') {
                return false;
            }
            if (!deskIds.has(b.deskID)) {
                return false;
            }
            const bookingStartMs = new Date(b.startTime).getTime();
            const bookingEndMs = new Date(b.endTime).getTime();
            if (!Number.isFinite(bookingStartMs) || !Number.isFinite(bookingEndMs)) {
                return false;
            }
            return bookingStartMs < rangeEndMs && rangeStartMs < bookingEndMs;
        });
    }
    protected shouldOfferOfficeWaitlist(): boolean {
        return !this.workerOwnsOfficeSlotInSearchRange() && !!this.waitlistDeskId();
    }
    protected closeModal(): void {
        try {
            this.modalRef?.close();
        }
        catch {
        }
        this.modalRef = null;
        this.selectedDeskForTimeline = null;
        this.showWaitlistInline = false;
        this.timelineSelfOverlapHint = null;
        this.timelineGenericBookError = null;
    }
    private openAvailabilityModal(): void {
        try {
            this.modalRef?.close();
        }
        catch {
        }
        this.modalRef = null;
        this.modalRef = this.modalService.open(this.availabilityModal, {
            modalClass: 'modal-dialog-centered modal-lg'
        });
    }
    protected joinOfficeWaitlist(): void {
        const deskId = this.waitlistDeskId();
        if (!deskId ||
            this.queueJoined() ||
            this.workerOwnsOfficeSlotInSearchRange() ||
            !this.hasCompleteSlotRange() ||
            this.selectionSpansUnavailableSlot()) {
            return;
        }
        const desiredStart = `${this.targetDate}T${this.slotStart}:00`;
        const desiredEnd = `${this.targetDate}T${this.slotEnd}:00`;
        this.bookingService
            .subscribeWaitlist(deskId, this.targetDate, desiredStart, desiredEnd)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.queueJoined.set(true);
                this.toast.success('Sei in coda: trovi la conferma in Notifiche. Quando si libera una postazione compatibile riceverai un altro avviso.');
                this.errorMsg = '';
            },
            error: (err: Error) => {
                this.toast.error(err.message || 'Impossibile entrare in coda.');
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
    public openDeskAvailability(desk: DeskViewModel): void {
        if (this.isMaintenanceDesk(desk))
            return;
        if (desk.availabilityKind === 'occupied')
            return;
        if (!desk.isBookable() && desk.availabilityKind !== 'partial')
            return;
        if (!this.hasCompleteSlotRange() || !this.slotStart?.trim() || !this.slotEnd?.trim()) {
            this.toast.error('Imposta prima una fascia oraria valida per questa sede.');
            return;
        }
        if (this.selectionIncludesPastSlot()) {
            this.toast.error('La fascia scelta include slot già iniziati. Scegli un orario futuro.');
            return;
        }
        this.availabilityRefreshCounter++;
        this.selectedDeskForTimeline = desk;
        this.clearTimelineBookingAlerts();
        this.refreshDeskSlotMaskFromApi(desk.id);
        queueMicrotask(() => this.openAvailabilityModal());
    }
    private refreshDeskSlotMaskFromApi(deskId: number): void {
        if (!this.targetDate?.trim()) {
            return;
        }
        this.bookingService
            .getSlotAvailability(deskId, this.targetDate)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (row) => {
                this.session.setDeskSlotMask(deskId, rowToFreeMask(row));
                this.refreshDeskCardAvailability(deskId);
            }
        });
    }
    private ensureAvailabilityModalOpen(): void {
        queueMicrotask(() => {
            if (!this.modalRef) {
                this.openAvailabilityModal();
            }
            this.cdr.detectChanges();
        });
    }
    private clearTimelineBookingAlerts(): void {
        this.timelineSelfOverlapHint = null;
        this.timelineGenericBookError = null;
        this.showWaitlistInline = false;
    }
    private handleTimelineBookError(desk: DeskViewModel, message: string): void {
        const { kind, userMessage } = classifyBookingError(message);
        this.clearTimelineBookingAlerts();
        if (kind === 'selfOverlap') {
            this.timelineSelfOverlapHint = userMessage;
            this.selectedDeskForTimeline = desk;
            this.ensureAvailabilityModalOpen();
            return;
        }
        if (kind === 'alreadyBooked') {
            this.offerWaitlist(desk, userMessage);
            return;
        }
        this.timelineGenericBookError = userMessage;
        this.toast.error(this.timelineGenericBookError);
        this.selectedDeskForTimeline = desk;
        this.ensureAvailabilityModalOpen();
    }
    private offerWaitlist(desk: DeskViewModel, originalMessage: string): void {
        this.timelineSelfOverlapHint = null;
        this.timelineGenericBookError = null;
        this.errorMsg = originalMessage;
        this.selectedDeskForTimeline = desk;
        this.showWaitlistInline = true;
        this.ensureAvailabilityModalOpen();
    }
    protected subscribeInlineWaitlist(): void {
        if (!this.selectedDeskForTimeline)
            return;
        const desiredStart = `${this.targetDate}T${this.slotStart}:00`;
        const desiredEnd = `${this.targetDate}T${this.slotEnd}:00`;
        this.bookingService
            .subscribeWaitlist(this.selectedDeskForTimeline.id, this.targetDate, desiredStart, desiredEnd)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.warningMsg =
                    'Sei in lista d’attesa: controlla Notifiche; quando si libera uno slot compatibile riceverai un nuovo messaggio.';
                this.toast.success('Sei in lista d’attesa per questa postazione.');
                this.errorMsg = '';
                this.showWaitlistInline = false;
                this.closeModal();
            },
            error: (err: Error) => {
                this.toast.error(err.message || 'Impossibile entrare in lista d’attesa.');
                this.errorMsg = err.message;
            }
        });
    }
    protected bookFromTimeline(range: {
        start: string;
        end: string;
    }): void {
        if (!this.selectedDeskForTimeline)
            return;
        const desk = this.selectedDeskForTimeline;
        this.clearTimelineBookingAlerts();
        const req = new BookingRequest(desk.id, range.end, range.start);
        this.bookingInProgressId.set(desk.id);
        this.bookingService
            .getSlotAvailability(desk.id, this.targetDate)
            .pipe(catchError(() => of([] as SlotStatus[])), switchMap((slots) => {
            if (Array.isArray(slots) && slots.length > 0) {
                const preflightBusy = slotsRangeHasBusyApi(slots, range.start, range.end);
                if (preflightBusy) {
                    this.availabilityRefreshCounter++;
                    this.handleTimelineBookError(desk, 'La postazione risulta già occupata nella fascia oraria selezionata.');
                    return EMPTY;
                }
            }
            return this.bookingService.bookDesk(req);
        }), finalize(() => this.bookingInProgressId.set(null)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                const bookedDeskId = desk.id;
                this.applyLocalBookingToDeskMask(bookedDeskId, range);
                this.refreshDeskCardAvailability(bookedDeskId);
                this.selectedDeskForTimeline = null;
                this.showWaitlistInline = false;
                this.timelineSelfOverlapHint = null;
                this.timelineGenericBookError = null;
                this.toast.success('Prenotazione completata.');
                this.loadMyBookings();
                this.session.setRefreshSearchAfterSlotsLoad(true);
                this.loadAggregateSlots();
                this.closeModal();
            },
            error: (error: Error) => {
                this.availabilityRefreshCounter++;
                const d = this.selectedDeskForTimeline;
                if (d) {
                    this.handleTimelineBookError(d, error.message ?? '');
                }
            }
        });
    }
    protected deskResultsHeading(desk: Desk): string {
        const c = desk.code?.trim();
        return c ? `Postazione ${c}` : `Postazione #${desk.id}`;
    }
    protected openAmenitiesModal(desk: DeskViewModel): void {
        this.amenityFilter?.openAmenitiesModal(desk);
    }
    protected hasHiddenTags(tags: string[]): boolean {
        return tags.length > 3;
    }
    protected amenityPreviewTags(tags: string[]): string[] {
        if (!tags?.length) {
            return [];
        }
        return sortAmenityPreviewTags(tags);
    }
    protected amenityPreviewTagsLimited(tags: string[]): string[] {
        return this.amenityPreviewTags(tags).slice(0, 3);
    }
    protected formatTag(tag: string): string {
        return formatAmenityTag(tag);
    }
}
