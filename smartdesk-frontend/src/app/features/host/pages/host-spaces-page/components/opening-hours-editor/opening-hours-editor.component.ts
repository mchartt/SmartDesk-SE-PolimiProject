import { Component, HostListener, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { hmToMinutes } from '../../../../../../core/utils/time.util';
import {
    addMinutesToHmClamped,
    applyAllDaysOpenPreset,
    applyBusinessHoursPreset,
    buildDefaultOpeningHoursRows,
    DEFAULT_OPEN,
    hoursTimeOptions,
    type HoursTimeField,
    type OpeningHoursRow,
    type WeekdayKey
} from '../../host-spaces.util';

@Component({
    selector: 'app-opening-hours-editor',
    standalone: true,
    imports: [FormsModule, MdbRippleModule, SdIconComponent],
    templateUrl: './opening-hours-editor.component.html',
    styleUrl: './opening-hours-editor.component.scss'
})
export class OpeningHoursEditorComponent {
    readonly openingHoursRows = input.required<OpeningHoursRow[]>();
    readonly hoursQuickAction = input<'weekdays' | 'allDays' | null>(null);

    readonly openingHoursRowsChange = output<OpeningHoursRow[]>();
    readonly hoursQuickActionChange = output<'weekdays' | 'allDays' | null>();
    readonly hoursEdited = output<void>();

    protected hoursTimePicker: { dayKey: WeekdayKey; field: HoursTimeField } | null = null;
    protected hoursDraftTime = DEFAULT_OPEN;

    closeTimePickerIfOpen(): boolean {
        if (!this.hoursTimePicker) {
            return false;
        }
        this.closeHoursTimePicker();
        return true;
    }

    @HostListener('document:click', ['$event'])
    protected onDocumentClick(ev: MouseEvent): void {
        if (!this.hoursTimePicker) {
            return;
        }
        const target = ev.target;
        if (target instanceof Element && target.closest('.sd-hours-time-popover, .sd-hours-time-button')) {
            return;
        }
        this.closeHoursTimePicker();
    }

    protected toggleWeekdaysPreset(): void {
        if (this.hoursQuickAction() === 'weekdays') {
            this.openingHoursRowsChange.emit(buildDefaultOpeningHoursRows());
            this.hoursQuickActionChange.emit(null);
            this.hoursEdited.emit();
            return;
        }
        this.openingHoursRowsChange.emit(applyBusinessHoursPreset(this.openingHoursRows()));
        this.hoursQuickActionChange.emit('weekdays');
        this.hoursEdited.emit();
    }

    protected toggleAllDaysPreset(): void {
        if (this.hoursQuickAction() === 'allDays') {
            this.openingHoursRowsChange.emit(buildDefaultOpeningHoursRows());
            this.hoursQuickActionChange.emit(null);
            this.hoursEdited.emit();
            return;
        }
        this.openingHoursRowsChange.emit(applyAllDaysOpenPreset(this.openingHoursRows()));
        this.hoursQuickActionChange.emit('allDays');
        this.hoursEdited.emit();
    }

    protected onDayToggle(row: OpeningHoursRow, isOpen: boolean): void {
        row.closed = !isOpen;
        this.hoursQuickActionChange.emit(null);
        this.hoursEdited.emit();
    }

    protected toggleHoursTimePicker(row: OpeningHoursRow, field: HoursTimeField): void {
        const current = this.hoursTimePicker;
        if (current?.dayKey === row.dayKey && current.field === field) {
            this.closeHoursTimePicker();
            return;
        }
        this.hoursTimePicker = { dayKey: row.dayKey, field };
        const value = field === 'open' ? row.open : row.close;
        const options = hoursTimeOptions(row, field);
        this.hoursDraftTime = options.includes(value) ? value : (options[0] ?? value);
    }

    protected closeHoursTimePicker(): void {
        this.hoursTimePicker = null;
    }

    protected isHoursTimePickerOpen(row: OpeningHoursRow, field: HoursTimeField): boolean {
        return this.hoursTimePicker?.dayKey === row.dayKey && this.hoursTimePicker?.field === field;
    }

    protected nudgeHoursTimePicker(delta: number): void {
        const active = this.hoursTimePicker;
        if (!active) {
            return;
        }
        const row = this.openingHoursRows().find((r) => r.dayKey === active.dayKey);
        if (!row) {
            return;
        }
        const options = hoursTimeOptions(row, active.field);
        const idx = options.indexOf(this.hoursDraftTime);
        const next = options[idx + delta];
        if (!next) {
            return;
        }
        this.hoursDraftTime = next;
        this.applyHoursTime(row, active.field, next);
    }

    protected canNudgeHoursTimePicker(delta: number): boolean {
        const active = this.hoursTimePicker;
        if (!active) {
            return false;
        }
        const row = this.openingHoursRows().find((r) => r.dayKey === active.dayKey);
        if (!row) {
            return false;
        }
        const options = hoursTimeOptions(row, active.field);
        const idx = options.indexOf(this.hoursDraftTime);
        return idx + delta >= 0 && idx + delta < options.length;
    }

    protected hoursPickerHour(): string {
        return this.hoursDraftTime.split(':')[0] ?? '--';
    }

    protected hoursPickerMinute(): string {
        return this.hoursDraftTime.split(':')[1] ?? '--';
    }

    private applyHoursTime(row: OpeningHoursRow, field: HoursTimeField, value: string): void {
        if (field === 'open') {
            row.open = value;
            if (hmToMinutes(row.close) <= hmToMinutes(value)) {
                row.close = addMinutesToHmClamped(value, 30) ?? row.close;
            }
        }
        else {
            row.close = value;
            if (hmToMinutes(row.open) >= hmToMinutes(value)) {
                row.open = addMinutesToHmClamped(value, -30) ?? row.open;
            }
        }
        this.hoursQuickActionChange.emit(null);
        this.hoursEdited.emit();
    }
}
