import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-edit-technician-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent],
    templateUrl: './edit-technician-modal.component.html',
    styleUrl: './edit-technician-modal.component.scss'
})
export class EditTechnicianModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly isEditModalOpen = this.modal.isEditModalOpen;
    readonly editSaving = this.modal.editSaving;

    protected get editName(): string {
        return this.modal.editName;
    }
    protected set editName(value: string) {
        this.modal.editName = value;
    }
    protected get editEmail(): string {
        return this.modal.editEmail;
    }
    protected set editEmail(value: string) {
        this.modal.editEmail = value;
    }
    protected get editSpecialization(): string {
        return this.modal.editSpecialization;
    }
    protected set editSpecialization(value: string) {
        this.modal.editSpecialization = value;
    }
    protected get editPassword(): string {
        return this.modal.editPassword;
    }
    protected set editPassword(value: string) {
        this.modal.editPassword = value;
    }

    protected closeEditModal(): void {
        this.modal.closeEditModal();
    }

    protected saveEditedTechnician(): void {
        this.modal.saveEditedTechnician();
    }
}
