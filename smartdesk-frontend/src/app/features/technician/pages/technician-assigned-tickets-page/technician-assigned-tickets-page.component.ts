import { ChangeDetectorRef, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, filter, finalize, switchMap, tap } from 'rxjs/operators';
import { TicketChatPanelComponent } from '../../../../shared/components/ticket-chat-panel/ticket-chat-panel.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { TicketStatusBadgeComponent } from '../../../../shared/components/ticket-status-badge/ticket-status-badge.component';
import { TicketStatusProgressComponent } from '../../../../shared/components/ticket-status-progress/ticket-status-progress.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { ToastService } from '../../../../core/services/toast.service';
import { localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
import { countUnseenIncomingChatMessages, incomingChatMessageSignature, incomingTicketChatMessages, loadChatSeenSignaturesFromStorage } from '../../../../core/utils/ticket-chat.util';
import { TechnicianAssignedSpaceRow, TechnicianService, TechnicianTicketRow } from '../../../../core/services/technician.service';
import { technicianTicketPrimaryHeading, technicianTicketRefSubtitle, technicianTicketTitleTooltip } from '../../../../core/utils/technician-ticket-display';
import { TechnicianSpaceFilterCardsComponent } from '../../components/technician-space-filter-cards/technician-space-filter-cards.component';
import type { TechnicianSpaceFilterValue } from '../../../../core/utils/technician-ticket-list.util';
import { filterTechnicianTicketsBySpace, technicianDeskLabel, technicianSpaceCaption, technicianTicketMatchesSearchQuery, technicianTicketSeverityLabel, technicianTicketStatusLabel } from '../../../../core/utils/technician-ticket-list.util';
export type TechnicianStatusListKind = 'in_progress' | 'verifying' | 'resolved';
import { formatShortDate, formatShortDateTime } from '../../../../core/utils/date.util';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
@Component({
    standalone: true,
    imports: [
        CommonModule,
        EmptyStateComponent,
        TicketStatusBadgeComponent,
        TicketStatusProgressComponent,
        FormsModule,
        TechnicianSpaceFilterCardsComponent,
        TicketChatPanelComponent,
        SdIconComponent,
        SdModalHeaderComponent,
        MdbRippleModule,
        MdbCollapseModule,
        MdbFormsModule
    ],
    templateUrl: './technician-assigned-tickets-page.component.html'
})
export class TechnicianAssignedTicketsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly technicianService = inject(TechnicianService);
    private readonly confirmModal = inject(ConfirmModalService);
    private readonly toast = inject(ToastService);
    private readonly cdr = inject(ChangeDetectorRef);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Segnalazioni assegnate');
    protected readonly ticketsAll = signal<TechnicianTicketRow[]>([]);
    protected readonly assignedSpaces = signal<TechnicianAssignedSpaceRow[]>([]);
    protected readonly spaceFilter = signal<TechnicianSpaceFilterValue>('all');
    protected readonly detailTicket = signal<TechnicianTicketRow | null>(null);
    protected readonly chatTicket = signal<TechnicianTicketRow | null>(null);
    protected readonly resolvedSectionOpen = signal(false);
    protected readonly resolvedHistoryClearing = signal(false);
    protected readonly statusListModal = signal<TechnicianStatusListKind | null>(null);
    protected readonly statusListSearchQuery = signal('');
    protected readonly severityDraft = signal('MEDIUM');
    protected readonly estimatedDateDraft = signal('');
    protected readonly savingFields = signal(false);
    protected readonly severityOptions = [
        { value: 'LOW', label: 'Bassa' },
        { value: 'MEDIUM', label: 'Media' },
        { value: 'HIGH', label: 'Alta' },
        { value: 'CRITICAL', label: 'Critica' }
    ] as const;
    private static readonly VALID_SEVERITIES = new Set(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
    protected readonly assignedSpaceIdSet = computed(() => new Set(this.assignedSpaces().map((s) => s.spaceID)));
    protected readonly workingTicketsAll = computed(() => this.ticketsAll().filter((t) => !this.isResolvedTicket(t)));
    protected readonly inProgressTicketsAll = computed(() => this.ticketsAll().filter((t) => {
        const s = (t.status ?? '').toUpperCase();
        return s === 'IN_PROGRESS' || s === 'OPEN';
    }));
    protected readonly verifyingTicketsAll = computed(() => this.ticketsAll().filter((t) => (t.status ?? '').toUpperCase() === 'VERIFYING'));
    protected readonly resolvedTicketsAll = computed(() => this.ticketsAll().filter((t) => this.isResolvedTicket(t)));
    protected readonly visibleInProgressTickets = computed(() => filterTechnicianTicketsBySpace(this.inProgressTicketsAll(), this.spaceFilter(), this.assignedSpaceIdSet()));
    protected readonly visibleVerifyingTickets = computed(() => filterTechnicianTicketsBySpace(this.verifyingTicketsAll(), this.spaceFilter(), this.assignedSpaceIdSet()));
    protected readonly visibleResolvedTickets = computed(() => filterTechnicianTicketsBySpace(this.resolvedTicketsAll(), this.spaceFilter(), this.assignedSpaceIdSet()));
    protected readonly listEmptyAfterFilter = computed(() => this.inProgressTicketsAll().length > 0 && this.visibleInProgressTickets().length === 0);
    protected readonly onlyArchivedVisible = computed(() => this.visibleInProgressTickets().length === 0 &&
        this.visibleVerifyingTickets().length === 0 &&
        (this.resolvedTicketsAll().length > 0 || this.verifyingTicketsAll().length > 0));
    protected readonly hasResolvedTickets = computed(() => this.resolvedTicketsAll().length > 0);
    protected readonly hasVerifyingTickets = computed(() => this.verifyingTicketsAll().length > 0);
    protected readonly statusListModalTitle = computed(() => {
        const kind = this.statusListModal();
        if (kind === 'in_progress') {
            return 'In lavorazione';
        }
        if (kind === 'verifying') {
            return 'In attesa di conferma host';
        }
        if (kind === 'resolved') {
            return 'Risoluzione confermata';
        }
        return '';
    });
    protected readonly statusListModalSubtitle = computed(() => {
        const n = this.statusListModalTickets().length;
        const word = n === 1 ? 'segnalazione' : 'segnalazioni';
        const filter = this.spaceFilter() !== 'all' ? ' (filtro ufficio attivo)' : '';
        return `${n} ${word}${filter}`;
    });
    protected readonly statusListModalTickets = computed(() => {
        const kind = this.statusListModal();
        let source: TechnicianTicketRow[] = [];
        if (kind === 'in_progress') {
            source = this.inProgressTicketsAll();
        }
        else if (kind === 'verifying') {
            source = this.verifyingTicketsAll();
        }
        else if (kind === 'resolved') {
            source = this.resolvedTicketsAll();
        }
        const bySpace = filterTechnicianTicketsBySpace(source, this.spaceFilter(), this.assignedSpaceIdSet());
        const q = this.statusListSearchQuery();
        return bySpace.filter((t) => technicianTicketMatchesSearchQuery(t, q));
    });
    protected commentDraftByTicketId: Record<number, string> = {};
    protected commentErrorByTicketId: Record<number, string> = {};
    private chatSeenSignatureByTicketId: Record<number, string> = {};
    private static readonly CHAT_SEEN_STORAGE_KEY = 'sd-tech-ticket-chat-seen';
    protected readonly commentSendingId = signal<number | null>(null);
    protected errorMsg = '';
    protected readonly loading = signal(false);
    public ngOnInit(): void {
        this.load();
    }
    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        forkJoin({
            spaces: this.technicianService.getAssignedSpaces().pipe(catchError(() => of([]))),
            tickets: this.technicianService.getAssignedTickets().pipe(catchError(() => of([])))
        })
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ spaces, tickets }) => {
                this.assignedSpaces.set(spaces);
                this.ticketsAll.set(tickets);
                this.commentDraftByTicketId = {};
                this.commentErrorByTicketId = {};
                this.spaceFilter.set('all');
                this.syncChatSeenBaseline(tickets);
                this.syncOpenTickets(tickets);
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.ticketsAll.set([]);
                this.assignedSpaces.set([]);
                this.commentDraftByTicketId = {};
                this.commentErrorByTicketId = {};
                this.detailTicket.set(null);
                this.chatTicket.set(null);
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
    protected onSpaceFilterChange(value: TechnicianSpaceFilterValue): void {
        this.spaceFilter.set(value);
    }
    protected toggleResolvedSection(): void {
        this.resolvedSectionOpen.update((open) => !open);
    }
    protected openStatusListModal(kind: TechnicianStatusListKind): void {
        this.statusListSearchQuery.set('');
        this.statusListModal.set(kind);
    }
    protected closeStatusListModal(): void {
        this.statusListModal.set(null);
        this.statusListSearchQuery.set('');
    }
    protected openResolvedArchiveFromListModal(): void {
        this.closeStatusListModal();
        this.resolvedSectionOpen.set(true);
        window.setTimeout(() => {
            document.getElementById('technician-resolved-history-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 80);
    }
    protected statusListModalHasTickets(kind: TechnicianStatusListKind): boolean {
        if (kind === 'in_progress') {
            return this.inProgressTicketsAll().length > 0;
        }
        if (kind === 'verifying') {
            return this.verifyingTicketsAll().length > 0;
        }
        return this.resolvedTicketsAll().length > 0;
    }
    protected requestClearResolvedHistory(): void {
        if (!this.resolvedTicketsAll().length || this.resolvedHistoryClearing()) {
            return;
        }
        this.confirmModal
            .confirm({
            title: 'Svuotare lo storico?',
            message: 'Le segnalazioni con risoluzione confermata verranno eliminate definitivamente dal tuo archivio. L\'operazione non può essere annullata.',
            confirmLabel: 'Svuota storico',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(filter(Boolean), tap(() => this.resolvedHistoryClearing.set(true)), switchMap(() => this.technicianService.clearResolvedHistory()), finalize(() => this.resolvedHistoryClearing.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (res) => {
                const n = res?.deleted ?? 0;
                this.ticketsAll.update((rows) => rows.filter((t) => !this.isResolvedTicket(t)));
                this.resolvedSectionOpen.set(false);
                this.toast.success(n === 0 ? 'Storico già vuoto.' : `Storico svuotato: ${n} segnalazione/i rimossa/e.`);
                this.cdr.markForCheck();
            },
            error: (err: Error) => this.toast.error(err.message)
        });
    }
    protected openTicketDetail(ticket: TechnicianTicketRow): void {
        this.closeStatusListModal();
        this.chatTicket.set(null);
        this.detailTicket.set(ticket);
        this.severityDraft.set(this.normalizeSeverity(ticket.severity));
        this.estimatedDateDraft.set(this.splitDateOnlyLocal(ticket.estimatedResolutionAt));
    }
    protected selectSeverity(value: string): void {
        this.severityDraft.set(value);
    }
    protected isSeverityDraftValid(): boolean {
        return TechnicianAssignedTicketsPageComponent.VALID_SEVERITIES.has(this.severityDraft().trim().toUpperCase());
    }
    protected closeTicketDetail(): void {
        this.detailTicket.set(null);
    }
    protected openChatModal(ticket: TechnicianTicketRow): void {
        this.closeStatusListModal();
        this.closeTicketDetail();
        this.markTicketChatSeen(ticket);
        this.chatTicket.set(ticket);
    }
    protected closeChatModal(): void {
        this.chatTicket.set(null);
    }
    protected openEstimatedDatePicker(input: HTMLInputElement): void {
        input.focus();
        try {
            input.showPicker();
        }
        catch {
            input.click();
        }
    }
    protected isResolvedTicket(t: TechnicianTicketRow): boolean {
        const s = (t.status ?? '').toUpperCase();
        return s === 'RESOLVED' || s === 'CLOSED';
    }
    protected isVerifyingTicket(t: TechnicianTicketRow): boolean {
        return (t.status ?? '').toUpperCase() === 'VERIFYING';
    }
    protected canEditTicket(ticket: TechnicianTicketRow): boolean {
        const s = (ticket.status ?? '').toUpperCase();
        return s === 'IN_PROGRESS' || s === 'OPEN';
    }
    protected ticketPrimaryHeading(t: TechnicianTicketRow): string {
        return technicianTicketPrimaryHeading(t);
    }
    protected ticketRefSubtitle(t: TechnicianTicketRow): string | null {
        return technicianTicketRefSubtitle(t);
    }
    protected ticketTitleTooltip(t: TechnicianTicketRow): string {
        return technicianTicketTitleTooltip(t);
    }
    protected deskLabel(t: TechnicianTicketRow): string {
        return technicianDeskLabel(t);
    }
    protected spaceCaption(t: TechnicianTicketRow): string {
        return technicianSpaceCaption(t);
    }
    protected workerFullName(ticket: TechnicianTicketRow): string {
        return `${ticket.workerName ?? ''} ${ticket.workerSurname ?? ''}`.trim() || '—';
    }
    protected statusLabel(status: string | undefined): string {
        return technicianTicketStatusLabel(status);
    }
    protected severityLabel(severity: string | null | undefined): string {
        return technicianTicketSeverityLabel(severity) || '—';
    }
    protected formatTicketWhen(iso: string | null | undefined): string {
        return formatShortDateTime(iso) || '—';
    }
    protected formatTicketEstimatedDate(iso: string | null | undefined): string {
        return formatShortDate(iso) || '—';
    }
    protected ticketDescriptionText(ticket: TechnicianTicketRow): string {
        return (ticket.description ?? '').trim();
    }
    protected ticketNewIncomingCount(ticket: TechnicianTicketRow): number {
        return countUnseenIncomingChatMessages(ticket, this.chatSeenSignatureByTicketId[ticket.ticketID]);
    }
    protected hasUnreadChat(ticket: TechnicianTicketRow): boolean {
        return this.ticketNewIncomingCount(ticket) > 0;
    }
    protected markTicketChatSeen(ticket: TechnicianTicketRow): void {
        this.chatSeenSignatureByTicketId[ticket.ticketID] = incomingChatMessageSignature(incomingTicketChatMessages(ticket));
        this.persistChatSeenSignatures();
        this.cdr.markForCheck();
    }
    protected canAddComment(ticket: TechnicianTicketRow): boolean {
        return !this.isResolvedTicket(ticket);
    }
    protected commentDraft(ticketId: number): string {
        return this.commentDraftByTicketId[ticketId] ?? '';
    }
    protected setCommentDraft(ticketId: number, value: string): void {
        this.commentDraftByTicketId[ticketId] = value;
    }
    protected commentError(ticketId: number): string {
        return this.commentErrorByTicketId[ticketId] ?? '';
    }
    protected isCommentSending(ticketId: number): boolean {
        return this.commentSendingId() === ticketId;
    }
    protected submitComment(ticketId: number): void {
        const body = (this.commentDraftByTicketId[ticketId] ?? '').trim();
        if (!body) {
            return;
        }
        this.commentErrorByTicketId[ticketId] = '';
        this.commentSendingId.set(ticketId);
        this.technicianService
            .addComment(ticketId, body)
            .pipe(finalize(() => this.commentSendingId.set(null)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (updated) => {
                this.applyTicketUpdate(updated);
                this.commentDraftByTicketId[ticketId] = '';
                this.toast.success('Commento inviato.');
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.commentErrorByTicketId[ticketId] = err.message;
                this.toast.error(err.message);
                this.cdr.markForCheck();
            }
        });
    }
    protected saveSeverity(): void {
        const ticket = this.detailTicket();
        if (!ticket || !this.canEditTicket(ticket)) {
            return;
        }
        const severity = this.severityDraft().trim().toUpperCase();
        if (!TechnicianAssignedTicketsPageComponent.VALID_SEVERITIES.has(severity)) {
            this.toast.error('Seleziona una gravità prima di salvare.');
            return;
        }
        this.patchTicket(ticket, { severity }, 'Gravità aggiornata.');
    }
    protected saveEstimatedResolution(): void {
        const ticket = this.detailTicket();
        if (!ticket || !this.canEditTicket(ticket)) {
            return;
        }
        const iso = this.combineDateOnlyLocal(this.estimatedDateDraft());
        if (!iso) {
            this.toast.error('Inserisci una data di risoluzione stimata valida.');
            return;
        }
        this.patchTicket(ticket, { estimatedResolutionAt: iso }, 'Data di risoluzione stimata aggiornata.');
    }
    protected confirmMarkRepaired(ticket: TechnicianTicketRow): void {
        if (!this.canEditTicket(ticket)) {
            return;
        }
        this.confirmModal
            .confirm({
            title: 'Segna come riparato',
            message: 'La segnalazione passerà in attesa di convalida da parte dell\'host. Continuare?',
            confirmLabel: 'Conferma',
            cancelLabel: 'Annulla',
            variant: 'success'
        })
            .pipe(filter((ok) => ok), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.markRepaired(ticket));
    }
    protected markRepaired(ticket: TechnicianTicketRow): void {
        const resolution = this.ticketDescriptionText(ticket) || 'Intervento completato dal tecnico.';
        this.errorMsg = '';
        this.technicianService
            .updateStatus(ticket.ticketID, 'VERIFYING', '', resolution)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (updated) => {
                this.applyTicketUpdate(updated);
                this.detailTicket.set(null);
                this.toast.success('Segnalazione inviata in attesa di convalida host.');
                this.load();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.toast.error(err.message);
            }
        });
    }
    private patchTicket(ticket: TechnicianTicketRow, patch: {
        severity?: string;
        estimatedResolutionAt?: string;
    }, successMessage: string): void {
        this.savingFields.set(true);
        this.errorMsg = '';
        this.technicianService
            .updateStatus(ticket.ticketID, this.apiStatusFor(ticket), '', undefined, patch.severity, patch.estimatedResolutionAt)
            .pipe(finalize(() => this.savingFields.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (updated) => {
                this.applyTicketUpdate(updated);
                this.severityDraft.set(this.normalizeSeverity(updated.severity));
                this.estimatedDateDraft.set(this.splitDateOnlyLocal(updated.estimatedResolutionAt));
                this.toast.success(successMessage);
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.toast.error(err.message);
            }
        });
    }
    private applyTicketUpdate(updated: TechnicianTicketRow): void {
        this.ticketsAll.update((rows) => rows.map((t) => (t.ticketID === updated.ticketID ? updated : t)));
        this.syncOpenTickets(this.ticketsAll());
    }
    private syncChatSeenBaseline(_tickets: TechnicianTicketRow[]): void {
        this.chatSeenSignatureByTicketId = this.loadChatSeenFromStorage();
    }
    private loadChatSeenFromStorage(): Record<number, string> {
        return loadChatSeenSignaturesFromStorage(TechnicianAssignedTicketsPageComponent.CHAT_SEEN_STORAGE_KEY);
    }
    private persistChatSeenSignatures(): void {
        try {
            sessionStorage.setItem(TechnicianAssignedTicketsPageComponent.CHAT_SEEN_STORAGE_KEY, JSON.stringify(this.chatSeenSignatureByTicketId));
        }
        catch {
        }
    }
    private syncOpenTickets(tickets: TechnicianTicketRow[]): void {
        const detail = this.detailTicket();
        if (detail) {
            this.detailTicket.set(tickets.find((t) => t.ticketID === detail.ticketID) ?? null);
        }
        const chat = this.chatTicket();
        if (chat) {
            this.chatTicket.set(tickets.find((t) => t.ticketID === chat.ticketID) ?? null);
        }
    }
    private normalizeSeverity(raw: string | null | undefined): string {
        const u = (raw ?? '').trim().toUpperCase();
        return TechnicianAssignedTicketsPageComponent.VALID_SEVERITIES.has(u) ? u : 'MEDIUM';
    }
    private splitDateOnlyLocal(iso: string | null | undefined): string {
        if (!iso) {
            return '';
        }
        const d = new Date(iso);
        if (!Number.isFinite(d.getTime())) {
            return '';
        }
        return localCalendarDateIsoFromDate(d);
    }
    private combineDateOnlyLocal(date: string): string | undefined {
        const d = date.trim();
        if (!d) {
            return undefined;
        }
        const parsed = new Date(`${d}T12:00:00`);
        if (!Number.isFinite(parsed.getTime())) {
            return undefined;
        }
        return parsed.toISOString();
    }
    private apiStatusFor(ticket: TechnicianTicketRow): 'IN_PROGRESS' | 'VERIFYING' | 'RESOLVED' {
        const s = (ticket.status ?? '').toUpperCase();
        if (s === 'VERIFYING') {
            return 'VERIFYING';
        }
        if (s === 'RESOLVED' || s === 'CLOSED') {
            return 'RESOLVED';
        }
        return 'IN_PROGRESS';
    }
}
