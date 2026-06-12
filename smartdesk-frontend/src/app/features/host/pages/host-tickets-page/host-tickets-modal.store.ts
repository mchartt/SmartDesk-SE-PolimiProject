import { computed, DestroyRef, inject, Injectable, signal, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, filter, finalize, forkJoin, Observable, of } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { Desk } from '../../../../core/models';
import { HostService, HostTechnicianDto } from '../../../../core/services/host.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { normalizeForSearch } from '../../../../core/utils/search.util';
import {
    appendHostNoteToTicket,
    buildPendingApprovalDesksByRoom,
    buildRoomsWithUnresolvedTickets,
    cloneTicketRecord,
    deskRowPrimaryLabel,
    desksWithOpenTicketsInRoom,
    filterAssignTechnicians,
    filterDesksBySearch,
    filterRoomRowsBySearch,
    HOST_TICKET_SEVERITY_OPTIONS,
    HostPendingApprovalItem,
    HostSpaceTicketsBundle,
    HostTicketsModalStep,
    modalSelectedRoomTitle,
    normalizeTicketSeverity,
    patchTicketInBundle,
    resolvedTicketMatchesModalSearch,
    ticketNumericId,
    unresolvedTicketCount,
    unresolvedTicketsForDesk,
    verifyingRowMatchesModalSearch
} from './host-tickets.util';

export { HOST_TICKET_SEVERITY_OPTIONS };

export interface HostTicketsModalHost {
    bundles: WritableSignal<HostSpaceTicketsBundle[]>;
    allVerifyingTickets(): HostPendingApprovalItem[];
    onPageError(message: string): void;
    hostChatAuthorLabel(): string;
    reloadBundles(): void;
}

@Injectable()
export class HostTicketsModalStore {
    private readonly hostService = inject(HostService);
    private readonly confirmModal = inject(ConfirmModalService);
    private readonly destroyRef = inject(DestroyRef);

    private host: HostTicketsModalHost | null = null;

    readonly modalBundle = signal<HostSpaceTicketsBundle | null>(null);
    readonly modalStep = signal<HostTicketsModalStep>('rooms');
    readonly modalRoomKey = signal<string | null>(null);
    readonly modalDeskId = signal<number | null>(null);
    readonly roomSearchQuery = signal('');
    readonly deskSearchQuery = signal('');
    readonly ticketDetailExpandedIds = signal<ReadonlySet<number>>(new Set());
    readonly assignTicketTechModalTicket = signal<Record<string, unknown> | null>(null);
    readonly assignModalContext = signal<{ space: string; desk: string } | null>(null);
    readonly maintReassignSelection = signal<number | 'same' | null>(null);
    readonly assignTicketTechCandidates = signal<HostTechnicianDto[]>([]);
    readonly assignTicketTechLoading = signal(false);
    readonly assignTicketTechAssigning = signal(false);
    readonly assignTicketTechSearch = signal('');
    readonly assignTicketTechError = signal('');
    readonly assignTicketSeverity = signal('MEDIUM');
    readonly isReassigning = signal(false);
    readonly actionSuccessToast = signal<string | null>(null);
    readonly resolvedHistoryModalOpen = signal(false);
    readonly historyModalSearchQuery = signal('');
    readonly resolvedTickets = signal<Record<string, unknown>[]>([]);
    readonly resolvedTicketsLoading = signal(false);
    readonly resolvedHistoryFetched = signal(false);
    readonly resolvedHistoryError = signal('');
    readonly resolvedHistoryClearing = signal(false);
    readonly approvalQueueModalOpen = signal(false);
    readonly approvalModalSearchQuery = signal('');
    readonly approvalDetailRow = signal<HostPendingApprovalItem | null>(null);
    readonly pendingApprovalViewMode = signal<'rooms' | 'flat'>('rooms');
    readonly commentSendingId = signal<number | null>(null);
    readonly isActionPerforming = signal(false);
    readonly deskActionInProgress = signal<number | null>(null);

    commentDraftByTicketId: Record<number, string> = {};
    commentErrorByTicketId: Record<number, string> = {};

    readonly pendingApprovalSpaceModalDesksByRoom = computed(() =>
        buildPendingApprovalDesksByRoom(this.h().allVerifyingTickets())
    );

