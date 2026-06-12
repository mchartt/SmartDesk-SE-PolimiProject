import {
    computed,
    DestroyRef,
    inject,
    Injectable,
    signal,
    WritableSignal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, finalize, map, switchMap, take, type Observable } from 'rxjs';
import { Space } from '../../../../core/models';
import { HostService, HostTechnicianAssignedSpaceDto, HostTechnicianDto } from '../../../../core/services/host.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import {
    assignableSpaceCount,
    assignSpacePickerPrimary,
    assignSpacePickerSecondary,
    assignTechPickerPrimary,
    assignTechPickerSecondary,
    buildCreateTechnicianDisplayName,
    createCognomeInlineError,
    createCognomeInvalid,
    createEmailInlineError,
    createEmailInvalid,
    createNomeInlineError,
    createNomeInvalid,
    createPasswordInlineError,
    createPasswordInvalid,
    createSpecializationInlineError,
    createSpecializationInvalid,
    fieldErrorsFromValidationResponse,
    filterAssignableSpaces,
    filterDashboardTechnicians,
    filterModalTechnicians,
    filterTechniciansBySearch,
    hiddenAssignedSpacesCount,
    HOST_TECH_ASSIGN_SUCCESS_TOAST_MS,
    HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS,
    HOST_TECH_RECENT_LIMIT,
    HostTechnicianAssignmentTicketContext,
    HostTechCreateFormState,
    isCreateTechnicianFormValid,
    isSpaceApproved,
    normalizeTechnicianRow,
    showDashboardInlineSearch,
    showSeeAllTechniciansCta,
    sortTechniciansRecentFirst,
    spaceCardAriaLabel,
    spacePickTechnicianLabel,
    spacesAssignableFor,
    spacesPickFilteredList,
    techInitials,
    techMgmtModalFiltersActive,
    technicianCountForSpace,
    technicianDisplayCode,
    technicianSpecDisplay,
    validateEditTechnicianFields,
    visibleAssignedSpaces
} from './host-technicians.util';

export { HOST_TECH_RECENT_LIMIT, HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS };

export interface HostTechniciansModalHost {
    approvedSpaces: () => Space[];
    selectedSpaceId: WritableSignal<number | null>;
    technicians: WritableSignal<HostTechnicianDto[]>;
    allTechnicians: WritableSignal<HostTechnicianDto[]>;
    loading: () => boolean;
    loadingTechs: WritableSignal<boolean>;
    hasNoSpaces: () => boolean;
    hasNoApprovedSpaces: () => boolean;
    selectedSpace: () => Space | null;
    selectedSpaceApproved: () => boolean;
    pageError: () => string;
    onPageError: (message: string) => void;
    loadTechnicians: () => void;
    loadAllTechnicians: () => void;
}

