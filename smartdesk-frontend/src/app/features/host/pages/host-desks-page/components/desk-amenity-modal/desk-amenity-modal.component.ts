import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk } from '../../../../../../core/models';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { AmenityPreset } from '../../host-desks.util';
import { HOST_DESKS_AMENITY_TOKEN_MAX, HostDesksModalStore } from '../../host-desks-modal.store';

@Component({
    selector: 'app-desk-amenity-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './desk-amenity-modal.component.html',
    styleUrl: './desk-amenity-modal.component.scss'
})
export class DeskAmenityModalComponent {
    protected readonly modal = inject(HostDesksModalStore);

    readonly deskAmenityModalDesk = this.modal.deskAmenityModalDesk;
    readonly deskAmenitySubModal = this.modal.deskAmenitySubModal;
    readonly hostCustomPresets = this.modal.hostCustomPresets;
    readonly loadingPresets = this.modal.loadingPresets;
    protected readonly hostAmenityTokenMax = HOST_DESKS_AMENITY_TOKEN_MAX;

    protected get deskAmenityDraft(): string {
        return this.modal.deskAmenityDraft;
    }

    protected closeDeskAmenityModal(): void {
        this.modal.closeDeskAmenityModal();
    }

    protected openDeskAmenityActiveSubModal(): void {
        this.modal.openDeskAmenityActiveSubModal();
    }

    protected openDeskAmenityApplySetSubModal(): void {
        this.modal.openDeskAmenityApplySetSubModal();
    }

    protected closeDeskAmenitySubModal(): void {
        this.modal.closeDeskAmenitySubModal();
    }

    protected setDeskAmenityDraft(value: string): void {
        this.modal.setDeskAmenityDraft(value);
    }

    protected addDeskAmenityFromModal(): void {
        this.modal.addDeskAmenityFromModal();
    }

    protected formatDeskTitle(code: string): string {
        return this.modal.formatDeskTitle(code);
    }

    protected removeAmenity(desk: Desk, amenity: string): void {
        this.modal.removeAmenity(desk, amenity);
    }

    protected openHostPresetCreateFromDeskAmenityModal(): void {
        this.modal.openHostPresetCreateFromDeskAmenityModal();
    }

    protected presetApplySearchForDesk(deskId: number): string {
        return this.modal.presetApplySearchForDesk(deskId);
    }

    protected setPresetApplySearchForDesk(deskId: number, value: string): void {
        this.modal.setPresetApplySearchForDesk(deskId, value);
    }

    protected filteredPresetsForDesk(desk: Desk): AmenityPreset[] {
        return this.modal.filteredPresetsForDesk(desk);
    }

    protected presetFullyApplied(desk: Desk, preset: AmenityPreset): boolean {
        return this.modal.presetFullyApplied(desk, preset);
    }

    protected applyAmenityPreset(desk: Desk, preset: AmenityPreset): void {
        this.modal.applyAmenityPreset(desk, preset);
    }
}
