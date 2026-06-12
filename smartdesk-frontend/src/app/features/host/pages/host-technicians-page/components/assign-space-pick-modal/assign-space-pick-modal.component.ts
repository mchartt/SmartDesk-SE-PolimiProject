import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { Space } from '../../../../../../core/models';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-assign-space-pick-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './assign-space-pick-modal.component.html',
    styleUrl: './assign-space-pick-modal.component.scss'
})
export class AssignSpacePickModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly assignPickSpaceModalOpen = this.modal.assignPickSpaceModalOpen;
    readonly assignPickSpaceSearch = this.modal.assignPickSpaceSearch;

    protected closeAssignSpaceModal(): void {
        this.modal.closeAssignSpaceModal();
    }

    protected assignSpacesAssignmentFiltered(): Space[] {
        return this.modal.assignSpacesAssignmentFiltered();
    }

    protected isAssignSpacePickSelected(spaceId: number): boolean {
        return this.modal.isAssignSpacePickSelected(spaceId);
    }

    protected pickSpaceForAssignment(space: Space): void {
        this.modal.pickSpaceForAssignment(space);
    }
}
