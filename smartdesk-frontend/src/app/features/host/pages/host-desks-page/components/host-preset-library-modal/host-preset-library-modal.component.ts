import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostCustomAmenityPreset } from '../../host-desks.util';
import { HOST_DESKS_AMENITY_TOKEN_MAX, HostDesksModalStore } from '../../host-desks-modal.store';

@Component({
    selector: 'app-host-preset-library-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './host-preset-library-modal.component.html',
    styleUrl: './host-preset-library-modal.component.scss'
})
export class HostPresetLibraryModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly hostPresetLibraryModalOpen = this.modal.hostPresetLibraryModalOpen;
    readonly editingHostPresetId = this.modal.editingHostPresetId;
    readonly editPresetAmenities = this.modal.editPresetAmenities;
    readonly hostPresetLibraryFilter = this.modal.hostPresetLibraryFilter;
    readonly filteredHostCustomPresets = this.modal.filteredHostCustomPresets;
    readonly hostCustomPresets = this.modal.hostCustomPresets;
    readonly presetsMutating = this.modal.presetsMutating;
    readonly loadingPresets = this.modal.loadingPresets;
    protected readonly hostAmenityTokenMax = HOST_DESKS_AMENITY_TOKEN_MAX;

    protected get editHostPresetFormError(): string {
        return this.modal.editHostPresetFormError;
    }
    protected get editHostPresetLabel(): string {
        return this.modal.editHostPresetLabel;
    }
    protected set editHostPresetLabel(value: string) {
        this.modal.editHostPresetLabel = value;
    }
    protected get editHostPresetHint(): string {
        return this.modal.editHostPresetHint;
    }
    protected set editHostPresetHint(value: string) {
        this.modal.editHostPresetHint = value;
    }
    protected get editPresetAmenityDraft(): string {
        return this.modal.editPresetAmenityDraft;
    }

    protected closeHostPresetLibraryModal(): void {
        this.modal.closeHostPresetLibraryModal();
    }

    protected setEditPresetAmenityDraft(value: string): void {
        this.modal.setEditPresetAmenityDraft(value);
    }

    protected addAmenityToEditPresetForm(): void {
        this.modal.addAmenityToEditPresetForm();
    }

    protected removeAmenityFromEditPresetForm(token: string): void {
        this.modal.removeAmenityFromEditPresetForm(token);
    }

    protected saveEditHostPreset(): void {
        this.modal.saveEditHostPreset();
    }

    protected cancelEditHostPreset(): void {
        this.modal.cancelEditHostPreset();
    }

    protected isEditHostPresetFormValid(): boolean {
        return this.modal.isEditHostPresetFormValid();
    }

    protected openHostPresetCreateFromLibrary(): void {
        this.modal.openHostPresetCreateFromLibrary();
    }

    protected startEditHostPreset(p: HostCustomAmenityPreset): void {
        this.modal.startEditHostPreset(p);
    }

    protected deleteHostCustomPreset(preset: HostCustomAmenityPreset): void {
        this.modal.deleteHostCustomPreset(preset);
    }
}
