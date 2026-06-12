import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { HostService } from '../../../../core/services/host.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Space } from '../../../../core/models';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import {
    buildAllVerifyingTickets,
    HostSpaceTicketsBundle,
    sortBundlesBySpaceName,
    unresolvedTicketCount,
    verifyingTicketCount
} from './host-tickets.util';
import { HostTicketsModalStore } from './host-tickets-modal.store';
import { OfficeTicketsModalComponent } from './components/office-tickets-modal/office-tickets-modal.component';
import { ApprovalQueueModalComponent } from './components/approval-queue-modal/approval-queue-modal.component';
import { ApprovalDetailModalComponent } from './components/approval-detail-modal/approval-detail-modal.component';
import { ResolvedHistoryModalComponent } from './components/resolved-history-modal/resolved-history-modal.component';
import { AssignTechnicianModalComponent } from './components/assign-technician-modal/assign-technician-modal.component';

export type {
    HostPendingApprovalItem,
    HostSpaceTicketsBundle,
    HostTicketsModalStep,
    ModalRoomRow
} from './host-tickets.util';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        MdbRippleModule,
        EmptyStateComponent,
        SdIconComponent,
        OfficeTicketsModalComponent,
        ApprovalQueueModalComponent,
        ApprovalDetailModalComponent,
        ResolvedHistoryModalComponent,
        AssignTechnicianModalComponent
    ],
    providers: [HostTicketsModalStore],
    templateUrl: './host-tickets-page.component.html',
    styleUrl: './host-tickets-page.component.scss'
})
export class HostTicketsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    private readonly authState = inject(AuthStateService);
    private readonly notifications = inject(NotificationService);
    private readonly modal = inject(HostTicketsModalStore);

    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Segnalazioni');
    protected readonly bundles = signal<HostSpaceTicketsBundle[]>([]);
    protected readonly allVerifyingTickets = computed(() => buildAllVerifyingTickets(this.bundles()));
    protected readonly sortedBundles = computed(() => sortBundlesBySpaceName(this.bundles()));
    protected readonly loading = signal(true);
    protected errorMsg = '';

    protected readonly actionSuccessToast = this.modal.actionSuccessToast;
    protected readonly resolvedHistoryFetched = this.modal.resolvedHistoryFetched;
    protected readonly resolvedTickets = this.modal.resolvedTickets;
    protected readonly resolvedTicketsLoading = this.modal.resolvedTicketsLoading;
    protected readonly resolvedHistoryClearing = this.modal.resolvedHistoryClearing;

    public ngOnInit(): void {
        this.modal.bindHost({
            bundles: this.bundles,
            allVerifyingTickets: () => this.allVerifyingTickets(),
            onPageError: (message) => {
                this.errorMsg = message;
            },
            hostChatAuthorLabel: () => this.hostChatAuthorLabel(),
            reloadBundles: () => this.reload()
        });
        this.notifications.requestRefresh();
        this.reload();
    }

    protected reload(): void {
        this.errorMsg = '';
        this.loading.set(true);
        this.hostService
            .getSpaces()
            .pipe(
                switchMap((spaces) => {
                    if (!spaces.length) {
                        return of([] as HostSpaceTicketsBundle[]);
                    }
                    return forkJoin(spaces.map((space) => this.loadBundle(space)));
                }),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (rows) => {
                    this.bundles.set(rows);
                    this.loading.set(false);
                    this.modal.onBundlesLoaded();
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                    this.bundles.set([]);
                    this.loading.set(false);
                    this.modal.onBundlesLoadFailed();
                }
            });
    }

    private loadBundle(space: Space): Observable<HostSpaceTicketsBundle> {
        return this.hostService.getDesks(space.spaceID).pipe(
            switchMap((desks) => {
                if (!desks.length) {
                    return of({ space, desks: [], ticketsByDeskId: new Map<number, Record<string, unknown>[]>() });
                }
                return forkJoin(
                    desks.map((d) =>
                        this.hostService.getDeskTickets(d.id).pipe(catchError(() => of([] as Record<string, unknown>[])))
                    )
                ).pipe(
                    map((rowsArr) => {
                        const ticketsByDeskId = new Map<number, Record<string, unknown>[]>();
                        desks.forEach((d, i) => ticketsByDeskId.set(d.id, rowsArr[i] ?? []));
                        return { space, desks, ticketsByDeskId };
                    })
                );
            }),
            catchError(() => of({ space, desks: [], ticketsByDeskId: new Map<number, Record<string, unknown>[]>() }))
        );
    }

    protected unresolvedTicketCount(bundle: HostSpaceTicketsBundle): number {
        return unresolvedTicketCount(bundle);
    }

    protected verifyingTicketCount(bundle: HostSpaceTicketsBundle): number {
        return verifyingTicketCount(bundle);
    }

    protected trackBundleSpaceId(_i: number, b: HostSpaceTicketsBundle): number {
        return b.space.spaceID;
    }

    protected openOfficeModal(bundle: HostSpaceTicketsBundle): void {
        this.modal.openOfficeModal(bundle);
    }

    protected openApprovalQueueModal(): void {
        this.modal.openApprovalQueueModal();
    }

    protected openResolvedHistoryModal(): void {
        this.modal.openResolvedHistoryModal();
    }

    protected requestClearResolvedHistory(): void {
        this.modal.requestClearResolvedHistory();
    }

    private hostChatAuthorLabel(): string {
        const user = this.authState.currentUserSnapshot();
        if (!user) {
            return 'Host';
        }
        const person = user.fullName?.trim() || `${user.name} ${user.surname}`.trim();
        return person ? `${person} · Host` : 'Host';
    }
}
