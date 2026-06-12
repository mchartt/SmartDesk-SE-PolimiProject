import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk, DeskStateCode } from '../../../../../../core/models';
import { HostRoom } from '../../../../../../core/services/host.service';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { DeskRoomSection } from '../../host-desks.util';
import { HostDesksModalStore } from '../../host-desks-modal.store';
import { DeskAmenityModalComponent } from '../desk-amenity-modal/desk-amenity-modal.component';

@Component({
    selector: 'app-room-browse-modal',
    standalone: true,
    imports: [
        FormsModule,
        MdbFormsModule,
        MdbRippleModule,
        SdModalHeaderComponent,
        SdIconComponent,
        DeskAmenityModalComponent
    ],
    templateUrl: './room-browse-modal.component.html',
    styleUrl: './room-browse-modal.component.scss'
})
export class RoomBrowseModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly roomBrowseModalKey = this.modal.roomBrowseModalKey;
    readonly browseModalSection = this.modal.browseModalSection;
    readonly deskAmenityModalDeskId = this.modal.deskAmenityModalDeskId;

    protected sectionKey(section: DeskRoomSection): string {
        return this.modal.sectionKey(section);
    }

    protected closeRoomBrowseModal(): void {
        this.modal.closeRoomBrowseModal();
    }

    protected closeRoomBrowseStack(): void {
        this.modal.closeRoomBrowseStack();
    }

    protected roomFilterInput(key: string): string {
        return this.modal.roomFilterInput(key);
    }

    protected setRoomFilter(key: string, value: string): void {
        this.modal.setRoomFilter(key, value);
    }

    protected filteredDesksInSection(section: DeskRoomSection): Desk[] {
        return this.modal.filteredDesksInSection(section);
    }

    protected deskStateBadgeClass(stateCode: DeskStateCode | string): string {
        return this.modal.deskStateBadgeClass(stateCode);
    }

    protected deskStateLabel(stateCode: DeskStateCode | string): string {
        return this.modal.deskStateLabel(stateCode);
    }

    protected deskCardRoomName(desk: Desk, sectionRoom: HostRoom | null): string {
        return this.modal.deskCardRoomName(desk, sectionRoom);
    }

    protected openDeskAmenityModal(desk: Desk): void {
        this.modal.openDeskAmenityModal(desk);
    }

    protected canEditDesk(state: DeskStateCode): boolean {
        return this.modal.canEditDesk(state);
    }

    protected editDesk(desk: Desk): void {
        this.modal.editDesk(desk);
    }

    protected deleteDesk(deskId: number): void {
        this.modal.deleteDesk(deskId);
    }
}
