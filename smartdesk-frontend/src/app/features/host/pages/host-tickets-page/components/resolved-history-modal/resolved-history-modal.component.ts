import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { formatShortDateTime } from '../../../../../../core/utils/date.util';
import { technicianTicketSeverityLabel } from '../../../../../../core/utils/technician-ticket-list.util';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTicketsModalStore } from '../../host-tickets-modal.store';
import {
    assignedTechnicianLabel,
    ticketDescriptionText,
    ticketHeading,
    ticketNumericId,
    ticketRefLineOnly,
    workerLabel
} from '../../host-tickets.util';

@Component({
    selector: 'app-resolved-history-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './resolved-history-modal.component.html',
    styleUrl: './resolved-history-modal.component.scss'
})
export class ResolvedHistoryModalComponent {
    protected readonly modal = inject(HostTicketsModalStore);

    readonly resolvedHistoryModalOpen = this.modal.resolvedHistoryModalOpen;
    readonly historyModalSearchQuery = this.modal.historyModalSearchQuery;
    readonly resolvedTickets = this.modal.resolvedTickets;
    readonly resolvedTicketsLoading = this.modal.resolvedTicketsLoading;
    readonly resolvedHistoryFetched = this.modal.resolvedHistoryFetched;
    readonly resolvedHistoryError = this.modal.resolvedHistoryError;
    readonly resolvedHistoryClearing = this.modal.resolvedHistoryClearing;
    readonly historyModalFilteredTickets = this.modal.historyModalFilteredTickets;

    protected closeResolvedHistoryModal(): void {
        this.modal.closeResolvedHistoryModal();
    }

    protected requestClearResolvedHistory(): void {
        this.modal.requestClearResolvedHistory();
    }

    protected historySubtitle(): string {
        if (this.resolvedHistoryFetched() && !this.resolvedTicketsLoading()) {
            const n = this.resolvedTickets().length;
            return n + (n === 1 ? ' elemento in archivio' : ' elementi in archivio');
        }
        return 'Archivio segnalazioni chiuse';
    }

    protected trackResolvedTicket(_i: number, t: Record<string, unknown>): unknown {
        return t['ticketID'] ?? _i;
    }

    protected ticketHeading(t: Record<string, unknown>): string {
        return ticketHeading(t);
    }

    protected ticketRefLineOnly(t: Record<string, unknown>): string {
        return ticketRefLineOnly(t);
    }

    protected workerLabel(ticket: Record<string, unknown>): string {
        return workerLabel(ticket);
    }

    protected formatTicketResolvedAt(value: unknown): string {
        return formatShortDateTime(value as string);
    }

    protected formatTicketWhen(iso: unknown): string {
        return formatShortDateTime(iso as string);
    }

    protected ticketSeverityLabel(severity: unknown): string {
        return technicianTicketSeverityLabel(typeof severity === 'string' ? severity : null) || '—';
    }

    protected ticketDescriptionText(ticket: Record<string, unknown>): string {
        return ticketDescriptionText(ticket);
    }

    protected assignedTechnicianLabel(ticket: Record<string, unknown>): string {
        return assignedTechnicianLabel(ticket);
    }

    protected isTicketDetailExpanded(ticket: Record<string, unknown>): boolean {
        return this.modal.isTicketDetailExpanded(ticket);
    }

    protected toggleTicketDetail(ticket: Record<string, unknown>): void {
        this.modal.toggleTicketDetail(ticket);
    }

    protected ticketNumericId(ticket: Record<string, unknown>): number | null {
        return ticketNumericId(ticket);
    }
}
