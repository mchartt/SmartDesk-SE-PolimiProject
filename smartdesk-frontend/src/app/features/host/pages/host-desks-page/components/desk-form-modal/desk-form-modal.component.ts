import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostDesksModalStore } from '../../host-desks-modal.store';

@Component({
    selector: 'app-desk-form-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './desk-form-modal.component.html',
    styleUrl: './desk-form-modal.component.scss'
})
export class DeskFormModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly isDeskModalOpen = this.modal.isDeskModalOpen;
    readonly rooms = this.modal.rooms;
    readonly previewDeskCode = this.modal.previewDeskCode;

    protected get editingDeskId(): number | null {
        return this.modal.editingDeskId;
    }
    protected get deskCode(): string {
        return this.modal.deskCode;
    }
    protected get deskRoomId(): number | null {
        return this.modal.deskRoomId;
    }
    protected set deskRoomId(value: number | null) {
        this.modal.deskRoomId = value;
    }
    protected get errorMsg(): string {
        return this.modal.pageError();
    }

    protected closeDeskModal(): void {
        this.modal.closeDeskModal();
    }

    protected saveDesk(): void {
        this.modal.saveDesk();
    }
}
