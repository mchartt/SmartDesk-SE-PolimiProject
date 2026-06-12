import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { Space } from '../../../../../../core/models';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-space-pick-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './space-pick-modal.component.html',
    styleUrl: './space-pick-modal.component.scss'
})
export class SpacePickModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly isSpacePickModalOpen = this.modal.isSpacePickModalOpen;
    readonly spacePickTechnicianLabel = this.modal.spacePickTechnicianLabel;
    readonly spacePickSearchQuery = this.modal.spacePickSearchQuery;
    readonly spacePickAssigning = this.modal.spacePickAssigning;
    readonly spacesPickFilteredList = this.modal.spacesPickFilteredList;

    protected closeSpacePickModal(): void {
        this.modal.closeSpacePickModal();
    }

    protected assignTechnicianToSpaceFromPick(space: Space): void {
        this.modal.assignTechnicianToSpaceFromPick(space);
    }
}