@Injectable()
export class HostTechniciansModalStore {
    private readonly hostService = inject(HostService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly destroyRef = inject(DestroyRef);

    private host: HostTechniciansModalHost | null = null;

    readonly spaceTechModalOpen = signal(false);
    readonly spaceModalTechSearch = signal('');
    readonly loadingAllTechs = signal(false);
    readonly techQuickQuery = signal('');
    readonly techModalUnifiedSearch = signal('');
    readonly techFilterNome = signal('');
    readonly techFilterCognome = signal('');
    readonly techFilterCodice = signal('');
    readonly isTechSearchModalOpen = signal(false);
    readonly isSpacePickModalOpen = signal(false);
    readonly spacePickTechnicianId = signal<number | null>(null);
    readonly spacePickSearchQuery = signal('');
    readonly spacePickAssigning = signal(false);
    readonly assignPickSpaceModalOpen = signal(false);
    readonly assignPickSpaceSearch = signal('');
    readonly assignPickTechModalOpen = signal(false);
    readonly assignPickTechSearch = signal('');
    readonly assignmentTicketContext = signal<HostTechnicianAssignmentTicketContext | null>(null);
    readonly editSaving = signal(false);
    readonly createFormShowErrors = signal(false);
    readonly createApiFieldErrors = signal<Record<string, string>>({});
    readonly technicianIdToAssign = signal<number | null>(null);
    readonly assignFlowBusy = signal(false);
    readonly assignSuccessMsg = signal('');
    readonly isModalOpen = signal(false);
    readonly isEditModalOpen = signal(false);
    readonly assignedSpacesDetailTechId = signal<number | null>(null);

    createNome = '';
    createCognome = '';
    email = '';
    password = '';
    specialization = '';
    editName = '';
    editEmail = '';
    editSpecialization = '';
    editPassword = '';
    editProfileVersion = 0;
    editingTechnicianId: number | null = null;

    readonly assignedSpacesModalTechnician = computed(() => {
        const id = this.assignedSpacesDetailTechId();
        if (id == null) {
            return null;
        }
        return this.h().allTechnicians().find((x) => x.technicianID === id) ?? null;
    });

    readonly techniciansSortedRecent = computed(() => sortTechniciansRecentFirst(this.h().allTechnicians()));

    readonly dashboardTechnicians = computed(() =>
        filterDashboardTechnicians(this.techniciansSortedRecent(), this.techQuickQuery(), HOST_TECH_RECENT_LIMIT)
    );

    readonly modalFilteredTechnicians = computed(() =>
        filterModalTechnicians(this.techniciansSortedRecent(), {
            unified: this.techModalUnifiedSearch(),
            nome: this.techFilterNome(),
            cognome: this.techFilterCognome(),
            codice: this.techFilterCodice()
        })
    );

    readonly techMgmtModalFiltersActive = computed(() =>
        techMgmtModalFiltersActive({
            unified: this.techModalUnifiedSearch(),
            nome: this.techFilterNome(),
            cognome: this.techFilterCognome(),
            codice: this.techFilterCodice()
        })
    );

    readonly showDashboardInlineSearch = computed(() =>
        showDashboardInlineSearch(this.h().allTechnicians().length, HOST_TECH_RECENT_LIMIT)
    );

    readonly showSeeAllTechniciansCta = computed(() =>
        showSeeAllTechniciansCta(this.h().allTechnicians().length, HOST_TECH_RECENT_LIMIT)
    );

    readonly technicianTotalCount = computed(() => this.h().allTechnicians().length);

    readonly spaceModalFilteredTechnicians = computed(() =>
        filterTechniciansBySearch(this.h().technicians(), this.spaceModalTechSearch())
    );

    readonly spacePickTechnicianLabel = computed(() => {
        const id = this.spacePickTechnicianId();
        if (id == null) {
            return '';
        }
        const t = this.h().allTechnicians().find((x) => x.technicianID === id);
        return spacePickTechnicianLabel(t);
    });

    readonly spacesPickFilteredList = computed(() => {
        const tid = this.spacePickTechnicianId();
        if (tid == null) {
            return [];
        }
        const tech = this.h().allTechnicians().find((x) => x.technicianID === tid);
        return spacesPickFilteredList(tech, this.h().approvedSpaces(), this.spacePickSearchQuery());
    });

    readonly anyModalOpen = computed(
        () =>
            this.isModalOpen() ||
            this.isEditModalOpen() ||
            this.isTechSearchModalOpen() ||
            this.isSpacePickModalOpen() ||
            this.assignPickSpaceModalOpen() ||
            this.assignPickTechModalOpen() ||
            this.spaceTechModalOpen() ||
            this.assignedSpacesDetailTechId() != null
    );

    readonly selectedSpaceId = computed(() => this.h().selectedSpaceId());

    bindHost(host: HostTechniciansModalHost): void {
        this.host = host;
    }

    private h(): HostTechniciansModalHost {
        if (!this.host) {
            throw new Error('HostTechniciansModalStore: host not bound');
        }
        return this.host;
    }

    pageError(): string {
        return this.h().pageError();
    }

    hasNoApprovedSpaces(): boolean {
        return this.h().hasNoApprovedSpaces();
    }

    hasNoSpaces(): boolean {
        return this.h().hasNoSpaces();
    }

    selectedSpace(): Space | null {
        return this.h().selectedSpace();
    }

    loadingTechs(): boolean {
        return this.h().loadingTechs();
    }

    approvedSpaces(): Space[] {
        return this.h().approvedSpaces();
    }

    allTechnicians(): HostTechnicianDto[] {
        return this.h().allTechnicians();
    }

    loadAllTechniciansFromApi(): void {
        this.loadingAllTechs.set(true);
        this.hostService
            .getAllTechnicians()
            .pipe(finalize(() => this.loadingAllTechs.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    this.h().allTechnicians.set(rows.map((r) => normalizeTechnicianRow(r)));
                },
                error: () => {
                    this.h().allTechnicians.set([]);
                }
            });
    }

