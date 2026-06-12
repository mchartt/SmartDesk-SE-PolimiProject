import { Component, ElementRef, HostListener, inject, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostSpacesModalStore } from '../../host-spaces-modal.store';
import {
    closureCalendarDayAria,
    closureCellDisabled,
    closureMonthOptions,
    closureRevokeCanSubmit,
    closureYearOptions,
    formatItDateRange,
    navigateClosureMonth,
    type ClosureCalCell,
    type ClosureUiGroup,
    type PendingClosureUiGroup
} from '../../host-spaces.util';

@Component({
    selector: 'app-closure-calendar',
    standalone: true,
    imports: [FormsModule, MdbRippleModule, SdIconComponent],
    templateUrl: './closure-calendar.component.html',
    styleUrl: './closure-calendar.component.scss'
})
export class ClosureCalendarComponent {
    @ViewChild('closureCalRoot')
    private closureCalRootRef?: ElementRef<HTMLElement>;

    protected readonly modal = inject(HostSpacesModalStore);
    protected readonly closureWeekdayLabels = ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom'] as const;

    readonly closureCalendarYear = this.modal.closureCalendarYear;
    readonly closureCalendarMonth = this.modal.closureCalendarMonth;
    readonly closureRows = this.modal.closureRows;
    readonly closurePendingDates = this.modal.closurePendingDates;
    readonly closureSaving = this.modal.closureSaving;
    readonly closureRevoking = this.modal.closureRevoking;
    readonly closureWorkflow = this.modal.closureWorkflow;
    readonly closureMonthPanelOpen = this.modal.closureMonthPanelOpen;
    readonly closureYearPanelOpen = this.modal.closureYearPanelOpen;
    readonly closureCalendarRows = this.modal.closureCalendarRows;
    readonly closureDisplayGroups = this.modal.closureDisplayGroups;
    readonly closurePendingDisplayGroups = this.modal.closurePendingDisplayGroups;

    closeCalMenusIfOpen(): boolean {
        if (!this.closureMonthPanelOpen() && !this.closureYearPanelOpen()) {
            return false;
        }
        this.modal.closeClosureCalMenus();
        return true;
    }

    @HostListener('document:click', ['$event'])
    protected onDocumentClick(ev: MouseEvent): void {
        if (!this.closureMonthPanelOpen() && !this.closureYearPanelOpen()) {
            return;
        }
        const target = ev.target;
        if (!(target instanceof Node)) {
            return;
        }
        const root = this.closureCalRootRef?.nativeElement;
        if (!root || !root.contains(target)) {
            this.modal.closeClosureCalMenus();
        }
    }

    protected closureMonthOptions(): { value: number; label: string }[] {
        return closureMonthOptions();
    }

    protected closureYearOptions(): number[] {
        return closureYearOptions();
    }

    protected closureNavigateMonth(delta: number): void {
        this.modal.closeClosureCalMenus();
        const next = navigateClosureMonth(this.closureCalendarYear(), this.closureCalendarMonth(), delta);
        this.closureCalendarYear.set(next.year);
        this.closureCalendarMonth.set(next.month);
    }

    protected closureMonthButtonLabel(): string {
        const m = this.closureCalendarMonth();
        const opt = closureMonthOptions().find((o) => o.value === m);
        return opt?.label ?? String(m);
    }

    protected closureYearButtonLabel(): string {
        return String(this.closureCalendarYear());
    }

    protected toggleClosureMonthMenu(ev: MouseEvent): void {
        ev.stopPropagation();
        this.modal.toggleClosureMonthMenu();
    }

    protected toggleClosureYearMenu(ev: MouseEvent): void {
        ev.stopPropagation();
        this.modal.toggleClosureYearMenu();
    }

    protected pickClosureMonth(value: number): void {
        this.modal.pickClosureMonth(value);
    }

    protected pickClosureYear(value: number): void {
        this.modal.pickClosureYear(value);
    }

    protected setClosureWorkflow(mode: 'schedule' | 'revoke'): void {
        this.modal.setClosureWorkflow(mode);
    }

    protected onClosureReasonChange(value: string): void {
        this.modal.onClosureReasonChange(value);
    }

    protected onClosureCalendarDayClick(cell: ClosureCalCell): void {
        this.modal.onClosureCalendarDayClick(cell);
    }

    protected closureCellDisabled(cell: ClosureCalCell): boolean {
        return closureCellDisabled(cell, this.closureWorkflow());
    }

    protected closureRevokeCanSubmit(): boolean {
        return closureRevokeCanSubmit(this.modal.closureRevokeBand(), this.closureRows());
    }

    protected closureCalendarDayAria(cell: ClosureCalCell): string | null {
        return closureCalendarDayAria(cell, this.closureWorkflow());
    }

    protected submitRevokeClosures(): void {
        this.modal.submitRevokeClosures();
    }

    protected removeClosurePendingGroup(group: PendingClosureUiGroup): void {
        this.modal.removeClosurePendingGroup(group);
    }

    protected submitClosures(): void {
        this.modal.submitClosures();
    }

    protected removeClosureGroup(group: ClosureUiGroup): void {
        const rangeLabel = group.dateLabelOverride ?? formatItDateRange(group.startIso, group.endIso);
        this.modal.removeClosureGroup(group, rangeLabel);
    }

    protected formatItDateRange(startIso: string, endIso: string): string {
        return formatItDateRange(startIso, endIso);
    }

    protected closeClosureCalMenusPublic(): void {
        this.modal.closeClosureCalMenus();
    }

    protected get closureReason(): string {
        return this.modal.closureReason;
    }

    protected get closureReasonInvalid(): boolean {
        return this.modal.closureReasonInvalid;
    }
}
