import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk } from '../../../../../../core/models';
import { formatShortDateTime } from '../../../../../../core/utils/date.util';
import { technicianTicketSeverityLabel } from '../../../../../../core/utils/technician-ticket-list.util';
import { TicketChatPanelComponent } from '../../../../../../shared/components/ticket-chat-panel/ticket-chat-panel.component';
import { TicketStatusBadgeComponent } from '../../../../../../shared/components/ticket-status-badge/ticket-status-badge.component';
import { TicketStatusProgressComponent } from '../../../../../../shared/components/ticket-status-progress/ticket-status-progress.component';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTicketsModalStore } from '../../host-tickets-modal.store';
import {
    deskRowPrimaryLabel,
    deskRowSecondaryLabel,
    isActiveTicket,
    modalStepHeading,
    ModalRoomRow,
    ticketHeading,
    ticketNeedsTechnicianAssignment,
    ticketNumericId,
    ticketRefLineOnly,
    unresolvedTicketsForDesk
} from '../../host-tickets.util';

@Component({
    selector: 'app-office-tickets-modal',
    standalone: true,
    imports: [
        FormsModule,
        MdbFormsModule,
        MdbRippleModule,
        SdModalHeaderComponent,
        SdIconComponent,
        TicketChatPanelComponent,
        TicketStatusBadgeComponent,
        TicketStatusProgressComponent
    ],
    templateUrl: './office-tickets-modal.component.html',
    styleUrl: './office-tickets-modal.component.scss'
})
export class OfficeTicketsModalComponent {
    protected readonly modal = inject(HostTicketsModalStore);

    readonly modalBundle = this.modal.modalBundle;
    readonly modalStep = this.modal.modalStep;
    readonly roomSearchQuery = this.modal.roomSearchQuery;
    readonly deskSearchQuery = this.modal.deskSearchQuery;
    readonly modalUnresolvedTicketTotal = this.modal.modalUnresolvedTicketTotal;
    readonly modalRoomsWithTickets = this.modal.modalRoomsWithTickets;
    readonly modalRoomsFiltered = this.modal.modalRoomsFiltered;
    readonly modalDesksFiltered = this.modal.modalDesksFiltered;
    readonly selectedRoomTitle = this.modal.selectedRoomTitle;
    readonly selectedDesk = this.modal.selectedDesk;
    readonly selectedDeskUnresolvedTickets = this.modal.selectedDeskUnresolvedTickets;

    protected closeOfficeModal(): void {
        this.modal.closeOfficeModal();
    }

    protected modalBack(): void {
        this.modal.modalBack();
    }

    protected pickRoom(key: string): void {
        this.modal.pickRoom(key);
    }

    protected pickDesk(deskId: number): void {
        this.modal.pickDesk(deskId);
    }

    protected modalSubtitle(mb: { space: { officeCode: string } }): string {
        const step = this.modalStep();
        if (step === 'rooms') {
            const code = mb.space.officeCode.trim() ? mb.space.officeCode : '—';
            return `Codice ${code} · ${this.modalUnresolvedTicketTotal()} segnalazioni attive`;
        }
        if (step === 'desks') {
            return this.selectedRoomTitle();
        }
        const desk = this.selectedDesk();
        return desk ? `Postazione ${deskRowPrimaryLabel(desk)}` : modalStepHeading(step);
    }

    protected deskRowSecondaryLabel(desk: Desk): string {
        const bundle = this.modalBundle();
        const n = bundle ? unresolvedTicketsForDesk(bundle, desk.id).length : 0;
        return deskRowSecondaryLabel(desk, n);
    }

    protected trackRoomRow(_i: number, r: ModalRoomRow): string {
        return r.key;
    }

    protected trackDeskId(_i: number, d: Desk): number {
        return d.id;
    }

    protected trackTicketId(_i: number, t: Record<string, unknown>): unknown {
        return t['ticketID'] ?? _i;
    }

    protected ticketHeading(t: Record<string, unknown>): string {
        return ticketHeading(t);
    }

    protected ticketRefLineOnly(t: Record<string, unknown>): string {
        return ticketRefLineOnly(t);
    }

    protected ticketStatusForProgress(ticket: Record<string, unknown>): string {
        const s = ticket['status'];
        return typeof s === 'string' && s.trim() ? s : 'OPEN';
    }

    protected ticketSeverityLabel(severity: unknown): string {
        return technicianTicketSeverityLabel(typeof severity === 'string' ? severity : null) || '—';
    }

    protected formatTicketWhen(iso: unknown): string {
        return formatShortDateTime(iso as string);
    }

    protected formatTicketEstimatedAt(value: unknown): string {
        const formatted = formatShortDateTime(typeof value === 'string' ? value : null);
        return formatted || '—';
    }

    protected ticketNumericId(ticket: Record<string, unknown>): number | null {
        return ticketNumericId(ticket);
    }

    protected canAddComment(ticket: Record<string, unknown>): boolean {
        const s = String(ticket['status'] ?? '').toUpperCase();
        return s !== 'RESOLVED' && s !== 'CLOSED';
    }

    protected commentDraft(ticketId: number): string {
        return this.modal.commentDraft(ticketId);
    }

    protected setCommentDraft(ticketId: number, value: string): void {
        this.modal.setCommentDraft(ticketId, value);
    }

    protected commentError(ticketId: number): string {
        return this.modal.commentError(ticketId);
    }

    protected isCommentSending(ticketId: number): boolean {
        return this.modal.isCommentSending(ticketId);
    }

    protected submitComment(ticketId: number): void {
        this.modal.submitComment(ticketId);
    }

    protected ticketNeedsTechnicianAssignment(ticket: Record<string, unknown>): boolean {
        return ticketNeedsTechnicianAssignment(ticket);
    }

    protected isActiveTicket(t: Record<string, unknown>): boolean {
        return isActiveTicket(t);
    }

    protected openAssignTicketTechnicianModal(ticket: Record<string, unknown>): void {
        this.modal.openAssignTicketTechnicianModal(ticket);
    }

    protected deskRowPrimaryLabel(desk: Desk): string {
        return deskRowPrimaryLabel(desk);
    }

    protected ticketToChatSource(ticket: Record<string, unknown>): any {
        return ticket;
    }
}
