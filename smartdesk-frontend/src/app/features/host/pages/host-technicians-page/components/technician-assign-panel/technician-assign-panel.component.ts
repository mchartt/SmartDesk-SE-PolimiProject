import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { Space } from '../../../../../../core/models';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-technician-assign-panel',
    standalone: true,
    imports: [RouterLink, MdbRippleModule, SdIconComponent],
    templateUrl: './technician-assign-panel.component.html',
    styleUrl: './technician-assign-panel.component.scss'
})
export class TechnicianAssignPanelComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly hasNoSpaces = input.required<boolean>();
    readonly hasNoApprovedSpaces = input.required<boolean>();
    readonly dimmed = input(false);

    readonly assignmentTicketContext = this.modal.assignmentTicketContext;
    readonly assignPickSpaceModalOpen = this.modal.assignPickSpaceModalOpen;
    readonly assignPickTechModalOpen = this.modal.assignPickTechModalOpen;
    readonly technicianIdToAssign = this.modal.technicianIdToAssign;
    readonly assignFlowBusy = this.modal.assignFlowBusy;
    readonly selectedSpaceId = this.modal.selectedSpaceId;

    protected clearAssignmentTicketContext(): void {
        this.modal.clearAssignmentTicketContext();
    }

    protected openAssignSpaceModal(): void {
        this.modal.openAssignSpaceModal();
    }

    protected openAssignTechModal(): void {
        this.modal.openAssignTechModal();
    }

    protected assignSpacePickerPrimary(): string {
        return this.modal.assignSpacePickerPrimary();
    }

    protected assignSpacePickerSecondary(): string {
        return this.modal.assignSpacePickerSecondary();
    }

    protected assignTechPickerPrimary(): string {
        return this.modal.assignTechPickerPrimary();
    }

    protected assignTechPickerSecondary(): string {
        return this.modal.assignTechPickerSecondary();
    }

    protected selectedSpace(): Space | null {
        return this.modal.selectedSpace();
    }

    protected assignTechnician(): void {
        this.modal.assignTechnician();
    }
}
