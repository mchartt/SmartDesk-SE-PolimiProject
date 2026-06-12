import { ChangeDetectorRef, Component, computed, DestroyRef, inject, signal, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, fromEvent, interval, merge } from 'rxjs';
import { debounceTime, exhaustMap, filter, finalize, take } from 'rxjs/operators';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbTabsModule, MdbTabChange } from 'mdb-angular-ui-kit/tabs';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { TicketStatusBadgeComponent } from '../../../../shared/components/ticket-status-badge/ticket-status-badge.component';
import { TicketStatusProgressComponent } from '../../../../shared/components/ticket-status-progress/ticket-status-progress.component';
import { TicketChatPanelComponent } from '../../../../shared/components/ticket-chat-panel/ticket-chat-panel.component';
import { ticketStatusHint } from '../../../../core/utils/ticket-status-display.util';
import { Booking, TicketResponse } from '../../../../core/models';
import { TICKET_DESCRIPTION_MAX_LENGTH, TICKET_TITLE_MAX_LENGTH } from '../../../../core/constants/ticket-form.constants';
import { BookingService } from '../../../../core/services/booking.service';
import { TicketService } from '../../../../core/services/ticket.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ToastService } from '../../../../core/services/toast.service';
import { buildTicketChatMessages, countUnseenIncomingChatMessages, incomingChatMessageSignature, loadChatSeenSignaturesFromStorage, ticketNoteSignature, workerIncomingChatMessages, type TicketChatMessage } from '../../../../core/utils/ticket-chat.util';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { bookingPublicRef } from '../../../../core/utils/booking.util';
import { formatShortDateTime, localCalendarDateIso, localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
import { technicianTicketSeverityLabel } from '../../../../core/utils/technician-ticket-list.util';
@Component({
    standalone: true,
    imports: [
        CommonModule,
        TicketStatusBadgeComponent,
        TicketStatusProgressComponent,
        TicketChatPanelComponent,
        FormsModule,
        MdbFormsModule,
        NgClass,
        RouterLink,
        SdIconComponent,
        SdModalHeaderComponent,
        MdbTabsModule,
        MdbRippleModule,
        MdbModalModule,
    ],
    templateUrl: './worker-tickets-page.component.html',
    styleUrl: './worker-tickets-page.component.scss'
})
export class WorkerTicketsPageComponent implements OnInit {
    @ViewChild('openTicketModal')
    private readonly openTicketModal!: TemplateRef<unknown>;
    @ViewChild('ticketDetailModal')
    private readonly ticketDetailModal!: TemplateRef<unknown>;
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly ticketService = inject(TicketService);
    private readonly bookingService = inject(BookingService);
    private readonly notificationService = inject(NotificationService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly toast = inject(ToastService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly modalService = inject(MdbModalService);
    private openTicketModalRef: MdbModalRef<unknown> | null = null;
    private ticketDetailModalRef: MdbModalRef<unknown> | null = null;
    private static readonly CHAT_SEEN_STORAGE_KEY = 'sd-worker-ticket-chat-seen';
    private chatSeenSignatureByTicketId: Record<number, string> = {};
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Le mie segnalazioni');
    protected tickets = signal<TicketResponse[]>([]);
    protected todayBookings = signal<Booking[]>([]);
    protected errorMsg = '';
    protected readonly loading = signal(false);
    protected draftTitle = '';
    protected draftDescription = '';
    protected readonly openingBooking = signal<Booking | null>(null);
    protected ticketModalError = '';
    protected readonly detailTicket = signal<TicketResponse | null>(null);
    protected detailCommentDraft = '';
    protected detailCommentError = '';
    protected readonly detailCommentSending = signal(false);
    private readonly ticketNoteSnapshot = signal<Map<number, string>>(new Map());
    protected readonly activeTab = signal<'ALL' | 'IN_PROGRESS' | 'RESOLVED'>('ALL');
    protected readonly maxResolvedAgeDays = 3;
    protected readonly ticketCounts = computed(() => this.tickets().reduce((acc, t) => {
        const s = (t.status || '').toUpperCase();
        if (s === 'OPEN')
            acc.open++;
        else if (s === 'IN_PROGRESS')
            acc.inProgress++;
        else if (s === 'RESOLVED')
            acc.resolved++;
        return acc;
    }, { open: 0, inProgress: 0, resolved: 0 }));
    protected readonly openCount = computed(() => this.ticketCounts().open);
    protected readonly inProgressCount = computed(() => this.ticketCounts().inProgress);
    protected readonly resolvedCount = computed(() => this.ticketCounts().resolved);
    protected readonly filteredTickets = computed(() => {
        const tab = this.activeTab();
        if (tab === 'ALL') {
            return this.tickets();
        }
        if (tab === 'IN_PROGRESS') {
            return this.tickets().filter((t) => (t.status || '').toUpperCase() !== 'RESOLVED');
        }
        return this.tickets().filter((t) => (t.status || '').toUpperCase() === 'RESOLVED');
    });
    protected readonly bookingPublicRef = bookingPublicRef;
    public ngOnInit(): void {
        this.chatSeenSignatureByTicketId = this.loadChatSeenFromStorage();
        this.load();
        this.startTicketNoteWatcher();
    }
    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        forkJoin({
            tickets: this.ticketService.getMyTickets(),
            bookings: this.bookingService.getMyBookings()
        })
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ tickets, bookings }) => {
                const filtered = this.filterExpiredResolved(tickets, this.maxResolvedAgeDays);
                this.tickets.set(filtered);
                this.seedNoteSnapshot(filtered);
                this.todayBookings.set(this.filterTodayBookings(bookings));
            },
            error: (err: Error) => {
                this.tickets.set([]);
                this.todayBookings.set([]);
                this.errorMsg = err.message;
            }
        });
    }
    protected setTab(tab: 'ALL' | 'IN_PROGRESS' | 'RESOLVED'): void {
        this.activeTab.set(tab);
    }
    protected onMdbTabChange(change: MdbTabChange): void {
        const tabs: Array<'ALL' | 'IN_PROGRESS' | 'RESOLVED'> = ['ALL', 'IN_PROGRESS', 'RESOLVED'];
        this.setTab(tabs[change.index] ?? 'ALL');
    }
    private filterExpiredResolved(tickets: TicketResponse[], maxAgeDays: number): TicketResponse[] {
        const cutoff = Date.now() - maxAgeDays * 24 * 60 * 60 * 1000;
        return tickets.filter((t) => {
            if ((t.status || '').toUpperCase() !== 'RESOLVED')
                return true;
            if (!t.resolvedAt)
                return true;
            const ts = new Date(t.resolvedAt).getTime();
            return Number.isFinite(ts) ? ts > cutoff : true;
        });
    }
    private filterTodayBookings(rows: Booking[]): Booking[] {
        const todayIso = localCalendarDateIso();
        return rows.filter((b) => {
            if ((b.status || '').toUpperCase() === 'CANCELLED') {
                return false;
            }
            if (b.bookedDay) {
                return b.bookedDay === todayIso;
            }
            const normalizedStart = (b.startTime || '').replace(/Z$/, '');
            const d = new Date(normalizedStart);
            if (Number.isNaN(d.getTime())) {
                return false;
            }
            return localCalendarDateIsoFromDate(d) === todayIso;
        });
    }
    protected startOpenTicket(bookingID: number): void {
        const booking = this.todayBookings().find((b) => b.bookingID === bookingID);
        if (!booking) {
            return;
        }
        this.ticketModalError = '';
        this.draftTitle = '';
        this.draftDescription = '';
        this.openingBooking.set(booking);
        if (this.openTicketModalRef) {
            return;
        }
        this.openTicketModalRef = this.modalService.open(this.openTicketModal, {
            modalClass: 'modal-dialog-centered modal-dialog-scrollable'
        });
        this.openTicketModalRef.onClose
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
            this.openTicketModalRef = null;
            this.resetOpenTicketForm();
        });
    }
    private startTicketNoteWatcher(): void {
        const visibleTab$ = fromEvent(document, 'visibilitychange').pipe(filter(() => !document.hidden), debounceTime(250));
        merge(interval(45000), visibleTab$)
            .pipe(filter(() => !document.hidden && !this.loading()), exhaustMap(() => this.ticketService.getMyTickets()), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                const filtered = this.filterExpiredResolved(rows, this.maxResolvedAgeDays);
                this.applyTicketsRefresh(filtered, true);
            }
        });
    }
    private seedNoteSnapshot(tickets: TicketResponse[]): void {
        const next = new Map<number, string>();
        tickets.forEach((t) => next.set(t.ticketID, ticketNoteSignature(t)));
        this.ticketNoteSnapshot.set(next);
    }
    private applyTicketsRefresh(tickets: TicketResponse[], notifyOnNoteChange: boolean): void {
        if (notifyOnNoteChange) {
            const prev = this.ticketNoteSnapshot();
            for (const t of tickets) {
                const sig = ticketNoteSignature(t);
                const old = prev.get(t.ticketID);
                if (old !== undefined && old !== sig && this.staffMessagesChanged(old, sig)) {
                    const unseen = this.ticketUnseenIncomingCount(t);
                    if (unseen > 0) {
                        this.toast.success(unseen === 1
                            ? `Nuovo messaggio su ${t.displayTicketHeading}.`
                            : `${unseen} nuovi messaggi su ${t.displayTicketHeading}.`);
                        this.notificationService.requestRefresh();
                    }
                }
            }
        }
        this.seedNoteSnapshot(tickets);
        this.tickets.set(tickets);
        const open = this.detailTicket();
        if (open) {
            const updated = tickets.find((t) => t.ticketID === open.ticketID);
            if (updated) {
                this.detailTicket.set(updated);
                this.markTicketChatSeen(updated);
            }
        }
        this.cdr.markForCheck();
    }
    private staffMessagesChanged(before: string, after: string): boolean {
        const split = (s: string) => {
            const parts = s.split('\u241f');
            return { tech: parts[1] ?? '', host: parts[3] ?? '' };
        };
        const a = split(after);
        const b = split(before);
        return a.tech !== b.tech || a.host !== b.host;
    }
    protected ticketDraftPlaceholder(booking: Booking): string {
        return `Descrivi problema su desk ${booking.deskID}: rumore, monitor, corrente, rete...`;
    }
    private resetOpenTicketForm(): void {
        this.openingBooking.set(null);
        this.ticketModalError = '';
        this.draftTitle = '';
        this.draftDescription = '';
    }
    protected cancelOpenTicket(): void {
        this.closeOpenTicketModal();
    }
    private closeOpenTicketModal(): void {
        this.openTicketModalRef?.close();
        this.openTicketModalRef = null;
    }
    protected workerFacingStatusShort(status: string): string {
        const u = (status || '').toUpperCase();
        if (u === 'RESOLVED' || u === 'CLOSED') {
            return 'Risoluzione confermata';
        }
        return 'In corso';
    }
    protected technicalStatusLine(status: string): string {
        return ticketStatusHint(status);
    }
    protected openTicketDetail(ticket: TicketResponse): void {
        this.detailCommentDraft = '';
        this.detailCommentError = '';
        this.detailTicket.set(ticket);
        this.markTicketChatSeen(ticket);
        if (this.ticketDetailModalRef) {
            return;
        }
        this.ticketDetailModalRef = this.modalService.open(this.ticketDetailModal, {
            modalClass: 'modal-dialog-centered modal-dialog-scrollable'
        });
        this.ticketDetailModalRef.onClose
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
            this.ticketDetailModalRef = null;
            this.resetTicketDetail();
        });
    }
    protected closeTicketDetail(): void {
        this.closeTicketDetailModal();
    }
    private resetTicketDetail(): void {
        this.detailTicket.set(null);
        this.detailCommentDraft = '';
        this.detailCommentError = '';
        this.detailCommentSending.set(false);
    }
    private closeTicketDetailModal(): void {
        this.ticketDetailModalRef?.close();
        this.ticketDetailModalRef = null;
    }
    protected canAddDetailComment(ticket: TicketResponse): boolean {
        return (ticket.status || '').toUpperCase() !== 'RESOLVED';
    }
    protected get detailCommentLength(): number {
        return this.detailCommentDraft.trim().length;
    }
    protected get canSubmitDetailComment(): boolean {
        const len = this.detailCommentLength;
        return len >= 1 && len <= TICKET_DESCRIPTION_MAX_LENGTH && !this.detailCommentSending();
    }
    protected submitDetailComment(): void {
        const ticket = this.detailTicket();
        if (!ticket || !this.canSubmitDetailComment) {
            return;
        }
        const body = this.detailCommentDraft.trim();
        this.detailCommentError = '';
        this.detailCommentSending.set(true);
        this.ticketService
            .addComment(ticket.ticketID, body)
            .pipe(finalize(() => this.detailCommentSending.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (updated) => {
                this.detailTicket.set(updated);
                this.tickets.update((rows) => rows.map((t) => (t.ticketID === updated.ticketID ? updated : t)));
                this.seedNoteSnapshot(this.tickets());
                this.detailCommentDraft = '';
                this.toast.success('Commento inviato.');
            },
            error: (err: Error) => {
                this.detailCommentError = err.message;
                this.toast.error(err.message || 'Impossibile inviare il commento.');
            }
        });
    }
    protected ticketChatMessages(ticket: TicketResponse): TicketChatMessage[] {
        return buildTicketChatMessages(ticket);
    }
    protected ticketDescriptionText(ticket: TicketResponse): string {
        return (ticket.description ?? '').trim();
    }
    protected ticketUnseenIncomingCount(ticket: TicketResponse): number {
        return countUnseenIncomingChatMessages(ticket, this.chatSeenSignatureByTicketId[ticket.ticketID]);
    }
    protected hasUnseenStaffMessages(ticket: TicketResponse): boolean {
        return this.ticketUnseenIncomingCount(ticket) > 0;
    }
    private markTicketChatSeen(ticket: TicketResponse): void {
        this.chatSeenSignatureByTicketId[ticket.ticketID] = incomingChatMessageSignature(workerIncomingChatMessages(ticket));
        this.persistChatSeenSignatures();
        this.cdr.markForCheck();
    }
    private loadChatSeenFromStorage(): Record<number, string> {
        return loadChatSeenSignaturesFromStorage(WorkerTicketsPageComponent.CHAT_SEEN_STORAGE_KEY);
    }
    private persistChatSeenSignatures(): void {
        try {
            sessionStorage.setItem(WorkerTicketsPageComponent.CHAT_SEEN_STORAGE_KEY, JSON.stringify(this.chatSeenSignatureByTicketId));
        }
        catch {
        }
    }
    protected formatTicketWhen(iso: string | null | undefined): string {
        return formatShortDateTime(iso);
    }
    protected severityUpper(severity: string | null): string {
        return (severity ?? '').toUpperCase();
    }
    protected severityLabel(severity: string | null | undefined): string {
        return technicianTicketSeverityLabel(severity);
    }
    protected canDeleteTicket(status: string): boolean {
        const u = (status || '').toUpperCase();
        return u !== 'IN_PROGRESS';
    }
    protected get descriptionMax(): number {
        return TICKET_DESCRIPTION_MAX_LENGTH;
    }
    protected get titleMax(): number {
        return TICKET_TITLE_MAX_LENGTH;
    }
    protected get titleTrimLength(): number {
        return this.draftTitle.trim().length;
    }
    protected get descriptionLength(): number {
        return this.draftDescription.trim().length;
    }
    protected get titleTooLong(): boolean {
        return this.titleTrimLength > TICKET_TITLE_MAX_LENGTH;
    }
    protected get titleFieldInvalid(): boolean {
        const len = this.titleTrimLength;
        return len === 0 || len > TICKET_TITLE_MAX_LENGTH;
    }
    protected get canSubmitTicket(): boolean {
        const titleLen = this.titleTrimLength;
        const descLen = this.descriptionLength;
        return (titleLen >= 1 &&
            titleLen <= TICKET_TITLE_MAX_LENGTH &&
            descLen >= 1 &&
            descLen <= TICKET_DESCRIPTION_MAX_LENGTH);
    }
    protected submitTicketForBooking(): void {
        const booking = this.openingBooking();
        if (!booking) {
            return;
        }
        const title = this.draftTitle.trim();
        const description = this.draftDescription.trim();
        if (!title) {
            this.ticketModalError = 'Il titolo è obbligatorio.';
            return;
        }
        if (title.length > TICKET_TITLE_MAX_LENGTH) {
            this.ticketModalError = `Il titolo non può superare ${TICKET_TITLE_MAX_LENGTH} caratteri.`;
            return;
        }
        if (!description) {
            this.ticketModalError = 'Descrivi il problema prima di inviare.';
            return;
        }
        if (description.length > TICKET_DESCRIPTION_MAX_LENGTH) {
            this.ticketModalError = `La nota non può superare ${TICKET_DESCRIPTION_MAX_LENGTH} caratteri (attuali: ${description.length}).`;
            return;
        }
        this.ticketModalError = '';
        this.ticketService
            .reportIssue({ bookingID: booking.bookingID, title, description })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.toast.success('Segnalazione aperta.');
                this.cancelOpenTicket();
                this.load();
            },
            error: (err: Error) => {
                this.ticketModalError = err.message;
                this.toast.error(err.message || 'Impossibile aprire la segnalazione.');
            }
        });
    }
    protected deleteTicket(ticketID: number): void {
        this.confirmService.confirm({
            title: 'Elimina segnalazione',
            message: 'Eliminare questa segnalazione? L’azione non può essere annullata.',
            confirmLabel: 'Elimina',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(confirmed => {
            if (!confirmed)
                return;
            this.errorMsg = '';
            this.ticketService
                .deleteTicket(ticketID)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.tickets.update(current => current.filter(t => t.ticketID !== ticketID));
                },
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }
}