    readonly approvalModalFilteredTickets = computed(() => {
        const q = normalizeForSearch(this.approvalModalSearchQuery());
        const rows = this.h().allVerifyingTickets();
        if (!q) {
            return rows;
        }
        return rows.filter((row) => verifyingRowMatchesModalSearch(row, q));
    });

    readonly historyModalFilteredTickets = computed(() => {
        const q = normalizeForSearch(this.historyModalSearchQuery());
        const rows = this.resolvedTickets();
        if (!q) {
            return rows;
        }
        return rows.filter((t) => resolvedTicketMatchesModalSearch(t, q));
    });

    readonly modalUnresolvedTicketTotal = computed(() => {
        const bundle = this.modalBundle();
        return bundle ? unresolvedTicketCount(bundle) : 0;
    });

    readonly modalRoomsWithTickets = computed(() => {
        const bundle = this.modalBundle();
        return bundle ? buildRoomsWithUnresolvedTickets(bundle) : [];
    });

    readonly modalRoomsFiltered = computed(() =>
        filterRoomRowsBySearch(this.modalRoomsWithTickets(), this.roomSearchQuery())
    );

    readonly desksWithOpenInSelectedRoom = computed(() => {
        const bundle = this.modalBundle();
        const rk = this.modalRoomKey();
        if (!bundle || !rk) {
            return [] as Desk[];
        }
        return desksWithOpenTicketsInRoom(bundle, rk);
    });

    readonly modalDesksFiltered = computed(() =>
        filterDesksBySearch(this.desksWithOpenInSelectedRoom(), this.deskSearchQuery())
    );

    readonly selectedRoomTitle = computed(() => {
        const rk = this.modalRoomKey();
        const bundle = this.modalBundle();
        if (!rk || !bundle) {
            return '';
        }
        return modalSelectedRoomTitle(bundle, rk);
    });

    readonly selectedDesk = computed(() => {
        const bundle = this.modalBundle();
        const id = this.modalDeskId();
        if (!bundle || id == null) {
            return null;
        }
        return bundle.desks.find((d) => d.id === id) ?? null;
    });

    readonly selectedDeskUnresolvedTickets = computed(() => {
        const bundle = this.modalBundle();
        const id = this.modalDeskId();
        if (!bundle || id == null) {
            return [] as Record<string, unknown>[];
        }
        return unresolvedTicketsForDesk(bundle, id);
    });

    readonly assignTicketTechFiltered = computed(() => {
        const ticket = this.assignTicketTechModalTicket();
        const curId = ticket?.['assignedTechID'] != null ? Number(ticket['assignedTechID']) : null;
        return filterAssignTechnicians(
            this.assignTicketTechCandidates(),
            this.assignTicketTechSearch(),
            this.isReassigning(),
            curId != null && Number.isFinite(curId) ? curId : null
        );
    });

    bindHost(host: HostTicketsModalHost): void {
        this.host = host;
    }

    private h(): HostTicketsModalHost {
        if (!this.host) {
            throw new Error('HostTicketsModalStore: host not bound');
        }
        return this.host;
    }

    onBundlesLoaded(): void {
        this.invalidateResolvedHistoryCache();
        this.prefetchResolvedHistoryCount();
    }

    onBundlesLoadFailed(): void {
        this.invalidateResolvedHistoryCache();
    }

    openApprovalQueueModal(): void {
        this.approvalModalSearchQuery.set('');
        this.approvalQueueModalOpen.set(true);
    }

    closeApprovalQueueModal(): void {
        this.approvalQueueModalOpen.set(false);
        this.approvalModalSearchQuery.set('');
        this.approvalDetailRow.set(null);
    }

    openApprovalTicketDetail(row: HostPendingApprovalItem): void {
        this.approvalDetailRow.set(row);
    }

    closeApprovalTicketDetail(): void {
        this.approvalDetailRow.set(null);
    }

    openResolvedHistoryModal(): void {
        this.historyModalSearchQuery.set('');
        this.resolvedHistoryModalOpen.set(true);
        this.loadResolvedTicketsHistory(false);
    }

    closeResolvedHistoryModal(): void {
        this.resolvedHistoryModalOpen.set(false);
        this.historyModalSearchQuery.set('');
    }

    openOfficeModal(bundle: HostSpaceTicketsBundle): void {
        if (unresolvedTicketCount(bundle) === 0) {
            return;
        }
        this.modalBundle.set(bundle);
        this.modalStep.set('rooms');
        this.modalRoomKey.set(null);
        this.modalDeskId.set(null);
        this.roomSearchQuery.set('');
        this.deskSearchQuery.set('');
        this.ticketDetailExpandedIds.set(new Set());
    }