    technicianCountForSpace(spaceId: number): number {
        return technicianCountForSpace(this.h().allTechnicians(), spaceId);
    }

    spaceCardAriaLabel(space: Space): string {
        return spaceCardAriaLabel(space, this.technicianCountForSpace(space.spaceID));
    }

    spacesAssignableFor(t: HostTechnicianDto): Space[] {
        return spacesAssignableFor(t, this.h().approvedSpaces());
    }

    assignableSpaceCount(t: HostTechnicianDto): number {
        return assignableSpaceCount(t, this.h().approvedSpaces());
    }

    assignSpacesAssignmentFiltered(): Space[] {
        return filterAssignableSpaces(this.h().approvedSpaces(), this.assignPickSpaceSearch());
    }

    assignTechAssignmentFiltered(): HostTechnicianDto[] {
        return filterTechniciansBySearch(this.h().allTechnicians(), this.assignPickTechSearch());
    }

    assignSpacePickerPrimary(): string {
        return assignSpacePickerPrimary(this.h().selectedSpace());
    }

    assignSpacePickerSecondary(): string {
        return assignSpacePickerSecondary(this.h().selectedSpace());
    }

    assignTechPickerPrimary(): string {
        const id = this.technicianIdToAssign();
        if (id == null) {
            return assignTechPickerPrimary(null);
        }
        const t = this.h().allTechnicians().find((x) => x.technicianID === id);
        return assignTechPickerPrimary(t);
    }

    assignTechPickerSecondary(): string {
        const id = this.technicianIdToAssign();
        if (id == null) {
            return '';
        }
        const t = this.h().allTechnicians().find((x) => x.technicianID === id);
        return assignTechPickerSecondary(t);
    }

    isAssignSpacePickSelected(spaceId: number): boolean {
        return this.h().selectedSpaceId() === spaceId;
    }

    isAssignTechPickSelected(technicianId: number): boolean {
        return this.technicianIdToAssign() === technicianId;
    }

    technicianDisplayCode(t: HostTechnicianDto): string {
        return technicianDisplayCode(t);
    }

    technicianSpecDisplay(t: HostTechnicianDto): string {
        return technicianSpecDisplay(t);
    }

    techInitials(name: string): string {
        return techInitials(name);
    }

    visibleAssignedSpaces(t: HostTechnicianDto): HostTechnicianAssignedSpaceDto[] {
        return visibleAssignedSpaces(t, HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS);
    }

    hiddenAssignedSpacesCount(t: HostTechnicianDto): number {
        return hiddenAssignedSpacesCount(t, HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS);
    }

    createFormState(): HostTechCreateFormState {
        return {
            showErrors: this.createFormShowErrors(),
            apiFieldErrors: this.createApiFieldErrors(),
            nome: this.createNome,
            cognome: this.createCognome,
            email: this.email,
            password: this.password,
            specialization: this.specialization
        };
    }

    createNomeInvalid(): boolean {
        return createNomeInvalid(this.createFormState());
    }

