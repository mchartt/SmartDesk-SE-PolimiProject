import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RoleRouteTitleBase } from '../../../../shared/base/role-route-title.base';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { AdminService } from '../../../../core/services/admin.service';
import { sortPendingHostsNewestFirst } from '../../../../core/utils/pending-hosts.util';
import { sortPendingSpacesNewestFirst } from '../../../../core/utils/pending-spaces.util';
import { mergePendingRequestsPreview, PendingRequestPreviewRow } from '../../../../core/utils/pending-requests.util';
const PENDING_REQUESTS_PREVIEW_LIMIT = 3;
@Component({
    standalone: true,
    imports: [CommonModule, RouterLink, SdIconComponent, MdbRippleModule],
    templateUrl: './admin-dashboard-page.component.html',
    styleUrl: './admin-dashboard-page.component.scss'
})
export class AdminDashboardPageComponent extends RoleRouteTitleBase implements OnInit {
    protected readonly pendingRequestsPreviewLimit = PENDING_REQUESTS_PREVIEW_LIMIT;
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    protected defaultRouteTitle(): string {
        return 'Panoramica amministratore';
    }
    protected defaultDashboardTitle(): string {
        return 'Panoramica amministratore';
    }
    protected pendingHostsTotal = signal(0);
    protected pendingSpacesTotal = signal(0);
    protected pendingRequestsPreview = signal<PendingRequestPreviewRow[]>([]);
    protected stats = signal({
        totalUsers: 0,
        activeSpaces: 0,
        pendingSpaces: 0
    });
    protected pendingRequestsTotal(): number {
        return this.pendingHostsTotal() + this.pendingSpacesTotal();
    }
    public ngOnInit(): void {
        this.loadDashboard();
    }
    protected loadDashboard(): void {
        forkJoin({
            hosts: this.adminService.getHosts(),
            pendingSpacesList: this.adminService.getPendingSpaces(),
            users: this.adminService.getUsers(),
            approvedSpacesList: this.adminService.getApprovedSpacesEnriched()
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ hosts, pendingSpacesList, users, approvedSpacesList }) => {
                const hostRows = sortPendingHostsNewestFirst(hosts as Array<Record<string, unknown>>);
                const spaceRows = sortPendingSpacesNewestFirst(pendingSpacesList as Array<Record<string, unknown>>);
                this.pendingHostsTotal.set(hostRows.length);
                this.pendingSpacesTotal.set(spaceRows.length);
                this.pendingRequestsPreview.set(mergePendingRequestsPreview(hostRows, spaceRows, PENDING_REQUESTS_PREVIEW_LIMIT));
                this.stats.set({
                    totalUsers: users.length,
                    activeSpaces: approvedSpacesList.length,
                    pendingSpaces: spaceRows.length
                });
            },
            error: () => {
                this.pendingHostsTotal.set(0);
                this.pendingSpacesTotal.set(0);
                this.pendingRequestsPreview.set([]);
            }
        });
    }
    protected requestKindLabel(kind: PendingRequestPreviewRow['kind']): string {
        return kind === 'host' ? 'Host' : 'Spazio';
    }
}
