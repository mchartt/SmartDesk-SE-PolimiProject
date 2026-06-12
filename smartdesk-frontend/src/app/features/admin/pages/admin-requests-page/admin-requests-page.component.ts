import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { filter, switchMap } from 'rxjs/operators';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { AdminService } from '../../../../core/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { sortPendingHostsNewestFirst } from '../../../../core/utils/pending-hosts.util';
import { sortPendingSpacesNewestFirst } from '../../../../core/utils/pending-spaces.util';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { haystackMatchesTokenSearch } from '../../../../core/utils/search.util';
const PREVIEW_LIMIT = 3;
type HostRow = {
    userID: number;
    name: string;
    surname: string;
    email: string;
    status?: string;
    description?: string;
    nameStructure?: string;
    registeredAt?: string;
};
type PendingSpaceRow = {
    spaceID: number;
    name: string;
    city: string;
    address: string;
    description: string;
    officeCode: string;
    hostName?: string;
};
@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        EmptyStateComponent,
        MdbFormsModule,
        MdbRippleModule,
        SdModalHeaderComponent,
        SdIconComponent
    ],
    templateUrl: './admin-requests-page.component.html',
    styleUrl: './admin-requests-page.component.scss'
})
export class AdminRequestsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    private readonly notifications = inject(NotificationService);
    private readonly confirmService = inject(ConfirmModalService);
    protected readonly previewLimit = PREVIEW_LIMIT;
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Richieste');
    protected readonly hosts = signal<HostRow[]>([]);
    protected readonly spaces = signal<PendingSpaceRow[]>([]);
    protected readonly hostSectionSearch = signal('');
    protected readonly spaceSectionSearch = signal('');
    protected readonly modalSearch = signal('');
    protected readonly requestsModalOpen = signal(false);
    protected readonly expandedDescriptionUserId = signal<number | null>(null);
    protected readonly expandedModalHostUserId = signal<number | null>(null);
    protected readonly expandedModalSpaceId = signal<number | null>(null);
    protected readonly expandedPreviewSpaceId = signal<number | null>(null);
    protected errorMsg = '';
    protected readonly filteredHosts = computed(() => {
        const q = this.hostSectionSearch().trim();
        const list = this.hosts();
        if (!q)
            return list;
        return list.filter((h) => AdminRequestsPageComponent.matchesHostSearch(h, q));
    });
    protected readonly filteredSpaces = computed(() => {
        const q = this.spaceSectionSearch().trim();
        const list = this.spaces();
        if (!q)
            return list;
        return list.filter((s) => AdminRequestsPageComponent.matchesSpaceSearch(s, q));
    });
    protected readonly hostPreviewRows = computed(() => this.filteredHosts().slice(0, PREVIEW_LIMIT));
    protected readonly spacePreviewRows = computed(() => this.filteredSpaces().slice(0, PREVIEW_LIMIT));
    protected readonly hostsHasMore = computed(() => this.hosts().length > PREVIEW_LIMIT);
    protected readonly spacesHasMore = computed(() => this.spaces().length > PREVIEW_LIMIT);
    protected readonly filteredHostsModal = computed(() => {
        const q = this.modalSearch().trim();
        const list = this.hosts();
        if (!q)
            return list;
        return list.filter((h) => AdminRequestsPageComponent.matchesHostSearch(h, q));
    });
    protected readonly filteredSpacesModal = computed(() => {
        const q = this.modalSearch().trim();
        const list = this.spaces();
        if (!q)
            return list;
        return list.filter((s) => AdminRequestsPageComponent.matchesSpaceSearch(s, q));
    });
    protected readonly hasHostSearchNoResults = computed(() => this.hosts().length > 0 && this.filteredHosts().length === 0 && this.hostSectionSearch().trim().length > 0);
    protected readonly hasSpaceSearchNoResults = computed(() => this.spaces().length > 0 && this.filteredSpaces().length === 0 && this.spaceSectionSearch().trim().length > 0);
    protected readonly modalHasNoMatches = computed(() => {
        if (!this.modalSearch().trim())
            return false;
        return this.filteredHostsModal().length === 0 && this.filteredSpacesModal().length === 0;
    });
    public ngOnInit(): void {
        this.loadAll();
    }
    protected displayName(host: HostRow): string {
        const parts = [host.name?.trim(), host.surname?.trim()].filter(Boolean);
        return parts.length ? parts.join(' ') : '—';
    }
    protected toggleDescription(userId: number): void {
        this.expandedDescriptionUserId.update((cur) => (cur === userId ? null : userId));
    }
    protected descriptionExpanded(userId: number): boolean {
        return this.expandedDescriptionUserId() === userId;
    }
    protected toggleModalHostDescription(userId: number): void {
        this.expandedModalHostUserId.update((cur) => (cur === userId ? null : userId));
    }
    protected modalHostDescriptionExpanded(userId: number): boolean {
        return this.expandedModalHostUserId() === userId;
    }
    protected toggleModalSpaceDescription(spaceId: number): void {
        this.expandedModalSpaceId.update((cur) => (cur === spaceId ? null : spaceId));
    }
    protected modalSpaceDescriptionExpanded(spaceId: number): boolean {
        return this.expandedModalSpaceId() === spaceId;
    }
    protected togglePreviewSpaceDescription(spaceId: number): void {
        this.expandedPreviewSpaceId.update((cur) => (cur === spaceId ? null : spaceId));
    }
    protected previewSpaceDescriptionExpanded(spaceId: number): boolean {
        return this.expandedPreviewSpaceId() === spaceId;
    }
    protected queueBadgeLabel(): string {
        return 'In revisione';
    }
    protected formatRequestSubmittedHint(host: HostRow): string {
        const v = host.registeredAt;
        if (v == null || v === '')
            return '';
        const d = new Date(v);
        if (Number.isNaN(d.getTime()))
            return '';
        return `Richiesta inviata: ${d.toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' })}`;
    }
    protected openRequestsModal(): void {
        this.modalSearch.set('');
        this.expandedModalHostUserId.set(null);
        this.expandedModalSpaceId.set(null);
        this.requestsModalOpen.set(true);
    }
    protected closeRequestsModal(): void {
        this.requestsModalOpen.set(false);
        this.modalSearch.set('');
        this.expandedModalHostUserId.set(null);
        this.expandedModalSpaceId.set(null);
    }
    protected loadAll(): void {
        this.errorMsg = '';
        forkJoin({
            hosts: this.adminService.getHosts(),
            spaces: this.adminService.getPendingSpaces()
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ hosts, spaces }) => {
                const sortedHosts = sortPendingHostsNewestFirst(hosts as Array<Record<string, unknown>>);
                const mappedHosts: HostRow[] = sortedHosts
                    .map((raw) => ({
                    userID: Number(raw['userID'] ?? raw['id'] ?? 0),
                    name: raw['name'] != null ? String(raw['name']) : '',
                    surname: raw['surname'] != null ? String(raw['surname']) : '',
                    email: raw['email'] != null ? String(raw['email']) : '',
                    status: raw['status'] != null ? String(raw['status']) : '',
                    description: raw['description'] != null ? String(raw['description']) : '',
                    nameStructure: raw['nameStructure'] != null ? String(raw['nameStructure']) : '',
                    registeredAt: raw['registeredAt'] != null ? String(raw['registeredAt']) : undefined
                }))
                    .filter((h) => Number.isFinite(h.userID) && h.userID > 0);
                const sortedSpaces = sortPendingSpacesNewestFirst(spaces as Array<Record<string, unknown>>);
                const mappedSpaces: PendingSpaceRow[] = sortedSpaces
                    .map((raw) => ({
                    spaceID: Number(raw['spaceID'] ?? raw['id'] ?? 0),
                    name: raw['name'] != null ? String(raw['name']) : '',
                    city: raw['city'] != null ? String(raw['city']) : '',
                    address: raw['address'] != null ? String(raw['address']) : '',
                    description: raw['description'] != null ? String(raw['description']) : '',
                    officeCode: raw['officeCode'] != null ? String(raw['officeCode']) : '',
                    hostName: raw['hostName'] != null ? String(raw['hostName']) : undefined
                }))
                    .filter((s) => Number.isFinite(s.spaceID) && s.spaceID > 0);
                this.hosts.set(mappedHosts);
                this.spaces.set(mappedSpaces);
            },
            error: (err: Error) => {
                this.hosts.set([]);
                this.spaces.set([]);
                this.errorMsg = err.message;
            }
        });
    }
    protected processHost(host: HostRow, approved: boolean): void {
        const email = host.email ?? '';
        const display = this.displayName(host).trim() || email;
        const confirmOpts = approved
            ? {
                title: 'Approva host',
                message: `Approvare la richiesta di iscrizione come host da parte di ${display} (${email})? Verrà attivato il ruolo host e potrà gestire gli spazi sulla piattaforma.`,
                confirmLabel: 'Approva',
                variant: 'success' as const
            }
            : {
                title: 'Conferma rifiuto richiesta host',
                message: `Stai per rifiutare la richiesta di iscrizione come host da parte di ${display} (${email}). Non è ancora un host approvato: il ruolo host non verrà attivato e non potrà accedere alle funzioni riservate agli host. Le credenziali usate per la richiesta non potranno più essere utilizzate per accedere. Procedere?`,
                confirmLabel: 'Sì, rifiuta',
                variant: 'danger' as const
            };
        this.confirmService
            .confirm({
            ...confirmOpts,
            cancelLabel: 'Annulla'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg = '';
            return this.adminService.approveHost(host.userID, approved);
        }))
            .subscribe({
            next: () => {
                this.notifications.requestRefresh();
                this.expandedDescriptionUserId.set(null);
                this.expandedModalHostUserId.set(null);
                this.loadAll();
            },
            error: (err: Error) => (this.errorMsg = err.message)
        });
    }
    protected processSpace(space: PendingSpaceRow, approved: boolean): void {
        const name = space.name ?? '';
        const confirmOpts = approved
            ? {
                title: 'Approva spazio',
                message: `Approvare lo spazio «${name}»?`,
                confirmLabel: 'Approva',
                variant: 'success' as const
            }
            : {
                title: 'Rifiuta spazio',
                message: `Rifiutare lo spazio «${name}»?`,
                confirmLabel: 'Rifiuta',
                variant: 'danger' as const
            };
        this.confirmService
            .confirm({
            ...confirmOpts,
            cancelLabel: 'Annulla'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg = '';
            return this.adminService.approveSpace(space.spaceID, approved ? 'APPROVE' : 'REJECT');
        }))
            .subscribe({
            next: () => {
                this.notifications.requestRefresh();
                this.expandedModalSpaceId.set(null);
                this.loadAll();
            },
            error: (err: Error) => (this.errorMsg = err.message)
        });
    }
    private static matchesHostSearch(host: HostRow, rawQuery: string): boolean {
        return haystackMatchesTokenSearch([host.name ?? '', host.surname ?? '', host.email ?? ''].join(' '), rawQuery);
    }
    private static matchesSpaceSearch(space: PendingSpaceRow, rawQuery: string): boolean {
        return haystackMatchesTokenSearch([
            space.name,
            space.city,
            space.address,
            space.officeCode,
            space.hostName ?? '',
            space.description,
            String(space.spaceID)
        ].join(' '), rawQuery);
    }
}
