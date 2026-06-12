import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';
import { HOST_TECH_RECENT_LIMIT, HostTechniciansModalStore } from '../../host-technicians-modal.store';
import { TechnicianMgmtRowComponent } from '../technician-mgmt-row/technician-mgmt-row.component';

@Component({
    selector: 'app-technician-mgmt-dashboard',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdIconComponent, TechnicianMgmtRowComponent],
    templateUrl: './technician-mgmt-dashboard.component.html',
    styleUrl: './technician-mgmt-dashboard.component.scss'
})
export class TechnicianMgmtDashboardComponent {
    protected readonly modal = inject(HostTechniciansModalStore);

    protected readonly recentTechDashboardLimit = HOST_TECH_RECENT_LIMIT;

    readonly loadingAllTechs = this.modal.loadingAllTechs;
    readonly techQuickQuery = this.modal.techQuickQuery;
    readonly dashboardTechnicians = this.modal.dashboardTechnicians;
    readonly showDashboardInlineSearch = this.modal.showDashboardInlineSearch;
    readonly showSeeAllTechniciansCta = this.modal.showSeeAllTechniciansCta;
    readonly technicianTotalCount = this.modal.technicianTotalCount;
    readonly isTechSearchModalOpen = this.modal.isTechSearchModalOpen;
    readonly techMgmtModalFiltersActive = this.modal.techMgmtModalFiltersActive;

    protected allTechniciansCount(): number {
        return this.modal.allTechnicians().length;
    }

    protected openTechSearchModal(): void {
        this.modal.openTechSearchModal();
    }
}