    createCognomeInvalid(): boolean {
        return createCognomeInvalid(this.createFormState());
    }

    createEmailInvalid(): boolean {
        return createEmailInvalid(this.createFormState());
    }

    createPasswordInvalid(): boolean {
        return createPasswordInvalid(this.createFormState());
    }

    createSpecializationInvalid(): boolean {
        return createSpecializationInvalid(this.createFormState());
    }

    createNomeInlineError(): string | null {
        return createNomeInlineError(this.createFormState());
    }

    createCognomeInlineError(): string | null {
        return createCognomeInlineError(this.createFormState());
    }

    createEmailInlineError(): string | null {
        return createEmailInlineError(this.createFormState());
    }

    createPasswordInlineError(): string | null {
        return createPasswordInlineError(this.createFormState());
    }

    createSpecializationInlineError(): string | null {
        return createSpecializationInlineError(this.createFormState());
    }

    clearAssignmentTicketContext(): void {
        this.assignmentTicketContext.set(null);
    }

    applyDeepLinkResult(
        spaceId: number | null,
        ticketContext: HostTechnicianAssignmentTicketContext | null,
        openTechPicker: boolean
    ): void {
        if (spaceId != null) {
            this.h().selectedSpaceId.set(spaceId);
            this.h().loadTechnicians();
        }
        this.assignmentTicketContext.set(ticketContext);
        if (openTechPicker) {
            queueMicrotask(() => {
                if (this.h().selectedSpaceId() != null && this.h().selectedSpaceApproved()) {
                    this.openAssignTechModal();
                }
            });
        }
    }

    openSpaceTechniciansModal(space: Space): void {
        if (!space.approved || this.h().hasNoApprovedSpaces()) {
            return;
        }
        this.h().selectedSpaceId.set(space.spaceID);
        this.spaceModalTechSearch.set('');
        this.h().loadTechnicians();
        this.spaceTechModalOpen.set(true);
    }

    closeSpaceTechniciansModal(): void {
        this.spaceTechModalOpen.set(false);
        this.spaceModalTechSearch.set('');
    }

    openSpacePickModal(t: HostTechnicianDto): void {
        if (this.h().hasNoApprovedSpaces() || this.assignableSpaceCount(t) === 0) {
            return;
        }
        this.spacePickSearchQuery.set('');
        this.spacePickTechnicianId.set(t.technicianID);
        this.isSpacePickModalOpen.set(true);
    }

    closeSpacePickModal(): void {
        if (this.spacePickAssigning()) {
            return;
        }
        this.isSpacePickModalOpen.set(false);
        this.spacePickTechnicianId.set(null);
        this.spacePickSearchQuery.set('');
    }

    openAssignSpaceModal(): void {
        if (this.h().hasNoSpaces() || this.h().hasNoApprovedSpaces()) {
            return;
        }
        this.assignPickSpaceSearch.set('');
        this.assignPickSpaceModalOpen.set(true);
    }

    closeAssignSpaceModal(): void {
        this.assignPickSpaceModalOpen.set(false);
        this.assignPickSpaceSearch.set('');
    }

    pickSpaceForAssignment(space: Space): void {
        if (!space.approved || this.h().hasNoApprovedSpaces()) {
            return;
        }
        const ctx = this.assignmentTicketContext();
        if (ctx && ctx.spaceID !== space.spaceID) {
            this.assignmentTicketContext.set(null);
        }
        this.h().selectedSpaceId.set(space.spaceID);
        this.technicianIdToAssign.set(null);
        this.h().loadTechnicians();
        this.closeAssignSpaceModal();
    }

    openAssignTechModal(): void {
        if (
            this.h().hasNoSpaces() ||
            this.h().hasNoApprovedSpaces() ||
            this.h().selectedSpaceId() == null
        ) {
            return;
        }
        this.assignPickTechSearch.set('');
        this.assignPickTechModalOpen.set(true);
    }

