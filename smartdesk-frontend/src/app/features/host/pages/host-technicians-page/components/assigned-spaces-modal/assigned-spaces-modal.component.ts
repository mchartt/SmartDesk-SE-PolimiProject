import { Component, inject } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-assigned-spaces-modal',
    standalone: true,
    imports: [MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './assigned-spaces-modal.component.html',
    styleUrl: './assigned-spaces-modal.component.scss'
})
export class AssignedSpacesModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly assignedSpacesModalTechnician = this.modal.assignedSpacesModalTechnician;

    protected get hasNoApprovedSpaces(): boolean {
        return this.modal.hasNoApprovedSpaces();
    }

    protected closeAssignedSpacesModal(): void {
        this.modal.closeAssignedSpacesModal();
    }

    protected unassignFromHostSpace(spaceId: number, technicianId: number): void {
        this.modal.unassignFromHostSpace(spaceId, technicianId);
    }
}
