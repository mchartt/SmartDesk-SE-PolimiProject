import { Component, inject, input } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { Space } from '../../../../../../core/models';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-technician-spaces-list',
    standalone: true,
    imports: [MdbRippleModule, SdIconComponent],
    templateUrl: './technician-spaces-list.component.html',
    styleUrl: './technician-spaces-list.component.scss'
})
export class TechnicianSpacesListComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly approvedSpaces = input.required<Space[]>();

    readonly selectedSpaceId = this.modal.selectedSpaceId;

    protected openSpaceTechniciansModal(space: Space): void {
        this.modal.openSpaceTechniciansModal(space);
    }

    protected spaceCardAriaLabel(space: Space): string {
        return this.modal.spaceCardAriaLabel(space);
    }

    protected technicianCountForSpace(spaceId: number): number {
        return this.modal.technicianCountForSpace(spaceId);
    }
}
