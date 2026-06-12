import { Component, computed, input } from '@angular/core';
import { ticketStatusBadgeClass, ticketStatusIcon, ticketStatusLabel } from '../../../core/utils/ticket-status-display.util';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-ticket-status-badge',
    standalone: true,
    imports: [SdIconComponent],
    styleUrl: './ticket-status-badge.component.scss',
    template: `    <span [class]="classes()" [attr.aria-label]="ariaLabel()">      @if (showIcon()) {        <app-sd-icon [name]="icon()" [size]="iconSize()" aria-hidden="true" />      }      {{ label() }}    </span>  `
})
export class TicketStatusBadgeComponent {
    readonly status = input.required<string>();
    readonly compact = input(false);
    readonly showIcon = input(true);
    protected readonly label = computed(() => ticketStatusLabel(this.status()));
    protected readonly icon = computed(() => ticketStatusIcon(this.status()));
    protected readonly classes = computed(() => ticketStatusBadgeClass(this.status(), this.compact()));
    protected readonly iconSize = computed(() => (this.compact() ? 12 : 14));
    protected readonly ariaLabel = computed(() => `Stato: ${this.label()}`);
}
