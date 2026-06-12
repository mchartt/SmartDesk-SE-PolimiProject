import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import {
    HOST_DESKS_ROOM_CODE_MAX_LEN,
    HOST_DESKS_ROOM_NAME_MAX_LEN,
    HostDesksModalStore
} from '../../host-desks-modal.store';

@Component({
    selector: 'app-room-form-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent],
    templateUrl: './room-form-modal.component.html',
    styleUrl: './room-form-modal.component.scss'
})
export class RoomFormModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly isRoomModalOpen = this.modal.isRoomModalOpen;
    protected readonly roomNameMaxLen = HOST_DESKS_ROOM_NAME_MAX_LEN;
    protected readonly roomCodeMaxLen = HOST_DESKS_ROOM_CODE_MAX_LEN;

    protected get editingRoomId(): number | null {
        return this.modal.editingRoomId;
    }
    protected get roomFormName(): string {
        return this.modal.roomFormName;
    }
    protected set roomFormName(value: string) {
        this.modal.roomFormName = value;
    }
    protected get roomFormCode(): string {
        return this.modal.roomFormCode;
    }
    protected set roomFormCode(value: string) {
        this.modal.roomFormCode = value;
    }
    protected get errorMsg(): string {
        return this.modal.pageError();
    }

    protected closeRoomModal(): void {
        this.modal.closeRoomModal();
    }

    protected saveRoom(): void {
        this.modal.saveRoom();
    }
}
