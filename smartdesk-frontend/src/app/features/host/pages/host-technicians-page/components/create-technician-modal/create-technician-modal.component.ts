import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-create-technician-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './create-technician-modal.component.html',
    styleUrl: './create-technician-modal.component.scss'
})
export class CreateTechnicianModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly isModalOpen = this.modal.isModalOpen;

    protected get createNome(): string {
        return this.modal.createNome;
    }
    protected set createNome(value: string) {
        this.modal.createNome = value;
    }
    protected get createCognome(): string {
        return this.modal.createCognome;
    }
    protected set createCognome(value: string) {
        this.modal.createCognome = value;
    }
    protected get email(): string {
        return this.modal.email;
    }
    protected set email(value: string) {
        this.modal.email = value;
    }
    protected get password(): string {
        return this.modal.password;
    }
    protected set password(value: string) {
        this.modal.password = value;
    }
    protected get specialization(): string {
        return this.modal.specialization;
    }
    protected set specialization(value: string) {
        this.modal.specialization = value;
    }
    protected get errorMsg(): string {
        return this.modal.pageError();
    }

    protected closeModal(): void {
        this.modal.closeModal();
    }

    protected createTechnician(): void {
        this.modal.createTechnician();
    }

    protected createNomeInvalid(): boolean {
        return this.modal.createNomeInvalid();
    }

    protected createCognomeInvalid(): boolean {
        return this.modal.createCognomeInvalid();
    }

    protected createEmailInvalid(): boolean {
        return this.modal.createEmailInvalid();
    }

    protected createPasswordInvalid(): boolean {
        return this.modal.createPasswordInvalid();
    }

    protected createSpecializationInvalid(): boolean {
        return this.modal.createSpecializationInvalid();
    }

    protected createNomeInlineError(): string | null {
        return this.modal.createNomeInlineError();
    }

    protected createCognomeInlineError(): string | null {
        return this.modal.createCognomeInlineError();
    }

    protected createEmailInlineError(): string | null {
        return this.modal.createEmailInlineError();
    }

    protected createPasswordInlineError(): string | null {
        return this.modal.createPasswordInlineError();
    }

    protected createSpecializationInlineError(): string | null {
        return this.modal.createSpecializationInlineError();
    }
}
