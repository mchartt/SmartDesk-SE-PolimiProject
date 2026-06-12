import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTechnicianDto } from '../../../../../../core/services/host.service';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-assign-tech-pick-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './assign-tech-pick-modal.component.html',
    styleUrl: './assign-tech-pick-modal.component.scss'
})
export class AssignTechPickModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly assignPickTechModalOpen = this.modal.assignPickTechModalOpen;
    readonly assignPickTechSearch = this.modal.assignPickTechSearch;

    protected closeAssignTechModal(): void {
        this.modal.closeAssignTechModal();
    }

    protected selectedSpaceName(): string | null {
        return this.modal.selectedSpace()?.name ?? null;
    }

    protected assignTechAssignmentFiltered(): HostTechnicianDto[] {
        return this.modal.assignTechAssignmentFiltered();
    }

    protected isAssignTechPickSelected(technicianId: number): boolean {
        return this.modal.isAssignTechPickSelected(technicianId);
    }

    protected pickTechnicianForAssignment(t: HostTechnicianDto): void {
        this.modal.pickTechnicianForAssignment(t);
    }

    protected techInitials(name: string): string {
        return this.modal.techInitials(name);
    }

    protected technicianDisplayCode(t: HostTechnicianDto): string {
        return this.modal.technicianDisplayCode(t);
    }

    protected technicianSpecDisplay(t: HostTechnicianDto): string {
        return this.modal.technicianSpecDisplay(t);
    }
}