    closeAssignTechModal(): void {
        this.assignPickTechModalOpen.set(false);
        this.assignPickTechSearch.set('');
    }

    pickTechnicianForAssignment(t: HostTechnicianDto): void {
        this.technicianIdToAssign.set(t.technicianID);
        this.closeAssignTechModal();
    }

    assignTechnicianToSpaceFromPick(space: Space): void {
        const tid = this.spacePickTechnicianId();
        if (tid == null) {
            return;
        }
        this.spacePickAssigning.set(true);
        this.h().onPageError('');
        this.hostService
            .assignTechnician(space.spaceID, tid)
            .pipe(finalize(() => this.spacePickAssigning.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.h().onPageError('');
                    this.assignSuccessMsg.set('Assegnamento riuscito.');
                    window.setTimeout(() => this.assignSuccessMsg.set(''), HOST_TECH_ASSIGN_SUCCESS_TOAST_MS);
                    this.isSpacePickModalOpen.set(false);
                    this.spacePickTechnicianId.set(null);
                    this.spacePickSearchQuery.set('');
                    this.h().loadAllTechnicians();
                    this.h().loadTechnicians();
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    unassignFromHostSpace(spaceId: number, technicianId: number): void {
        this.h().onPageError('');
        this.hostService
            .unassignTechnician(spaceId, technicianId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.h().loadAllTechnicians();
                    this.h().loadTechnicians();
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    openModal(): void {
        this.h().onPageError('');
        this.createFormShowErrors.set(false);
        this.createApiFieldErrors.set({});
        this.createNome = '';
        this.createCognome = '';
        this.email = '';
        this.password = '';
        this.specialization = '';
        this.isModalOpen.set(true);
    }

    closeModal(): void {
        this.isModalOpen.set(false);
        this.createFormShowErrors.set(false);
        this.createNome = '';
        this.createCognome = '';
        this.email = '';
        this.password = '';
        this.specialization = '';
    }

    openEditModal(t: HostTechnicianDto): void {
        this.h().onPageError('');
        this.editingTechnicianId = t.technicianID;
        this.editProfileVersion = Number(t.profileVersion ?? 0);
        this.editName = t.name;
        this.editEmail = t.email;
        this.editSpecialization = t.specialization ?? '';
        this.editPassword = '';
        this.isEditModalOpen.set(true);
    }

    closeEditModal(): void {
        this.isEditModalOpen.set(false);
        this.editingTechnicianId = null;
        this.editPassword = '';
        this.editProfileVersion = 0;
    }

    saveEditedTechnician(): void {
        if (this.editingTechnicianId == null) {
            return;
        }
        if (!this.h().allTechnicians().some((x) => x.technicianID === this.editingTechnicianId)) {
            this.h().onPageError('Tecnico non trovato.');
            return;
        }
        const validated = validateEditTechnicianFields(this.editName, this.editEmail, this.editPassword);
        if (!validated.valid) {
            this.h().onPageError(validated.error);
            return;
        }
        this.editSaving.set(true);
        this.h().onPageError('');
        const payload = {
            name: validated.name,
            email: validated.email,
            specialization: this.editSpecialization.trim(),
            profileVersion: this.editProfileVersion,
            ...(validated.password ? { password: validated.password } : {})
        };
        this.hostService
            .updateTechnician(this.editingTechnicianId, payload)
            .pipe(finalize(() => this.editSaving.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.closeEditModal();
                    this.h().loadAllTechnicians();
                    this.h().loadTechnicians();
                },
                error: (err: Error) => {
                    this.h().onPageError(err.message);
                    if (err.message.includes('nel frattempo') || err.message.includes('Ricarica')) {
                        this.h().loadAllTechnicians();
                    }
                }
            });
    }

    confirmDeleteTechnician(t: HostTechnicianDto): void {
        this.confirmService
            .confirm({
                title: 'Elimina tecnico',
                message: `Eliminare definitivamente ${t.name}? Possibile solo senza segnalazioni aperte o in lavorazione.`,
                confirmLabel: 'Elimina',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(
                take(1),
                filter(Boolean),
                switchMap(() => this.hostService.deleteTechnician(t.technicianID)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: () => {
                    this.h().loadAllTechnicians();
                    this.h().loadTechnicians();
                    if (this.technicianIdToAssign() === t.technicianID) {
                        this.technicianIdToAssign.set(null);
                    }
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    openTechSearchModal(): void {
        this.techModalUnifiedSearch.set('');
        this.isTechSearchModalOpen.set(true);
    }

    closeTechSearchModal(): void {
        this.isTechSearchModalOpen.set(false);
        this.techModalUnifiedSearch.set('');
    }

    clearTechModalFilters(): void {
        this.techModalUnifiedSearch.set('');
        this.techFilterNome.set('');
        this.techFilterCognome.set('');
        this.techFilterCodice.set('');
    }

    openAssignedSpacesModal(t: HostTechnicianDto): void {
        this.assignedSpacesDetailTechId.set(t.technicianID);
    }

    closeAssignedSpacesModal(): void {
        this.assignedSpacesDetailTechId.set(null);
    }

    createTechnician(): void {
        this.h().onPageError('');
        this.createApiFieldErrors.set({});
        this.createFormShowErrors.set(true);
        const state = this.createFormState();
        if (!isCreateTechnicianFormValid(state)) {
            return;
        }
        const displayName = buildCreateTechnicianDisplayName(state.nome, state.cognome);
        this.hostService
            .createTechnician({
                name: displayName,
                email: state.email.trim(),
                password: state.password,
                specialization: state.specialization.trim()
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (created) => {
                    this.h().loadAllTechnicians();
                    this.technicianIdToAssign.set(created.technicianID);
                    this.closeModal();
                },
                error: (err: unknown) => {
                    const map = fieldErrorsFromValidationResponse(err);
                    if (map) {
                        this.createApiFieldErrors.set(map);
                        this.createFormShowErrors.set(true);
                        return;
                    }
                    this.h().onPageError(
                        err instanceof Error ? err.message : 'Impossibile creare il tecnico.'
                    );
                }
            });
    }

    assignTechnician(): void {
        this.h().onPageError('');
        const spaceId = this.h().selectedSpaceId();
        const techId = this.technicianIdToAssign();
        if (spaceId == null) {
            this.h().onPageError('Seleziona uno spazio approvato prima di assegnare un tecnico.');
            return;
        }
        if (!this.h().selectedSpaceApproved()) {
            this.h().onPageError(
                "Non puoi assegnare tecnici a uno spazio non ancora approvato dall'amministratore."
            );
            return;
        }
        if (techId == null) {
            this.h().onPageError('Seleziona un tecnico dalla lista.');
            return;
        }
        const ticketCtx = this.assignmentTicketContext();
        this.assignFlowBusy.set(true);
        this.assignSuccessMsg.set('');
        const req$: Observable<void> =
            ticketCtx != null
                ? this.hostService.assignTechnicianToTicket(ticketCtx.ticketId, techId).pipe(map(() => undefined))
                : this.hostService.assignTechnician(spaceId, techId);
        req$
            .pipe(finalize(() => this.assignFlowBusy.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.h().onPageError('');
                    this.assignSuccessMsg.set('Assegnamento riuscito.');
                    window.setTimeout(() => this.assignSuccessMsg.set(''), HOST_TECH_ASSIGN_SUCCESS_TOAST_MS);
                    this.technicianIdToAssign.set(null);
                    this.assignmentTicketContext.set(null);
                    this.h().loadTechnicians();
                    this.h().loadAllTechnicians();
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }

    unassign(technicianId: number): void {
        this.h().onPageError('');
        const sid = this.h().selectedSpaceId();
        if (sid == null) {
            return;
        }
        this.hostService
            .unassignTechnician(sid, technicianId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.h().loadTechnicians();
                    this.h().loadAllTechnicians();
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }
}