    closeOfficeModal(): void {
        this.closeAssignTicketTechModal();
        this.modalBundle.set(null);
        this.modalStep.set('rooms');
        this.modalRoomKey.set(null);
        this.modalDeskId.set(null);
        this.roomSearchQuery.set('');
        this.deskSearchQuery.set('');
        this.ticketDetailExpandedIds.set(new Set());
    }

    pickRoom(key: string): void {
        this.modalRoomKey.set(key);
        this.deskSearchQuery.set('');
        this.modalStep.set('desks');
    }

    pickDesk(deskId: number): void {
        this.modalDeskId.set(deskId);
        this.modalStep.set('detail');
    }

    modalBack(): void {
        const step = this.modalStep();
        if (step === 'detail') {
            this.modalDeskId.set(null);
            this.modalStep.set('desks');
            return;
        }
        if (step === 'desks') {
            this.modalRoomKey.set(null);
            this.deskSearchQuery.set('');
            this.modalStep.set('rooms');
        }
    }

    loadResolvedTicketsHistory(force: boolean): void {
        if (this.resolvedTicketsLoading()) {
            return;
        }
        if (this.resolvedHistoryFetched() && !force) {
            return;
        }
        this.resolvedHistoryError.set('');
        this.resolvedTicketsLoading.set(true);
        this.hostService
            .getResolvedTickets(100)
            .pipe(finalize(() => this.resolvedTicketsLoading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    this.resolvedTickets.set(rows);
                    this.resolvedHistoryFetched.set(true);
                },
                error: (err: Error) => this.resolvedHistoryError.set(err.message)
            });
    }

    requestClearResolvedHistory(): void {
        if (!this.resolvedTickets().length || this.resolvedHistoryClearing()) {
            return;
        }
        this.confirmModal
            .confirm({
                title: 'Pulire lo storico?',
                message:
                    'Le segnalazioni risolte verranno eliminate definitivamente dal sistema per tutti i tuoi spazi. L’operazione non può essere annullata.',
                confirmLabel: 'Pulisci storico',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(
                filter(Boolean),
                tap(() => {
                    this.resolvedHistoryError.set('');
                    this.resolvedHistoryClearing.set(true);
                }),
                switchMap(() => this.hostService.clearResolvedTickets()),
                finalize(() => this.resolvedHistoryClearing.set(false)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (res) => {
                    const n = res?.deleted ?? 0;
                    this.resolvedTickets.set([]);
                    this.resolvedHistoryFetched.set(true);
                    if (!this.resolvedTickets().length) {
                        this.closeResolvedHistoryModal();
                    }
                    this.flashActionSuccess(
                        n === 0 ? 'Storico già vuoto.' : `Storico pulito: ${n} segnalazione/i rimossa/e.`,
                        5000
                    );
                },
                error: (err: Error) => this.resolvedHistoryError.set(err.message)
            });
    }

    flashActionSuccess(msg = 'Operazione riuscita.', durationMs = 4000): void {
        this.actionSuccessToast.set(msg);
        window.setTimeout(() => this.actionSuccessToast.set(null), durationMs);
    }

    isTicketDetailExpanded(ticket: Record<string, unknown>): boolean {
        const id = ticketNumericId(ticket);
        return id != null && this.ticketDetailExpandedIds().has(id);
    }

    toggleTicketDetail(ticket: Record<string, unknown>): void {
        const id = ticketNumericId(ticket);
        if (id == null) {
            return;
        }
        this.ticketDetailExpandedIds.update((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            }
            else {
                next.add(id);
            }
            return next;
        });
    }

    openAssignTicketTechnicianModal(ticket: Record<string, unknown>): void {
        const tid = ticketNumericId(ticket);
        if (tid == null) {
            return;
        }
        this.isReassigning.set(false);
        this.assignModalContext.set(null);
        this.assignTicketTechError.set('');
        this.assignTicketTechSearch.set('');
        this.assignTicketSeverity.set(normalizeTicketSeverity(ticket['severity'], HOST_TICKET_SEVERITY_OPTIONS));
        this.assignTicketTechModalTicket.set(ticket);
        this.loadTechniciansForAssignModal(null);
    }

    openMaintenanceReassignModal(row: HostPendingApprovalItem): void {
        if (!row.ticket) {
            return;
        }
        const tid = ticketNumericId(row.ticket);
        if (tid == null) {
            return;
        }
        this.isReassigning.set(true);
        this.assignModalContext.set({
            space: row.space.name,
            desk: deskRowPrimaryLabel(row.desk)
        });
        this.maintReassignSelection.set(row.ticket['assignedTechID'] != null ? 'same' : null);
        this.assignTicketTechError.set('');
        this.assignTicketTechSearch.set('');
        this.assignTicketTechModalTicket.set(row.ticket);
        this.loadTechniciansForAssignModal(row.space.spaceID);
    }

    closeAssignTicketTechModal(): void {
        if (this.assignTicketTechAssigning()) {
            return;
        }
        this.assignTicketTechModalTicket.set(null);
        this.assignTicketTechCandidates.set([]);
        this.assignTicketTechSearch.set('');
        this.assignTicketTechError.set('');
        this.assignTicketSeverity.set('MEDIUM');
        this.assignTicketTechLoading.set(false);
        this.isReassigning.set(false);
        this.assignModalContext.set(null);
        this.maintReassignSelection.set(null);
    }

    selectMaintReassignSame(): void {
        this.maintReassignSelection.set('same');
    }

    selectMaintReassignTechnician(technicianId: number): void {
        this.maintReassignSelection.set(technicianId);
    }

    isMaintReassignSameSelected(): boolean {
        return this.maintReassignSelection() === 'same';
    }

    isMaintReassignTechSelected(technicianId: number): boolean {
        return this.maintReassignSelection() === technicianId;
    }

    canConfirmMaintReassign(): boolean {
        return (
            this.maintReassignSelection() !== null &&
            !this.assignTicketTechLoading() &&
            !this.assignTicketTechAssigning()
        );
    }

    confirmMaintReassignChoice(): void {
        const sel = this.maintReassignSelection();
        if (sel == null) {
            return;
        }
        if (sel === 'same') {
            this.executeRejectSameTechnician();
            return;
        }
        const tech = this.assignTicketTechCandidates().find((t) => t.technicianID === sel);
        const name = tech?.name?.trim() || `tecnico #${sel}`;
        this.executeRejectOtherTechnician(sel, name);
    }

    assignModalEyebrow(): string {
        return this.isReassigning() ? 'Rimetti in manutenzione' : 'Assegna alla segnalazione';
    }

    assignModalSameTechnicianVisible(): boolean {
        const ticket = this.assignTicketTechModalTicket();
        if (!ticket || !this.isReassigning()) {
            return false;
        }
        return ticket['assignedTechID'] != null;
    }

    commentDraft(ticketId: number): string {
        return this.commentDraftByTicketId[ticketId] ?? '';
    }

    setCommentDraft(ticketId: number, value: string): void {
        this.commentDraftByTicketId[ticketId] = value;
    }

    commentError(ticketId: number): string {
        return this.commentErrorByTicketId[ticketId] ?? '';
    }

    isCommentSending(ticketId: number): boolean {
        return this.commentSendingId() === ticketId;
    }

    submitComment(ticketId: number): void {
        const body = (this.commentDraftByTicketId[ticketId] ?? '').trim();
        if (!body) {
            return;
        }
        const current = this.findTicketRecord(ticketId);
        if (!current) {
            return;
        }
        const snapshot = cloneTicketRecord(current);
        const optimistic = appendHostNoteToTicket(current, body, this.h().hostChatAuthorLabel());
        this.commentErrorByTicketId[ticketId] = '';
        this.commentDraftByTicketId[ticketId] = '';
        this.updateLocalTicket(optimistic);
        this.commentSendingId.set(ticketId);
        this.hostService
            .addHostComment(ticketId, body)
            .pipe(finalize(() => this.commentSendingId.set(null)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (updated) => this.updateLocalTicket(updated),
                error: (err: Error) => {
                    this.commentErrorByTicketId[ticketId] = err.message;
                    this.commentDraftByTicketId[ticketId] = body;
                    this.updateLocalTicket(snapshot);
                }
            });
    }

    requestApproveTicket(ticketId: number): void {
        this.confirmModal
            .confirm({
                title: 'Approva riparazione?',
                message:
                    'La segnalazione verrà chiusa come risolta. Il lavoratore riceverà conferma e la segnalazione andrà nello storico.',
                confirmLabel: 'Approva',
                cancelLabel: 'Annulla',
                variant: 'primary'
            })
            .pipe(
                filter(Boolean),
                switchMap(() => {
                    this.isActionPerforming.set(true);
                    return this.hostService.approveTicket(ticketId);
                }),
                finalize(() => this.isActionPerforming.set(false)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (updated) => {
                    this.updateLocalTicket(updated);
                    this.invalidateResolvedHistoryCache();
                    if (!this.h().allVerifyingTickets().length) {
                        this.closeApprovalQueueModal();
                    }
                    this.flashActionSuccess('Riparazione approvata e chiusa.');
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    requestDismissDesk(ticketId: number): void {
        this.confirmModal
            .confirm({
                title: 'Dismettere postazione?',
                message: 'La segnalazione verrà chiusa e la postazione non sarà più prenotabile. Confermi?',
                confirmLabel: 'Dismetti',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(
                filter(Boolean),
                switchMap(() => {
                    this.isActionPerforming.set(true);
                    return this.hostService.dismissDeskTicket(ticketId);
                }),
                finalize(() => this.isActionPerforming.set(false)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (updated) => {
                    this.updateLocalTicket(updated);
                    if (!this.h().allVerifyingTickets().length) {
                        this.closeApprovalQueueModal();
                    }
                    this.flashActionSuccess('Postazione dismessa e segnalazione chiusa.');
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    confirmAssignTicketTechnician(technicianId: number): void {
        const ticket = this.assignTicketTechModalTicket();
        const tid = ticket ? ticketNumericId(ticket) : null;
        if (tid == null) {
            return;
        }
        if (this.isReassigning()) {
            this.selectMaintReassignTechnician(technicianId);
            return;
        }
        this.assignTicketTechError.set('');
        this.assignTicketTechAssigning.set(true);
        this.hostService
            .assignTechnicianToTicket(tid, technicianId, this.assignTicketSeverity())
            .pipe(finalize(() => this.assignTicketTechAssigning.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (updated) => {
                    this.flashActionSuccess('Tecnico assegnato alla segnalazione.');
                    this.updateLocalTicket(updated);
                    this.closeAssignTicketTechModal();
                },
                error: (err: Error) => this.assignTicketTechError.set(err.message)
            });
    }

    deskActionBusy(deskId: number): boolean {
        return this.deskActionInProgress() === deskId;
    }

    approveInspection(desk: Desk): void {
        if (this.deskActionInProgress() !== null || desk.currentState !== 'PENDING_INSPECTION') {
            return;
        }
        this.runDeskStateAction(desk.id, this.hostService.approveInspection(desk.id));
    }

    rejectInspection(desk: Desk): void {
        if (this.deskActionInProgress() !== null || desk.currentState !== 'PENDING_INSPECTION') {
            return;
        }
        this.confirmModal
            .confirm({
                title: 'Rimetti in manutenzione',
                message: `Postazione ${desk.code}: verrà rimessa in manutenzione senza una segnalazione associata.`,
                confirmLabel: 'Conferma',
                cancelLabel: 'Annulla',
                variant: 'warning'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                this.runDeskStateAction(desk.id, this.hostService.rejectInspection(desk.id));
            });
    }

    decommissionDesk(desk: Desk): void {
        if (this.deskActionInProgress() !== null || desk.currentState !== 'PENDING_INSPECTION') {
            return;
        }
        this.confirmModal
            .confirm({
                title: 'Dismetti postazione',
                message: `Postazione ${desk.code}: operazione irreversibile.`,
                confirmLabel: 'Dismetti',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                this.runDeskStateAction(desk.id, this.hostService.decommissionDesk(desk.id));
            });
    }

    approveRoomInspections(items: HostPendingApprovalItem[]): void {
        if (!items.length) {
            return;
        }
        const desks = items.map((i) => i.desk);
        this.confirmModal
            .confirm({
                title: 'Approva tutte le postazioni',
                message: `Vuoi approvare tutte e ${desks.length} le postazioni della sala?`,
                confirmLabel: 'Approva tutte',
                cancelLabel: 'Annulla',
                variant: 'success'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                const reqs = desks.map((d) =>
                    this.hostService.approveInspection(d.id).pipe(catchError(() => of(d)))
                );
                this.isActionPerforming.set(true);
                forkJoin(reqs)
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                    next: () => {
                        this.isActionPerforming.set(false);
                        this.h().reloadBundles();
                    },
                    error: () => {
                        this.isActionPerforming.set(false);
                        this.h().reloadBundles();
                    }
                });
            });
    }

    private loadTechniciansForAssignModal(spaceId: number | null): void {
        this.assignTicketTechLoading.set(true);
        this.assignTicketTechCandidates.set([]);
        const source =
            this.isReassigning() && spaceId != null
                ? this.hostService.getTechniciansForSpace(spaceId)
                : this.hostService.getAllTechnicians();
        source
            .pipe(finalize(() => this.assignTicketTechLoading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    this.assignTicketTechCandidates.set(
                        rows.map((r) => ({ ...r, assignedSpaces: r.assignedSpaces ?? [] }))
                    );
                },
                error: (err: Error) => this.assignTicketTechError.set(err.message)
            });
    }

    private executeRejectSameTechnician(): void {
        const ticket = this.assignTicketTechModalTicket();
        if (!ticket) {
            return;
        }
        const tid = ticketNumericId(ticket);
        if (tid == null) {
            return;
        }
        this.assignTicketTechError.set('');
        this.assignTicketTechAssigning.set(true);
        this.hostService
            .rejectTicket(tid, { reason: 'Host: riapertura manutenzione (stesso tecnico)' })
            .pipe(finalize(() => this.assignTicketTechAssigning.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (updated) => {
                    this.updateLocalTicket(updated);
                    this.flashActionSuccess('Manutenzione riaperta con lo stesso tecnico.');
                    this.closeAssignTicketTechModal();
                    if (!this.h().allVerifyingTickets().length) {
                        this.closeApprovalQueueModal();
                    }
                },
                error: (err: Error) => this.assignTicketTechError.set(err.message)
            });
    }

    private executeRejectOtherTechnician(technicianId: number, technicianName: string): void {
        const ticket = this.assignTicketTechModalTicket();
        const tid = ticket ? ticketNumericId(ticket) : null;
        if (tid == null) {
            return;
        }
        this.assignTicketTechError.set('');
        this.assignTicketTechAssigning.set(true);
        this.hostService
            .rejectTicket(tid, {
                newTechnicianId: technicianId,
                reason: 'Host: riapertura manutenzione (nuovo tecnico)'
            })
            .pipe(finalize(() => this.assignTicketTechAssigning.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (updated) => {
                    this.updateLocalTicket(updated);
                    this.flashActionSuccess(`Manutenzione riaperta e assegnata a ${technicianName}.`);
                    this.closeAssignTicketTechModal();
                    if (!this.h().allVerifyingTickets().length) {
                        this.closeApprovalQueueModal();
                    }
                },
                error: (err: Error) => this.assignTicketTechError.set(err.message)
            });
    }

     private runDeskStateAction(deskId: number, req$: Observable<Desk>): void {
        this.deskActionInProgress.set(deskId);
        req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.deskActionInProgress.set(null);
                this.h().reloadBundles();
            },
            error: (err: Error) => {
                this.h().onPageError(err.message);
                this.deskActionInProgress.set(null);
            }
        });
    }

    private updateLocalTicket(updated: Record<string, unknown>): void {
        const bundles = this.h().bundles;
        bundles.update((bs) => bs.map((b) => patchTicketInBundle(b, updated)));
        const modal = this.modalBundle();
        if (modal) {
            this.modalBundle.set(patchTicketInBundle(modal, updated));
        }
    }

    private findTicketRecord(ticketId: number): Record<string, unknown> | null {
        const sources: (HostSpaceTicketsBundle | null)[] = [this.modalBundle(), ...this.h().bundles()];
        for (const bundle of sources) {
            if (!bundle) {
                continue;
            }
            for (const tickets of bundle.ticketsByDeskId.values()) {
                const hit = tickets.find((t) => t['ticketID'] === ticketId);
                if (hit) {
                    return hit;
                }
            }
        }
        return null;
    }

    private invalidateResolvedHistoryCache(): void {
        this.resolvedHistoryFetched.set(false);
        this.resolvedTickets.set([]);
        this.resolvedHistoryError.set('');
        if (this.resolvedHistoryModalOpen()) {
            queueMicrotask(() => this.loadResolvedTicketsHistory(true));
        }
    }

    private prefetchResolvedHistoryCount(): void {
        if (!this.resolvedHistoryFetched() && !this.resolvedTicketsLoading()) {
            this.loadResolvedTicketsHistory(false);
        }
    }
}
