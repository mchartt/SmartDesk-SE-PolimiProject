import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HostTechniciansModalStore } from '../../host-technicians-modal.store';
import { TechnicianMgmtRowComponent } from '../technician-mgmt-row/technician-mgmt-row.component';

@Component({
    selector: 'app-technician-search-modal',
    standalone: true,
    imports: [
        FormsModule,
        MdbFormsModule,
        MdbRippleModule,
        SdModalHeaderComponent,
        SdIconComponent,
        TechnicianMgmtRowComponent
    ],
    templateUrl: './technician-search-modal.component.html',
    styleUrl: './technician-search-modal.component.scss'
})
export class TechnicianSearchModalComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    readonly isTechSearchModalOpen = this.modal.isTechSearchModalOpen;
    readonly techModalUnifiedSearch = this.modal.techModalUnifiedSearch;
    readonly techFilterNome = this.modal.techFilterNome;
    readonly techFilterCognome = this.modal.techFilterCognome;
    readonly techFilterCodice = this.modal.techFilterCodice;
    readonly modalFilteredTechnicians = this.modal.modalFilteredTechnicians;
    readonly technicianTotalCount = this.modal.technicianTotalCount;
    readonly techMgmtModalFiltersActive = this.modal.techMgmtModalFiltersActive;

    protected closeTechSearchModal(): void {
        this.modal.closeTechSearchModal();
    }

    protected clearTechModalFilters(): void {
        this.modal.clearTechModalFilters();
    }
}
