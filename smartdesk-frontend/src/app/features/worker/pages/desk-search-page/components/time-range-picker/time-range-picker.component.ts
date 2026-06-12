import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, inject, input, output } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import {
    SLOT_END_LABELS,
    SLOT_LABELS,
    addMinutesToHm,
    parseHmToMins
} from '../../../desk-slot-grid.util';
import { localCalendarDateIso } from '../../../../../../core/utils/date.util';

export type TimeRangePickerMode = 'start' | 'end';

export type TimeRangePickerChange = {
    slotStart: string;
    slotEnd: string;
};

@Component({
    selector: 'app-time-range-picker',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MdbRippleModule, SdIconComponent],
    templateUrl: './time-range-picker.component.html',
    styleUrl: './time-range-picker.component.scss'
})
export class TimeRangePickerComponent {
    readonly slotStart = input('');
    readonly slotEnd = input('');
    readonly bookingHoursAvailable = input(false);
    readonly selectionIncludesPastSlot = input(false);
    readonly step2Done = input(false);
    readonly targetDate = input('');
    readonly bookingBounds = input<[number, number] | null>(null);

    readonly slotRangeChange = output<TimeRangePickerChange>();

    protected activeTimePicker: TimeRangePickerMode | null = null;
    protected draftTime = '08:00';

    private readonly cdr = inject(ChangeDetectorRef);

    closeTimePicker(): void {
        this.activeTimePicker = null;
        this.cdr.markForCheck();
    }

    protected toggleTimePicker(mode: TimeRangePickerMode): void {
        if (!this.step2Done()) {
            return;
        }
        if (this.activeTimePicker === mode) {
            this.closeTimePicker();
            return;
        }
        const options = this.timeOptionsForPicker(mode);
        if (!options.length) {
            this.closeTimePicker();
            return;
        }
        this.activeTimePicker = mode;
        const current = mode === 'start' ? this.slotStart() : this.slotEnd();
        const pick = options.includes(current) ? current : options[0];
        this.draftTime = pick;
        if (!(current ?? '').trim() || !options.includes(current ?? '')) {
            this.applyTimeOption(mode, pick);
        }
        this.cdr.markForCheck();
    }

    @HostListener('document:click', ['$event'])
    protected onDocumentClick(event: MouseEvent): void {
        if (!this.activeTimePicker) {
            return;
        }
        const target = event.target;
        if (target instanceof Element && target.closest('.time-picker-popover, .desk-search-time-btn')) {
            return;
        }
        this.closeTimePicker();
    }

    protected nudgeTimePicker(delta: number): void {
        const mode = this.activeTimePicker;
        if (!mode) {
            return;
        }
        const options = this.timeOptionsForPicker(mode);
        const idx = options.indexOf(this.draftTime);
        const next = options[idx + delta];
        if (next) {
            this.draftTime = next;
            this.applyTimeOption(mode, next);
        }
    }

    protected canNudgeTimePicker(delta: number): boolean {
        const mode = this.activeTimePicker;
        if (!mode) {
            return false;
        }
        const options = this.timeOptionsForPicker(mode);
        const idx = options.indexOf(this.draftTime);
        return idx + delta >= 0 && idx + delta < options.length;
    }

    protected timePickerHour(): string {
        return this.draftTime.split(':')[0] ?? '--';
    }

    protected timePickerMinute(): string {
        return this.draftTime.split(':')[1] ?? '--';
    }

    protected adjustTime(which: TimeRangePickerMode, deltaMinutes: number): void {
        const base = which === 'start' ? this.slotStart() : this.slotEnd();
        if (!(base ?? '').trim()) {
            return;
        }
        const next = addMinutesToHm(base, deltaMinutes);
        if (!next) {
            return;
        }
        if (which === 'start') {
            this.emitSlotRange(next, this.slotEnd());
        }
        else {
            this.emitSlotRange(this.slotStart(), next);
        }
    }

    protected timeOptionsForPicker(mode: TimeRangePickerMode): string[] {
        const bounds = this.bookingBounds();
        if (!bounds) {
            return [];
        }
        const [open, close] = bounds;
        const source = mode === 'start' ? SLOT_LABELS : SLOT_END_LABELS;
        return source.filter((hm) => {
            const mins = parseHmToMins(hm);
            const withinOffice = mode === 'start' ? mins >= open && mins + 30 <= close : mins - 30 >= open && mins <= close;
            if (!withinOffice) {
                return false;
            }
            return mode === 'start' ? !this.isSlotStartHmPast(hm) : !this.isSlotEndHmPast(hm);
        });
    }

    private applyTimeOption(mode: TimeRangePickerMode, value: string): void {
        if (mode === 'start') {
            let newEnd = this.slotEnd();
            if (!newEnd || parseHmToMins(newEnd) <= parseHmToMins(value)) {
                newEnd = addMinutesToHm(value, 30) ?? '';
            }
            this.emitSlotRange(value, newEnd);
        }
        else {
            let newStart = this.slotStart();
            if (!newStart || parseHmToMins(newStart) >= parseHmToMins(value)) {
                newStart = addMinutesToHm(value, -30) ?? '';
            }
            this.emitSlotRange(newStart, value);
        }
    }

    private emitSlotRange(slotStart: string, slotEnd: string): void {
        this.slotRangeChange.emit({ slotStart, slotEnd });
    }

    private slotStartMsForSearchSlot(dayIso: string, slotIdx: number): number {
        const startHm = SLOT_LABELS[slotIdx];
        const [h, m] = startHm.split(':').map((x) => Number(x));
        return new Date(`${dayIso}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`).getTime();
    }

    private isSearchSlotPast(idx: number): boolean {
        if (this.targetDate() !== localCalendarDateIso()) {
            return false;
        }
        return this.slotStartMsForSearchSlot(this.targetDate(), idx) <= Date.now();
    }

    private isSlotStartHmPast(hm: string): boolean {
        const idx = SLOT_LABELS.indexOf(hm);
        return idx >= 0 && this.isSearchSlotPast(idx);
    }

    private isSlotEndHmPast(hm: string): boolean {
        const lastStart = addMinutesToHm(hm, -30);
        if (!lastStart) {
            return false;
        }
        return this.isSlotStartHmPast(lastStart);
    }
}
