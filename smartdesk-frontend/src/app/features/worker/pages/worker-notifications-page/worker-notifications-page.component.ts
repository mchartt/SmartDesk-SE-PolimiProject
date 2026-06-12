import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { MdbTabsModule, MdbTabChange } from 'mdb-angular-ui-kit/tabs';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { NotificationModel, Space } from '../../../../core/models';
import { AdminService } from '../../../../core/services/admin.service';
import { HostService } from '../../../../core/services/host.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { SdIconComponent, SdIconName } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { formatShortDateTime } from '../../../../core/utils/date.util';
import { haystackMatchesTokenSearch } from '../../../../core/utils/search.util';
@Component({
    standalone: true,
    imports: [NgClass, SdIconComponent, FormsModule, MdbTabsModule, MdbRippleModule],
    templateUrl: './worker-notifications-page.component.html',
    styleUrl: './worker-notifications-page.component.scss'
})
export class WorkerNotificationsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly notificationService = inject(NotificationService);
    private readonly adminService = inject(AdminService);
    private readonly hostService = inject(HostService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Notifiche');
    protected readonly notifications = signal<NotificationModel[]>([]);
    protected readonly errorMsg = signal('');
    protected readonly bulkSuccess = signal('');
    protected readonly loading = signal(false);
    protected readonly activeTab = signal<'all' | 'unread' | 'read'>('all');
    protected readonly searchQuery = signal('');
    protected readonly markingAllRead = signal(false);
    protected readonly clearingHistory = signal(false);
    protected readonly pendingHostsQueue = signal(0);
    protected readonly pendingSpacesQueue = signal(0);
    protected readonly pendingOwnSpaces = signal(0);
    protected readonly markingReadId = signal<number | null>(null);
    protected readonly openingIconIds = signal<ReadonlySet<number>>(new Set());
    private static readonly ENVELOPE_OPEN_MS = 480;
    protected readonly messagesTotal = computed(() => this.notifications().length);
    protected readonly messagesUnread = computed(() => this.notifications().filter((n) => !n.read).length);
    protected readonly messagesRead = computed(() => this.notifications().filter((n) => n.read).length);
    protected readonly approvalQueueTotal = computed(() => this.pendingHostsQueue() + this.pendingSpacesQueue());
    protected readonly filteredNotifications = computed(() => {
        const rows = this.notifications();
        let list: NotificationModel[];
        switch (this.activeTab()) {
            case 'unread':
                list = rows.filter((n) => !n.read);
                break;
            case 'read':
                list = rows.filter((n) => n.read);
                break;
            default:
                list = rows;
        }
        if (this.routeRole() === 'HOST') {
            list = list.filter((n) => n.kind !== 'TICKET_NOTE_UPDATED');
        }
        list = WorkerNotificationsPageComponent.sortNotifications(list, this.activeTab() === 'all');
        const raw = this.searchQuery().trim();
        if (!raw) {
            return list;
        }
        return list.filter((n) => haystackMatchesTokenSearch([
            n.title,
            this.notificationDisplayTitle(n),
            this.hostTicketProblemLine(n),
            this.actorDisplayName(n),
            n.kind,
            n.actorName,
            n.actorSurname,
            n.actorEmail,
            this.workerActivityKindLabel(n.kind),
            n.createdAt ?? ''
        ]
            .filter(Boolean)
            .join(' '), raw));
    });
    protected readonly filteredEmptyMessage = computed(() => {
        if (this.searchQuery().trim()) {
            return 'Nessuna notifica corrisponde ai termini cercati.';
        }
        switch (this.activeTab()) {
            case 'unread':
                return 'Nessuna notifica non letta.';
            case 'read':
                return 'Nessuna notifica letta in elenco.';
            default:
                return 'Nessun risultato.';
        }
    });
    public ngOnInit(): void {
        this.load();
        this.bindRealtimeNotifications();
    }
    private bindRealtimeNotifications(): void {
        this.notificationService.notificationCreated$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((incoming) => {
            if (this.shouldHideIncomingForRole(incoming)) {
                return;
            }
            this.notifications.update((rows) => {
                if (rows.some((n) => n.id === incoming.id)) {
                    return rows;
                }
                return [incoming, ...rows];
            });
        });
        this.notificationService.notificationUpdated$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event) => {
            this.notifications.update((rows) => rows.map((n) => (n.id === event.id ? n.withRead(event.read) : n)));
        });
        this.notificationService.allNotificationsMarkedRead$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
            const unreadIds = this.notifications()
                .filter((n) => !n.read)
                .map((n) => n.id);
            if (!unreadIds.length) {
                return;
            }
            this.notifications.update((rows) => rows.map((n) => (n.read ? n : n.withRead(true))));
            this.triggerEnvelopeOpenAnimation(unreadIds);
        });
    }
    private shouldHideIncomingForRole(n: NotificationModel): boolean {
        return this.routeRole() === 'HOST' && n.kind === 'TICKET_NOTE_UPDATED';
    }
    protected setActiveTab(tab: 'all' | 'unread' | 'read'): void {
        this.activeTab.set(tab);
    }
    protected onMdbTabChange(change: MdbTabChange): void {
        const tabs: Array<'all' | 'unread' | 'read'> = ['all', 'unread', 'read'];
        this.setActiveTab(tabs[change.index] ?? 'all');
    }
    protected routeRole(): string {
        return (this.route.snapshot.data['role'] as string) ?? '';
    }
    protected iconAnimateOpen(n: NotificationModel): boolean {
        return n.read && this.openingIconIds().has(n.id);
    }
    private triggerEnvelopeOpenAnimation(ids: number[]): void {
        if (!ids.length) {
            return;
        }
        const batch = new Set(ids);
        this.openingIconIds.update((current) => new Set([...current, ...batch]));
        window.setTimeout(() => {
            this.openingIconIds.update((current) => {
                const next = new Set(current);
                for (const id of batch) {
                    next.delete(id);
                }
                return next;
            });
        }, WorkerNotificationsPageComponent.ENVELOPE_OPEN_MS);
    }
    protected notificationIcon(n: NotificationModel): SdIconName {
        if (n.read) {
            return 'envelope-open';
        }
        if (n.kind === 'HOST_TICKET_OPENED' ||
            n.kind === 'HOST_TICKET_VERIFYING' ||
            n.kind === 'TICKET_NOTE_UPDATED') {
            return 'ticket';
        }
        if (n.kind === 'HOST_REVIEW_LEFT') {
            return 'star';
        }
        if (n.kind === 'BOOKING_CANCELLED') {
            return 'calendar-check';
        }
        const t = n.title.toLowerCase();
        if (t.includes('spazio')) {
            return 'layers';
        }
        if (t.includes('host')) {
            return 'host-venue';
        }
        if (t.includes('ticket') || t.includes('segnal')) {
            return 'ticket';
        }
        if (t.includes('prenot')) {
            return 'calendar-check';
        }
        return 'envelope';
    }
    protected hasWorkerActorDetails(n: NotificationModel): boolean {
        return (n.kind === 'HOST_TICKET_OPENED' ||
            n.kind === 'HOST_REVIEW_LEFT' ||
            n.kind === 'HOST_REPLY_ON_REVIEW' ||
            n.kind === 'TICKET_NOTE_UPDATED');
    }
    protected workerActivityKindLabel(kind: string | null): string {
        switch (kind) {
            case 'HOST_TICKET_OPENED':
                return 'Ha aperto una segnalazione';
            case 'HOST_TICKET_VERIFYING':
                return 'Riparazione da approvare';
            case 'HOST_REVIEW_LEFT':
                return 'Ha lasciato una recensione';
            case 'HOST_REPLY_ON_REVIEW':
                return 'Ha risposto alla tua recensione';
            case 'BOOKING_CANCELLED':
                return 'Prenotazione annullata';
            case 'TICKET_NOTE_UPDATED':
                return 'Nuovo messaggio sul ticket';
            default:
                return '';
        }
    }
    protected actorFirstName(n: NotificationModel): string {
        return n.actorName?.trim() || '—';
    }
    protected actorLastName(n: NotificationModel): string {
        return n.actorSurname?.trim() || '—';
    }
    protected formatNotificationWhen(iso: string | null): string {
        return formatShortDateTime(iso);
    }
    protected readonly stars = [1, 2, 3, 4, 5] as const;
    protected actorDisplayName(n: NotificationModel): string {
        const parts = [n.actorName?.trim(), n.actorSurname?.trim()].filter((p): p is string => !!p);
        return parts.length ? parts.join(' ') : '—';
    }
    protected hostTicketProblemLine(n: NotificationModel): string {
        let text = n.title.trim();
        text = text.replace(/^Nuova segnalazione\s+/i, '').replace(/\.\s*$/, '').trim();
        text = text.replace(/\s*(?:—|--|\.\.\.)\s*/g, ' · ').replace(/\s{2,}/g, ' ').trim();
        return text || '—';
    }
    protected usesCompactActorLayout(n: NotificationModel): boolean {
        return this.hasWorkerActorDetails(n);
    }
    protected notificationDisplayTitle(n: NotificationModel): string {
        if (n.kind === 'HOST_TICKET_OPENED') {
            const who = this.actorDisplayName(n);
            return who === '—' ? 'Nuova segnalazione aperta' : `Nuova segnalazione aperta da ${who}`;
        }
        if (n.kind === 'HOST_REVIEW_LEFT') {
            const who = this.actorDisplayName(n);
            return who === '—' ? 'Nuova recensione' : `Nuova recensione da ${who}`;
        }
        if (n.kind === 'HOST_REPLY_ON_REVIEW') {
            const who = this.actorDisplayName(n);
            return who === '—' ? 'Nuova risposta alla recensione' : `Nuova risposta da ${who}`;
        }
        return n.title;
    }
    protected notificationActivityDetail(n: NotificationModel): string {
        if (n.kind === 'HOST_REVIEW_LEFT') {
            return this.notificationDisplayTitleLegacyReview(n);
        }
        return n.title.trim() || '—';
    }
    private notificationDisplayTitleLegacyReview(n: NotificationModel): string {
        return n.title
            .replace(/\s*\([1-5]\/5\)\s*/g, ' ')
            .replace(/\s[\u2605\u2606]{5}\s/g, ' ')
            .replace(/\s{2,}/g, ' ')
            .trim();
    }
    protected load(): void {
        this.errorMsg.set('');
        this.bulkSuccess.set('');
        this.loading.set(true);
        const role = this.routeRole();
        const endLoad = () => this.loading.set(false);
        if (role === 'SYS_ADMIN') {
            forkJoin({
                notifications: this.notificationService.getNotifications(),
                hosts: this.adminService.getHosts(),
                spaces: this.adminService.getPendingSpaces()
            })
                .pipe(finalize(endLoad), takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: ({ notifications, hosts, spaces }) => {
                    this.notifications.set(notifications);
                    this.pendingHostsQueue.set(hosts.length);
                    this.pendingSpacesQueue.set(spaces.length);
                },
                error: (err: Error) => {
                    this.notifications.set([]);
                    this.pendingHostsQueue.set(0);
                    this.pendingSpacesQueue.set(0);
                    this.errorMsg.set(err.message);
                }
            });
            return;
        }
        if (role === 'HOST') {
            forkJoin({
                notifications: this.notificationService.getNotifications(),
                spaces: this.hostService.getSpaces().pipe(catchError(() => of([] as Space[])))
            })
                .pipe(finalize(endLoad), takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: ({ notifications, spaces }) => {
                    this.notifications.set(notifications);
                    this.pendingOwnSpaces.set(spaces.filter((s) => !s.approved).length);
                },
                error: (err: Error) => {
                    this.notifications.set([]);
                    this.pendingOwnSpaces.set(0);
                    this.errorMsg.set(err.message);
                }
            });
            return;
        }
        this.notificationService
            .getNotifications()
            .pipe(finalize(endLoad), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                this.notifications.set(rows);
                this.pendingOwnSpaces.set(0);
            },
            error: (err: Error) => {
                this.notifications.set([]);
                this.errorMsg.set(err.message);
            }
        });
    }
    protected markAsRead(id: number): void {
        this.errorMsg.set('');
        this.bulkSuccess.set('');
        this.markingReadId.set(id);
        this.notificationService
            .markAsRead(id)
            .pipe(finalize(() => this.markingReadId.set(null)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.notifications.update((rows) => rows.map((n) => (n.id === id ? n.withRead(true) : n)));
                this.triggerEnvelopeOpenAnimation([id]);
                this.notificationService.requestRefresh();
            },
            error: (err: Error) => this.errorMsg.set(err.message)
        });
    }
    protected markAllAsRead(): void {
        if (!this.messagesUnread() || this.markingAllRead()) {
            return;
        }
        this.bulkSuccess.set('');
        this.errorMsg.set('');
        this.markingAllRead.set(true);
        this.notificationService
            .markAllAsRead()
            .pipe(finalize(() => this.markingAllRead.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                const unreadIds = this.notifications()
                    .filter((n) => !n.read)
                    .map((n) => n.id);
                this.notifications.update((rows) => rows.map((n) => (n.read ? n : n.withRead(true))));
                this.triggerEnvelopeOpenAnimation(unreadIds);
                this.notificationService.resetLocalCount();
                this.bulkSuccess.set('Tutte le notifiche sono state segnate come lette (Leggi tutte).');
            },
            error: (err: Error) => this.errorMsg.set(err.message)
        });
    }
    protected clearHistory(): void {
        if (this.clearingHistory())
            return;
        this.errorMsg.set('');
        this.bulkSuccess.set('');
        this.clearingHistory.set(true);
        this.notificationService
            .clearHistory()
            .pipe(finalize(() => this.clearingHistory.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.notifications.update((rows) => rows.filter((n) => !n.read));
                this.bulkSuccess.set('Storico notifiche lette pulito.');
            },
            error: (err: Error) => this.errorMsg.set(err.message)
        });
    }
    private static sortNotifications(list: NotificationModel[], groupByRead: boolean): NotificationModel[] {
        return [...list].sort((a, b) => {
            if (groupByRead && a.read !== b.read) {
                return a.read ? 1 : -1;
            }
            const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return tb - ta;
        });
    }
}
