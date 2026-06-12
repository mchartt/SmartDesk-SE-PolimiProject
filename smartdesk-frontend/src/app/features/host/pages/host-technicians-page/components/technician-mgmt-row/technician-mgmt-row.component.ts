import { Component, inject, input } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTechnicianDto } from '../../../../../../core/services/host.service';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-technician-mgmt-row',
    standalone: true,
    imports: [MdbRippleModule, SdIconComponent],
    templateUrl: './technician-mgmt-row.component.html',
    styleUrl: './technician-mgmt-row.component.scss'
})
export class TechnicianMgmtRowComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly technician = input.required<HostTechnicianDto>();

    protected get hasNoApprovedSpaces(): boolean {
        return this.modal.hasNoApprovedSpaces();
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

    protected visibleAssignedSpaces(t: HostTechnicianDto) {
        return this.modal.visibleAssignedSpaces(t);
    }

    protected hiddenAssignedSpacesCount(t: HostTechnicianDto): number {
        return this.modal.hiddenAssignedSpacesCount(t);
    }

    protected assignableSpaceCount(t: HostTechnicianDto): number {
        return this.modal.assignableSpaceCount(t);
    }

    protected unassignFromHostSpace(spaceId: number, technicianId: number): void {
        this.modal.unassignFromHostSpace(spaceId, technicianId);
    }

    protected openAssignedSpacesModal(t: HostTechnicianDto): void {
        this.modal.openAssignedSpacesModal(t);
    }

    protected openSpacePickModal(t: HostTechnicianDto): void {
        this.modal.openSpacePickModal(t);
    }

    protected openEditModal(t: HostTechnicianDto): void {
        this.modal.openEditModal(t);
    }

    protected confirmDeleteTechnician(t: HostTechnicianDto): void {
        this.modal.confirmDeleteTechnician(t);
    }
}
