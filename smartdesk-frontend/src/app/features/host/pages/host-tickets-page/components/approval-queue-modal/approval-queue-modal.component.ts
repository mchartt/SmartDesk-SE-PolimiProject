import { Component, inject, input } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk } from '../../../../../../core/models';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { HostTicketsModalStore } from '../../host-tickets-modal.store';
import { HostPendingApprovalItem, ticketHeading, ticketNumericId, ticketRefLineOnly } from '../../host-tickets.util';

@Component({
    selector: 'app-approval-queue-modal',
    standalone: true,
    imports: [MdbRippleModule, SdModalHeaderComponent],
    templateUrl: './approval-queue-modal.component.html',
    styleUrl: './approval-queue-modal.component.scss'
})
export class ApprovalQueueModalComponent {
    readonly allVerifyingTickets = input.required<HostPendingApprovalItem[]>();

    protected readonly modal = inject(HostTicketsModalStore);

    readonly approvalQueueModalOpen = this.modal.approvalQueueModalOpen;
    readonly pendingApprovalViewMode = this.modal.pendingApprovalViewMode;
    readonly pendingApprovalSpaceModalDesksByRoom = this.modal.pendingApprovalSpaceModalDesksByRoom;
    readonly isActionPerforming = this.modal.isActionPerforming;

    protected closeApprovalQueueModal(): void {
        this.modal.closeApprovalQueueModal();
    }

    protected ticketRefLineOnly(t: Record<string, unknown>): string {
        return ticketRefLineOnly(t);
    }

    protected ticketHeading(t: Record<string, unknown>): string {
        return ticketHeading(t);
    }

    protected ticketNumericId(ticket: Record<string, unknown>): number | null {
        return ticketNumericId(ticket);
    }

    protected requestApproveTicket(ticketId: number): void {
        this.modal.requestApproveTicket(ticketId);
    }

    protected openMaintenanceReassignModal(row: HostPendingApprovalItem): void {
        this.modal.openMaintenanceReassignModal(row);
    }

    protected requestDismissDesk(ticketId: number): void {
        this.modal.requestDismissDesk(ticketId);
    }

    protected approveInspection(desk: Desk): void {
        this.modal.approveInspection(desk);
    }

    protected rejectInspection(desk: Desk): void {
        this.modal.rejectInspection(desk);
    }

    protected decommissionDesk(desk: Desk): void {
        this.modal.decommissionDesk(desk);
    }

    protected deskActionBusy(deskId: number): boolean {
        return this.modal.deskActionBusy(deskId);
    }

    protected approveRoomInspections(items: HostPendingApprovalItem[]): void {
        this.modal.approveRoomInspections(items);
    }
}
