import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { HostService, HostTechnicianDto } from '../../../../core/services/host.service';
import { Space } from '../../../../core/models';
import {
    HostTechnicianAssignmentTicketContext,
    isSpaceApproved,
    normalizeTechnicianRow,
    parseTechnicianAssignmentDeepLink
} from './host-technicians.util';
import { HostTechniciansModalStore } from './host-technicians-modal.store';
import { CreateTechnicianModalComponent } from './components/create-technician-modal/create-technician-modal.component';
import { EditTechnicianModalComponent } from './components/edit-technician-modal/edit-technician-modal.component';
import { AssignedSpacesModalComponent } from './components/assigned-spaces-modal/assigned-spaces-modal.component';
import { TechnicianSearchModalComponent } from './components/technician-search-modal/technician-search-modal.component';
import { SpacePickModalComponent } from './components/space-pick-modal/space-pick-modal.component';
import { AssignSpacePickModalComponent } from './components/assign-space-pick-modal/assign-space-pick-modal.component';
import { AssignTechPickModalComponent } from './components/assign-tech-pick-modal/assign-tech-pick-modal.component';
import { SpaceTechniciansModalComponent } from './components/space-technicians-modal/space-technicians-modal.component';
import { TechnicianAssignPanelComponent } from './components/technician-assign-panel/technician-assign-panel.component';
import { TechnicianMgmtDashboardComponent } from './components/technician-mgmt-dashboard/technician-mgmt-dashboard.component';
import { TechnicianSpacesListComponent } from './components/technician-spaces-list/technician-spaces-list.component';

export type { HostTechnicianAssignmentTicketContext } from './host-technicians.util';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        MdbRippleModule,
        SdIconComponent,
        CreateTechnicianModalComponent,
        EditTechnicianModalComponent,
        AssignedSpacesModalComponent,
        TechnicianSearchModalComponent,
        SpacePickModalComponent,
        AssignSpacePickModalComponent,
        AssignTechPickModalComponent,
        SpaceTechniciansModalComponent,
        TechnicianAssignPanelComponent,
        TechnicianMgmtDashboardComponent,
        TechnicianSpacesListComponent
    ],
    providers: [HostTechniciansModalStore],
    templateUrl: './host-technicians-page.component.html',
    styleUrl: './host-technicians-page.component.scss'
})
export class HostTechniciansPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    private readonly modal = inject(HostTechniciansModalStore);

    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Tecnici');
    protected spaces: Space[] = [];
    protected approvedSpaces: Space[] = [];
    protected readonly selectedSpaceId = signal<number | null>(null);
    protected readonly techniciansSig = signal<HostTechnicianDto[]>([]);
    protected readonly allTechniciansSig = signal<HostTechnicianDto[]>([]);
    protected loading = signal(true);
    protected loadingTechs = signal(true);
    protected errorMsg = '';

    protected readonly assignSuccessMsg = this.modal.assignSuccessMsg;

    protected get hasNoSpaces(): boolean {
        return !this.loading() && this.spaces.length === 0;
    }

    protected get hasNoApprovedSpaces(): boolean {
        return !this.loading() && this.spaces.length > 0 && this.approvedSpaces.length === 0;
    }

    protected get selectedSpaceApproved(): boolean {
        return isSpaceApproved(this.approvedSpaces, this.selectedSpaceId());
    }

    public ngOnInit(): void {
        this.modal.bindHost({
            approvedSpaces: () => this.approvedSpaces,
            selectedSpaceId: this.selectedSpaceId,
            technicians: this.techniciansSig,
            allTechnicians: this.allTechniciansSig,
            loading: () => this.loading(),
            loadingTechs: this.loadingTechs,
            hasNoSpaces: () => this.hasNoSpaces,
            hasNoApprovedSpaces: () => this.hasNoApprovedSpaces,
            selectedSpace: () => this.selectedSpace(),
            selectedSpaceApproved: () => this.selectedSpaceApproved,
            pageError: () => this.errorMsg,
            onPageError: (message) => {
                this.errorMsg = message;
            },
            loadTechnicians: () => this.loadTechnicians(),
            loadAllTechnicians: () => this.modal.loadAllTechniciansFromApi()
        });
        this.loadInitialData();
    }

    protected openModal(): void {
        this.modal.openModal();
    }

    protected loadInitialData(): void {
        this.loading.set(true);
        this.errorMsg = '';
        this.hostService
            .getSpaces()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (spaces) => {
                    this.spaces = spaces;
                    this.approvedSpaces = spaces.filter((s) => s.approved);
                    this.loading.set(false);
                    const consumedDeepLink = this.consumeTechnicianAssignmentDeepLink();
                    if (!consumedDeepLink) {
                        if (this.approvedSpaces.length) {
                            this.selectedSpaceId.set(this.approvedSpaces[0].spaceID);
                            this.loadTechnicians();
                        } else {
                            this.selectedSpaceId.set(null);
                            this.loadingTechs.set(false);
                        }
                    }
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                    this.loading.set(false);
                    this.loadingTechs.set(false);
                }
            });
        this.modal.loadAllTechniciansFromApi();
    }

    private consumeTechnicianAssignmentDeepLink(): boolean {
        const qp = this.route.snapshot.queryParamMap;
        const parsed = parseTechnicianAssignmentDeepLink(
            {
                assignSpace: qp.get('assignSpace'),
                spaceId: qp.get('spaceId'),
                ticketId: qp.get('ticketId'),
                ticketCode: qp.get('ticketCode'),
                deskCode: qp.get('deskCode'),
                pickTech: qp.get('pickTech')
            },
            this.approvedSpaces.map((s) => s.spaceID)
        );
        const rawSpace = qp.get('assignSpace') ?? qp.get('spaceId');
        if (!parsed.consumed) {
            if (rawSpace?.trim()) {
                void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
            }
            return false;
        }
        if (parsed.invalidSpace) {
            this.errorMsg = "Impossibile aprire l'assegnazione: spazio non trovato o non ancora approvato.";
            void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
            return true;
        }
        this.modal.applyDeepLinkResult(parsed.spaceId, parsed.ticketContext, parsed.openTechPicker);
        void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
        return true;
    }

    protected loadTechnicians(): void {
        this.errorMsg = '';
        const sid = this.selectedSpaceId();
        if (sid == null) {
            this.techniciansSig.set([]);
            this.loadingTechs.set(false);
            return;
        }
        this.loadingTechs.set(true);
        this.hostService
            .getTechniciansForSpace(sid)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    this.techniciansSig.set(rows.map((r) => normalizeTechnicianRow(r)));
                    this.loadingTechs.set(false);
                },
                error: (err: Error) => {
                    this.techniciansSig.set([]);
                    this.errorMsg = err.message;
                    this.loadingTechs.set(false);
                }
            });
    }

    protected selectedSpace(): Space | null {
        const sid = this.selectedSpaceId();
        if (sid == null) {
            return null;
        }
        return this.approvedSpaces.find((s) => s.spaceID === sid) ?? null;
    }
}
