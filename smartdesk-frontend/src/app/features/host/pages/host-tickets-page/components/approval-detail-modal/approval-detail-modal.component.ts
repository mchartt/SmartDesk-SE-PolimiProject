import { Component, inject } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { TicketChatPanelComponent } from '../../../../../../shared/components/ticket-chat-panel/ticket-chat-panel.component';
import { TicketStatusProgressComponent } from '../../../../../../shared/components/ticket-status-progress/ticket-status-progress.component';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { HostTicketsModalStore } from '../../host-tickets-modal.store';
import {
    approvalDetailBullets,
    approvalResolutionSnippet,
    HostPendingApprovalItem,
    ticketHeading,
    ticketNumericId,
    workerLabel
} from '../../host-tickets.util';

@Component({
    selector: 'app-approval-detail-modal',
    standalone: true,
    imports: [MdbRippleModule, SdModalHeaderComponent, TicketChatPanelComponent, TicketStatusProgressComponent],
    templateUrl: './approval-detail-modal.component.html',
    styleUrl: './approval-detail-modal.component.scss'
})
export class ApprovalDetailModalComponent {
    protected readonly modal = inject(HostTicketsModalStore);

    readonly approvalDetailRow = this.modal.approvalDetailRow;

    protected closeApprovalTicketDetail(): void {
        this.modal.closeApprovalTicketDetail();
    }

    protected ticketHeading(t: Record<string, unknown>): string {
        return ticketHeading(t);
    }

    protected workerLabel(ticket: Record<string, unknown>): string {
        return workerLabel(ticket);
    }

    protected approvalDetailBullets(row: HostPendingApprovalItem): { label: string; value: string }[] {
        return approvalDetailBullets(row);
    }

    protected approvalResolutionSnippet(ticket: Record<string, unknown>): string | null {
        return approvalResolutionSnippet(ticket);
    }

    protected ticketStatusForProgress(ticket: Record<string, unknown>): string {
        const s = ticket['status'];
        return typeof s === 'string' && s.trim() ? s : 'OPEN';
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

    protected ticketToChatSource(ticket: Record<string, unknown>): any {
        return ticket;
    }
}
