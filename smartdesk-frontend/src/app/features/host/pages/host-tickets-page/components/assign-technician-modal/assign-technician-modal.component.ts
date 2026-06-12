import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { HostTechnicianDto } from '../../../../../../core/services/host.service';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HOST_TICKET_SEVERITY_OPTIONS, HostTicketsModalStore } from '../../host-tickets-modal.store';
import {
    assignedTechnicianLabel,
    isCurrentAssignTechnician,
    maintDeskMaintenanceNotice,
    ticketCodeSubtitle,
    ticketHeading,
    ticketRefLineOnly,
    technicianDisplayCode
} from '../../host-tickets.util';

@Component({
    selector: 'app-assign-technician-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './assign-technician-modal.component.html',
    styleUrl: './assign-technician-modal.component.scss'
})
export class AssignTechnicianModalComponent {
    protected readonly modal = inject(HostTicketsModalStore);

    protected readonly ticketSeverityOptions = HOST_TICKET_SEVERITY_OPTIONS;

    readonly assignTicketTechModalTicket = this.modal.assignTicketTechModalTicket;
    readonly assignModalContext = this.modal.assignModalContext;
    readonly assignTicketTechError = this.modal.assignTicketTechError;
    readonly assignTicketTechLoading = this.modal.assignTicketTechLoading;
    readonly assignTicketTechAssigning = this.modal.assignTicketTechAssigning;
    readonly assignTicketTechSearch = this.modal.assignTicketTechSearch;
    readonly assignTicketSeverity = this.modal.assignTicketSeverity;
    readonly assignTicketTechFiltered = this.modal.assignTicketTechFiltered;
    readonly isReassigning = this.modal.isReassigning;

    protected closeAssignTicketTechModal(): void {
        this.modal.closeAssignTicketTechModal();
    }

    protected ticketHeading(t: Record<string, unknown>): string {
        return ticketHeading(t);
    }

    protected ticketCodeSubtitle(t: Record<string, unknown>): string | null {
        return ticketCodeSubtitle(t);
    }

    protected ticketRefLineOnly(t: Record<string, unknown>): string {
        return ticketRefLineOnly(t);
    }

    protected assignedTechnicianLabel(ticket: Record<string, unknown>): string {
        return assignedTechnicianLabel(ticket);
    }

    protected maintDeskMaintenanceNotice(): string {
        return maintDeskMaintenanceNotice(this.assignModalContext()?.desk);
    }

    protected assignModalSameTechnicianVisible(): boolean {
        return this.modal.assignModalSameTechnicianVisible();
    }

    protected isMaintReassignSameSelected(): boolean {
        return this.modal.isMaintReassignSameSelected();
    }

    protected selectMaintReassignSame(): void {
        this.modal.selectMaintReassignSame();
    }

    protected isMaintReassignTechSelected(technicianId: number): boolean {
        return this.modal.isMaintReassignTechSelected(technicianId);
    }

    protected selectMaintReassignTechnician(technicianId: number): void {
        this.modal.selectMaintReassignTechnician(technicianId);
    }

    protected isCurrentAssignTechnician(tech: HostTechnicianDto): boolean {
        return isCurrentAssignTechnician(this.assignTicketTechModalTicket(), tech);
    }

    protected technicianDisplayCode(t: HostTechnicianDto): string {
        return technicianDisplayCode(t);
    }

    protected canConfirmMaintReassign(): boolean {
        return this.modal.canConfirmMaintReassign();
    }

    protected confirmMaintReassignChoice(): void {
        this.modal.confirmMaintReassignChoice();
    }

    protected confirmAssignTicketTechnician(technicianId: number): void {
        this.modal.confirmAssignTicketTechnician(technicianId);
    }
}
