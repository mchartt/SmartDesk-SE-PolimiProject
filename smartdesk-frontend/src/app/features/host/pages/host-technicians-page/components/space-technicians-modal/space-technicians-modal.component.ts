import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { Space } from '../../../../../../core/models';
import { HostTechnicianDto } from '../../../../../../core/services/host.service';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';

@Component({
    selector: 'app-space-technicians-modal',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './space-technicians-modal.component.html',
    styleUrl: './space-technicians-modal.component.scss'
})
export class SpaceTechniciansModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly spaceTechModalOpen = this.modal.spaceTechModalOpen;
    readonly spaceModalTechSearch = this.modal.spaceModalTechSearch;
    readonly spaceModalFilteredTechnicians = this.modal.spaceModalFilteredTechnicians;

    protected closeSpaceTechniciansModal(): void {
        this.modal.closeSpaceTechniciansModal();
    }

    protected selectedSpace(): Space | null {
        return this.modal.selectedSpace();
    }

    protected spaceSubtitle(): string | null {
        const sp = this.selectedSpace();
        if (!sp) {
            return null;
        }
        return (
            (sp.officeCode.trim() ? sp.officeCode + ' · ' : '') + sp.city + ' · ' + sp.address
        );
    }

    protected get hasNoApprovedSpaces(): boolean {
        return this.modal.hasNoApprovedSpaces();
    }

    protected loadingTechs(): boolean {
        return this.modal.loadingTechs();
    }

    protected techInitials(name: string): string {
        return this.modal.techInitials(name);
    }

    protected technicianDisplayCode(t: HostTechnicianDto): string {
        return this.modal.technicianDisplayCode(t);
    }

    protected unassign(technicianId: number): void {
        this.modal.unassign(technicianId);
    }
}
