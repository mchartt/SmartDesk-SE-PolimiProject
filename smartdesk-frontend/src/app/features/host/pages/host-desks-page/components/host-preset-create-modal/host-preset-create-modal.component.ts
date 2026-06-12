import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HOST_DESKS_AMENITY_TOKEN_MAX, HostDesksModalStore } from '../../host-desks-modal.store';

@Component({
    selector: 'app-host-preset-create-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './host-preset-create-modal.component.html',
    styleUrl: './host-preset-create-modal.component.scss'
})
export class HostPresetCreateModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly isHostPresetFormOpen = this.modal.isHostPresetFormOpen;
    readonly newPresetAmenities = this.modal.newPresetAmenities;
    readonly presetsMutating = this.modal.presetsMutating;
    readonly loadingPresets = this.modal.loadingPresets;
    protected readonly hostAmenityTokenMax = HOST_DESKS_AMENITY_TOKEN_MAX;

    protected get hostPresetFormError(): string {
        return this.modal.hostPresetFormError;
    }
    protected get newHostPresetLabel(): string {
        return this.modal.newHostPresetLabel;
    }
    protected set newHostPresetLabel(value: string) {
        this.modal.newHostPresetLabel = value;
    }
    protected get newHostPresetHint(): string {
        return this.modal.newHostPresetHint;
    }
    protected set newHostPresetHint(value: string) {
        this.modal.newHostPresetHint = value;
    }
    protected get newPresetAmenityDraft(): string {
        return this.modal.newPresetAmenityDraft;
    }

    protected closeHostPresetCreateModal(): void {
        this.modal.closeHostPresetCreateModal();
    }

    protected setNewPresetAmenityDraft(value: string): void {
        this.modal.setNewPresetAmenityDraft(value);
    }

    protected addAmenityToNewPresetForm(): void {
        this.modal.addAmenityToNewPresetForm();
    }

    protected removeAmenityFromNewPresetForm(token: string): void {
        this.modal.removeAmenityFromNewPresetForm(token);
    }

    protected isNewHostPresetFormValid(): boolean {
        return this.modal.isNewHostPresetFormValid();
    }

    protected saveNewHostPreset(): void {
        this.modal.saveNewHostPreset();
    }
}
