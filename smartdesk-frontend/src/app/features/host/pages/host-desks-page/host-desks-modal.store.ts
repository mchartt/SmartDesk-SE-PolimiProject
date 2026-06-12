import {
    computed,
    DestroyRef,
    effect,
    inject,
    Injectable,
    Injector,
    runInInjectionContext,
    signal,
    WritableSignal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Desk, DeskStateCode, Space } from '../../../../core/models';
import { HostRoom, HostService } from '../../../../core/services/host.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import {
    amenitiesToAddFromPreset,
    AmenityPreset,
    buildHostPresetSummaryText,
    canEditDesk as utilCanEditDesk,
    DeskRoomSection,
    deskCardRoomName as utilDeskCardRoomName,
    deskStateBadgeClass as utilDeskStateBadgeClass,
    deskStateLabel as utilDeskStateLabel,
    filterDesksInSection,
    filterHostCustomPresets,
    filterPresetsForDeskApply,
    findPresetConflict,
    formatDeskTitle as utilFormatDeskTitle,
    HOST_DESKS_AMENITY_TOKEN_MAX,
    HOST_DESKS_LEGACY_LS_CUSTOM_AMENITY_PRESETS,
    HOST_DESKS_PRESET_HINT_MAX,
    HOST_DESKS_PRESET_LABEL_MAX,
    HOST_DESKS_ROOM_CODE_MAX_LEN,
    HOST_DESKS_ROOM_NAME_MAX_LEN,
    HostCustomAmenityPreset,
    isHostPresetFormFilled,
    mapPresetApiError,
    mapPresetFromDto,
    normalizeAmenityToken,
    normalizePresetAmenitiesList,
    parseLegacyPresetPayloads,
    presetFullyApplied as utilPresetFullyApplied,
    previewDeskCodeForNewDesk,
    sectionKey as utilSectionKey,
    sortHostPresetsByLabel,
    validateHostPresetFormFields,
    validateRoomFormFields
} from './host-desks.util';

export {
    HOST_DESKS_AMENITY_TOKEN_MAX,
    HOST_DESKS_ROOM_CODE_MAX_LEN,
    HOST_DESKS_ROOM_NAME_MAX_LEN
};

export interface HostDesksModalHost {
    desks: WritableSignal<Desk[]>;
    rooms: WritableSignal<HostRoom[]>;
    selectedSpaceId: WritableSignal<number | null>;
    selectedSpace: () => Space | null;
    canManageDesks: () => boolean;
    deskSections: () => DeskRoomSection[];
    pageError: () => string;
    onPageError: (message: string) => void;
    reloadSpaceContext: (spaceId: number) => void;
}

@Injectable()
export class HostDesksModalStore {
    private readonly hostService = inject(HostService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly injector = inject(Injector);

    private host: HostDesksModalHost | null = null;
    private effectsBound = false;

    readonly isDeskModalOpen = signal(false);
    readonly isRoomModalOpen = signal(false);
    readonly deskAmenityModalDeskId = signal<number | null>(null);
    readonly deskAmenitySubModal = signal<'active' | 'apply-set' | null>(null);
    readonly hostCustomPresets = signal<HostCustomAmenityPreset[]>([]);
    readonly loadingPresets = signal(false);
    readonly presetsMutating = signal(false);
    readonly presetSectionError = signal('');
    readonly hostPresetLibraryFilter = signal('');
    readonly isHostPresetFormOpen = signal(false);
    readonly newPresetAmenities = signal<string[]>([]);
    readonly hostPresetLibraryModalOpen = signal(false);
    readonly editingHostPresetId = signal<number | null>(null);
    readonly editPresetAmenities = signal<string[]>([]);
    readonly presetApplySearchByDesk = signal<Record<number, string>>({});
    readonly roomBrowseModalKey = signal<string | null>(null);
    readonly roomDeskFilter = signal<Record<string, string>>({});

    editingDeskId: number | null = null;
    deskCode = '';
    deskRoomId: number | null = null;
    deskAmenityDraft = '';
    newHostPresetLabel = '';
    newHostPresetHint = '';
    newPresetAmenityDraft = '';
    editHostPresetLabel = '';
    editHostPresetHint = '';
    editPresetAmenityDraft = '';
    editHostPresetFormError = '';
    hostPresetFormError = '';
    editingRoomId: number | null = null;
    roomFormName = '';
    roomFormCode = '';

    readonly deskAmenityModalDesk = computed(() => {
        const id = this.deskAmenityModalDeskId();
        if (id == null) {
            return null;
        }
        return this.h().desks().find((d) => d.id === id) ?? null;
    });

    readonly browseModalSection = computed(() => {
        const key = this.roomBrowseModalKey();
        if (!key) {
            return null;
        }
        return this.h().deskSections().find((s) => utilSectionKey(s) === key) ?? null;
    });

    readonly sortedHostPresetsForPreview = computed(() => sortHostPresetsByLabel(this.hostCustomPresets()));

    readonly hostPresetSummaryText = computed(() =>
        buildHostPresetSummaryText(this.sortedHostPresetsForPreview())
    );

    readonly filteredHostCustomPresets = computed(() =>
        filterHostCustomPresets(this.hostCustomPresets(), this.hostPresetLibraryFilter())
    );

    readonly previewDeskCode = computed(() =>
        previewDeskCodeForNewDesk(
            this.editingDeskId,
            this.deskRoomId,
            this.h().rooms(),
            this.h().desks()
        )
    );

    readonly rooms = computed(() => this.h().rooms());

    readonly anyModalOpen = computed(
        () =>
            this.isDeskModalOpen() ||
            this.isRoomModalOpen() ||
            this.roomBrowseModalKey() !== null ||
            this.hostPresetLibraryModalOpen() ||
            this.isHostPresetFormOpen()
    );

    bindHost(host: HostDesksModalHost): void {
        this.host = host;
        if (this.effectsBound) {
            return;
        }
        this.effectsBound = true;
        runInInjectionContext(this.injector, () => {
            effect(() => {
                const id = host.selectedSpaceId();
                this.resetOnSpaceChange(id);
                if (id === null) {
                    this.hostCustomPresets.set([]);
                    this.presetSectionError.set('');
                    return;
                }
                this.reloadHostPresetsFromApi(id);
            });
            effect(() => {
                const key = this.roomBrowseModalKey();
                if (key === null) {
                    return;
                }
                if (this.browseModalSection() === null) {
                    this.roomBrowseModalKey.set(null);
                }
            });
            effect(() => {
                if (this.deskAmenityModalDeskId() == null) {
                    return;
                }
                if (this.deskAmenityModalDesk() === null) {
                    this.closeDeskAmenityModal();
                }
            });
        });
    }

    private h(): HostDesksModalHost {
        if (!this.host) {
            throw new Error('HostDesksModalStore: host not bound');
        }
        return this.host;
    }

    pageError(): string {
        return this.h().pageError();
    }

    onEscapeCloseModals(): void {
        if (this.isHostPresetFormOpen()) {
            this.closeHostPresetCreateModal();
        } else if (this.deskAmenitySubModal() != null) {
            this.closeDeskAmenitySubModal();
        } else if (this.deskAmenityModalDeskId() != null) {
            this.closeDeskAmenityModal();
        } else if (this.hostPresetLibraryModalOpen()) {
            this.closeHostPresetLibraryModal();
        } else if (this.roomBrowseModalKey()) {
            this.closeRoomBrowseModal();
        } else if (this.isRoomModalOpen()) {
            this.closeRoomModal();
        } else if (this.isDeskModalOpen()) {
            this.closeDeskModal();
        }
    }

    private resetOnSpaceChange(_spaceId: number | null): void {
        this.roomBrowseModalKey.set(null);
        this.roomDeskFilter.set({});
        this.hostPresetLibraryFilter.set('');
        this.isHostPresetFormOpen.set(false);
        this.presetApplySearchByDesk.set({});
        this.hostPresetLibraryModalOpen.set(false);
        this.cancelEditHostPreset();
        this.clearNewHostPresetForm();
    }

    canEditDesk(state: DeskStateCode): boolean {
        return utilCanEditDesk(state);
    }

    deskStateLabel(stateCode: DeskStateCode | string): string {
        return utilDeskStateLabel(stateCode);
    }

    deskStateBadgeClass(stateCode: DeskStateCode | string): string {
        return utilDeskStateBadgeClass(stateCode);
    }

    sectionKey(section: DeskRoomSection): string {
        return utilSectionKey(section);
    }

    formatDeskTitle(code: string): string {
        return utilFormatDeskTitle(code);
    }

    deskCardRoomName(desk: Desk, sectionRoom: HostRoom | null): string {
        return utilDeskCardRoomName(desk, sectionRoom);
    }

    presetFullyApplied(desk: Desk, preset: AmenityPreset): boolean {
        return utilPresetFullyApplied(desk, preset);
    }

    openDeskModal(desk?: Desk): void {
        this.closeRoomBrowseStack();
        this.closeHostPresetLibraryModal();
        if (!this.h().canManageDesks()) {
            this.h().onPageError(
                'Crea almeno una stanza con il pulsante «Nuova stanza» prima di aggiungere postazioni.'
            );
            return;
        }
        if (desk) {
            this.editingDeskId = desk.id;
            this.deskCode = desk.code;
            this.deskRoomId = desk.roomID ?? this.h().rooms()[0]?.roomID ?? null;
        } else {
            this.clearDeskForm();
            this.deskRoomId = this.h().rooms()[0]?.roomID ?? null;
        }
        this.h().onPageError('');
        this.isDeskModalOpen.set(true);
    }

    closeDeskModal(): void {
        this.isDeskModalOpen.set(false);
        this.clearDeskForm();
    }

    saveDesk(): void {
        this.h().onPageError('');
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId) {
            this.h().onPageError('Seleziona prima uno spazio.');
            return;
        }
        const rid = this.deskRoomId;
        if (rid === null) {
            this.h().onPageError('Seleziona la stanza in cui si trova la postazione.');
            return;
        }
        const existingDesk =
            this.editingDeskId !== null
                ? this.h().desks().find((d) => d.id === this.editingDeskId) ?? null
                : null;
        const payload = {
            roomID: rid,
            code: existingDesk?.code ?? '',
            amenities: existingDesk ? existingDesk.amenities : [],
            spaceID: spaceId
        };
        const req$ =
            this.editingDeskId === null
                ? this.hostService.createDesk(payload)
                : this.hostService.updateDesk(this.editingDeskId, payload);
        req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.closeDeskModal();
                this.h().reloadSpaceContext(spaceId);
            },
            error: (err: Error) => this.h().onPageError(err.message)
        });
    }

    editDesk(desk: Desk): void {
        this.openDeskModal(desk);
    }

    openRoomBrowseModal(section: DeskRoomSection): void {
        this.closeHostPresetLibraryModal();
        const sk = this.sectionKey(section);
        this.roomDeskFilter.update((m) => ({ ...m, [sk]: '' }));
        this.roomBrowseModalKey.set(sk);
    }

    closeRoomBrowseModal(): void {
        this.roomBrowseModalKey.set(null);
        this.presetApplySearchByDesk.set({});
    }

    closeRoomBrowseStack(): void {
        this.closeDeskAmenityModal();
        this.closeRoomBrowseModal();
    }

    openDeskAmenityModal(desk: Desk): void {
        this.deskAmenitySubModal.set(null);
        this.deskAmenityModalDeskId.set(desk.id);
        this.deskAmenityDraft = '';
        this.setPresetApplySearchForDesk(desk.id, '');
        const spaceId = this.h().selectedSpaceId();
        if (spaceId != null) {
            this.reloadHostPresetsFromApi(spaceId);
        }
    }

    openDeskAmenityActiveSubModal(): void {
        this.deskAmenitySubModal.set('active');
    }

    openDeskAmenityApplySetSubModal(): void {
        const desk = this.deskAmenityModalDesk();
        if (desk) {
            this.setPresetApplySearchForDesk(desk.id, '');
        }
        this.deskAmenitySubModal.set('apply-set');
    }

    closeDeskAmenitySubModal(): void {
        this.deskAmenitySubModal.set(null);
    }

    openHostPresetCreateFromDeskAmenityModal(): void {
        this.closeDeskAmenitySubModal();
        this.closeDeskAmenityModal();
        this.closeHostPresetLibraryModal();
        this.openHostPresetCreateModal();
    }

    openPresetHubFromDeskAmenityModal(): void {
        this.closeDeskAmenitySubModal();
        this.closeDeskAmenityModal();
        this.openHostPresetLibraryModal();
    }

    closeDeskAmenityModal(): void {
        this.closeDeskAmenitySubModal();
        this.deskAmenityModalDeskId.set(null);
        this.deskAmenityDraft = '';
    }

    setDeskAmenityDraft(value: string): void {
        this.deskAmenityDraft = normalizeAmenityToken(value);
    }

    roomFilterInput(key: string): string {
        return this.roomDeskFilter()[key] ?? '';
    }

    setRoomFilter(key: string, value: string): void {
        this.roomDeskFilter.update((m) => ({ ...m, [key]: value }));
    }

    filteredDesksInSection(section: DeskRoomSection): Desk[] {
        return filterDesksInSection(section, this.roomDeskFilter()[this.sectionKey(section)] ?? '');
    }

    presetApplySearchForDesk(deskId: number): string {
        return this.presetApplySearchByDesk()[deskId] ?? '';
    }

    setPresetApplySearchForDesk(deskId: number, value: string): void {
        this.presetApplySearchByDesk.update((m) => ({ ...m, [deskId]: value }));
    }

    filteredPresetsForDesk(desk: Desk): AmenityPreset[] {
        return filterPresetsForDeskApply(
            this.hostCustomPresets(),
            this.presetApplySearchByDesk()[desk.id] ?? ''
        );
    }

    openHostPresetCreateModal(): void {
        this.clearNewHostPresetForm();
        this.hostPresetFormError = '';
        this.isHostPresetFormOpen.set(true);
    }

    openHostPresetCreateFromLibrary(): void {
        this.closeHostPresetLibraryModal();
        this.openHostPresetCreateModal();
    }

    closeHostPresetCreateModal(): void {
        this.isHostPresetFormOpen.set(false);
        this.clearNewHostPresetForm();
        this.hostPresetFormError = '';
    }

    setNewPresetAmenityDraft(value: string): void {
        this.newPresetAmenityDraft = normalizeAmenityToken(value);
    }

    addAmenityToNewPresetForm(): void {
        const token = normalizeAmenityToken(this.newPresetAmenityDraft);
        if (!token) {
            this.hostPresetFormError = `Inserisci una dotazione (max ${HOST_DESKS_AMENITY_TOKEN_MAX} caratteri).`;
            return;
        }
        if (this.newPresetAmenities().includes(token)) {
            this.hostPresetFormError = 'Questa dotazione è già nell’elenco.';
            return;
        }
        this.hostPresetFormError = '';
        this.newPresetAmenities.update((arr) => [...arr, token]);
        this.newPresetAmenityDraft = '';
    }

    removeAmenityFromNewPresetForm(token: string): void {
        this.newPresetAmenities.update((arr) => arr.filter((a) => a !== token));
    }

    isNewHostPresetFormValid(): boolean {
        return isHostPresetFormFilled(
            this.newHostPresetLabel,
            this.newHostPresetHint,
            this.newPresetAmenities().length
        );
    }

    isEditHostPresetFormValid(): boolean {
        return isHostPresetFormFilled(
            this.editHostPresetLabel,
            this.editHostPresetHint,
            this.editPresetAmenities().length
        );
    }

    saveNewHostPreset(): void {
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId) {
            return;
        }
        this.hostPresetFormError = '';
        const validated = this.applyHostPresetFormValidation(
            this.newHostPresetLabel,
            this.newHostPresetHint,
            this.newPresetAmenities().length,
            false
        );
        if (!validated) {
            return;
        }
        const { label, hint } = validated;
        const amenitiesNorm = normalizePresetAmenitiesList([...this.newPresetAmenities()]);
        const conflict = findPresetConflict(this.hostCustomPresets(), label, amenitiesNorm);
        if (conflict === 'label') {
            this.hostPresetFormError = 'Esiste già un set con questo nome.';
            return;
        }
        if (conflict === 'amenities') {
            this.hostPresetFormError = 'Esiste già un set con lo stesso elenco di dotazioni.';
            return;
        }
        this.presetsMutating.set(true);
        this.hostService
            .createAmenityPreset(spaceId, { label, hint, amenities: amenitiesNorm })
            .pipe(finalize(() => this.presetsMutating.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.closeHostPresetCreateModal();
                    this.reloadHostPresetsFromApi(spaceId);
                },
                error: (err: Error) => {
                    this.hostPresetFormError = mapPresetApiError(err);
                }
            });
    }

    openHostPresetLibraryModal(): void {
        this.hostPresetLibraryFilter.set('');
        this.cancelEditHostPreset();
        this.hostPresetLibraryModalOpen.set(true);
    }

    openHostPresetLibraryModalEditing(p: HostCustomAmenityPreset): void {
        this.hostPresetLibraryFilter.set('');
        this.hostPresetLibraryModalOpen.set(true);
        this.startEditHostPreset(p);
    }

    closeHostPresetLibraryModal(): void {
        this.hostPresetLibraryModalOpen.set(false);
        this.hostPresetLibraryFilter.set('');
        this.cancelEditHostPreset();
    }

    startEditHostPreset(p: HostCustomAmenityPreset): void {
        this.editingHostPresetId.set(p.id);
        this.editHostPresetLabel = p.label;
        this.editHostPresetHint = p.hint ?? '';
        this.editPresetAmenities.set([...p.amenities]);
        this.editPresetAmenityDraft = '';
        this.editHostPresetFormError = '';
    }

    cancelEditHostPreset(): void {
        this.editingHostPresetId.set(null);
        this.editHostPresetLabel = '';
        this.editHostPresetHint = '';
        this.editPresetAmenities.set([]);
        this.editPresetAmenityDraft = '';
        this.editHostPresetFormError = '';
    }

    setEditPresetAmenityDraft(value: string): void {
        this.editPresetAmenityDraft = normalizeAmenityToken(value);
    }

    addAmenityToEditPresetForm(): void {
        const token = normalizeAmenityToken(this.editPresetAmenityDraft);
        if (!token) {
            this.editHostPresetFormError = `Inserisci una dotazione (max ${HOST_DESKS_AMENITY_TOKEN_MAX} caratteri).`;
            return;
        }
        if (this.editPresetAmenities().includes(token)) {
            this.editHostPresetFormError = 'Questa dotazione è già nell’elenco.';
            return;
        }
        this.editHostPresetFormError = '';
        this.editPresetAmenities.update((arr) => [...arr, token]);
        this.editPresetAmenityDraft = '';
    }

    removeAmenityFromEditPresetForm(token: string): void {
        this.editPresetAmenities.update((arr) => arr.filter((a) => a !== token));
    }

    saveEditHostPreset(): void {
        const spaceId = this.h().selectedSpaceId();
        const id = this.editingHostPresetId();
        if (!spaceId || !id) {
            return;
        }
        this.editHostPresetFormError = '';
        const validated = this.applyHostPresetFormValidation(
            this.editHostPresetLabel,
            this.editHostPresetHint,
            this.editPresetAmenities().length,
            true
        );
        if (!validated) {
            return;
        }
        const { label, hint } = validated;
        const amenitiesNorm = normalizePresetAmenitiesList([...this.editPresetAmenities()]);
        const conflict = findPresetConflict(this.hostCustomPresets(), label, amenitiesNorm, id);
        if (conflict === 'label') {
            this.editHostPresetFormError = 'Esiste già un altro set con questo nome.';
            return;
        }
        if (conflict === 'amenities') {
            this.editHostPresetFormError = 'Esiste già un altro set con lo stesso elenco di dotazioni.';
            return;
        }
        this.presetsMutating.set(true);
        this.hostService
            .updateAmenityPreset(spaceId, id, { label, hint, amenities: amenitiesNorm })
            .pipe(finalize(() => this.presetsMutating.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.cancelEditHostPreset();
                    this.reloadHostPresetsFromApi(spaceId);
                },
                error: (err: Error) => {
                    this.editHostPresetFormError = mapPresetApiError(err);
                }
            });
    }

    deleteHostCustomPreset(preset: HostCustomAmenityPreset): void {
        this.confirmService
            .confirm({
                title: 'Elimina set personalizzato',
                message: `Rimuovere il set «${preset.label}»?`,
                confirmLabel: 'Elimina set',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                const spaceId = this.h().selectedSpaceId();
                if (!spaceId) {
                    return;
                }
                if (this.editingHostPresetId() === preset.id) {
                    this.cancelEditHostPreset();
                }
                this.presetsMutating.set(true);
                this.hostService
                    .deleteAmenityPreset(spaceId, preset.id)
                    .pipe(finalize(() => this.presetsMutating.set(false)), takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                        next: () => this.reloadHostPresetsFromApi(spaceId),
                        error: (err: Error) => this.presetSectionError.set(mapPresetApiError(err))
                    });
            });
    }

    applyAmenityPreset(desk: Desk, preset: AmenityPreset): void {
        const toAdd = amenitiesToAddFromPreset(desk, preset);
        if (!toAdd.length) {
            return;
        }
        this.updateDeskAmenities(desk, [...desk.amenities, ...toAdd]);
    }

    addDeskAmenityFromModal(): void {
        const desk = this.deskAmenityModalDesk();
        if (!desk) {
            return;
        }
        const token = normalizeAmenityToken(this.deskAmenityDraft);
        if (!token) {
            return;
        }
        const deskUpper = new Set(desk.amenities.map((a) => a.toUpperCase()));
        if (deskUpper.has(token)) {
            this.deskAmenityDraft = '';
            return;
        }
        this.updateDeskAmenities(desk, [...desk.amenities, token]);
        this.deskAmenityDraft = '';
    }

    removeAmenity(desk: Desk, amenity: string): void {
        const updatedAmenities = desk.amenities.filter((a) => a !== amenity);
        this.updateDeskAmenities(desk, updatedAmenities);
    }

    deleteDesk(deskId: number): void {
        const target = this.h().desks().find((d) => d.id === deskId);
        const label = target?.code ? `«${this.formatDeskTitle(target.code)}»` : 'questa postazione';
        this.confirmService
            .confirm({
                title: 'Elimina postazione',
                message: `Sei sicuro di voler eliminare ${label}? Le prenotazioni future associate verranno annullate. L’azione non può essere annullata.`,
                confirmLabel: 'Elimina postazione',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((confirmed) => {
                if (!confirmed) {
                    return;
                }
                this.h().onPageError('');
                this.hostService
                    .deleteDesk(deskId)
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                        next: () => {
                            if (this.editingDeskId === deskId) {
                                this.clearDeskForm();
                            }
                            if (this.deskAmenityModalDeskId() === deskId) {
                                this.closeDeskAmenityModal();
                            }
                            const spaceId = this.h().selectedSpaceId();
                            if (spaceId != null) {
                                this.h().reloadSpaceContext(spaceId);
                            } else {
                                this.h().desks.update((current) => current.filter((d) => d.id !== deskId));
                            }
                        },
                        error: (err: Error) => this.h().onPageError(err.message)
                    });
            });
    }

    openRoomModal(room?: HostRoom): void {
        this.closeRoomBrowseStack();
        this.closeHostPresetLibraryModal();
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId || !this.h().selectedSpace()?.approved) {
            return;
        }
        if (room) {
            this.editingRoomId = room.roomID;
            this.roomFormName = room.name;
            this.roomFormCode = room.code;
        } else {
            this.editingRoomId = null;
            this.roomFormName = '';
            this.roomFormCode = '';
        }
        this.h().onPageError('');
        this.isRoomModalOpen.set(true);
    }

    closeRoomModal(): void {
        this.isRoomModalOpen.set(false);
        this.editingRoomId = null;
        this.roomFormName = '';
        this.roomFormCode = '';
    }

    saveRoom(): void {
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId) {
            return;
        }
        const validated = validateRoomFormFields(this.roomFormName, this.roomFormCode, {
            nameMax: HOST_DESKS_ROOM_NAME_MAX_LEN
        });
        if (!validated.valid) {
            this.h().onPageError(validated.error);
            return;
        }
        const { name, code } = validated;
        const req$ =
            this.editingRoomId === null
                ? this.hostService.createRoom(spaceId, { name, code })
                : this.hostService.updateRoom(spaceId, this.editingRoomId, { name, code });
        req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.closeRoomModal();
                this.h().reloadSpaceContext(spaceId);
            },
            error: (err: Error) => this.h().onPageError(err.message)
        });
    }

    deleteRoom(room: HostRoom): void {
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId) {
            return;
        }
        this.confirmService
            .confirm({
                title: 'Elimina stanza',
                message: `Eliminare la stanza “${room.name}” (${room.code})? Solo stanze senza postazioni possono essere rimosse.`,
                confirmLabel: 'Elimina stanza',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                this.hostService
                    .deleteRoom(spaceId, room.roomID)
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                        next: () => this.h().reloadSpaceContext(spaceId),
                        error: (err: Error) => this.h().onPageError(err.message)
                    });
            });
    }

    private clearDeskForm(): void {
        this.editingDeskId = null;
        this.deskCode = '';
        this.deskRoomId = null;
    }

    private clearNewHostPresetForm(): void {
        this.newHostPresetLabel = '';
        this.newHostPresetHint = '';
        this.newPresetAmenities.set([]);
        this.newPresetAmenityDraft = '';
        this.hostPresetFormError = '';
    }

    private applyHostPresetFormValidation(
        labelRaw: string,
        hintRaw: string,
        amenitiesCount: number,
        forEdit: boolean
    ): { label: string; hint: string } | null {
        const result = validateHostPresetFormFields(labelRaw, hintRaw, amenitiesCount, {
            labelMax: HOST_DESKS_PRESET_LABEL_MAX,
            hintMax: HOST_DESKS_PRESET_HINT_MAX
        });
        if (!result.valid) {
            if (forEdit) {
                this.editHostPresetFormError = result.error;
            } else {
                this.hostPresetFormError = result.error;
            }
            return null;
        }
        return { label: result.label, hint: result.hint };
    }

    private reloadHostPresetsFromApi(spaceId: number): void {
        this.loadingPresets.set(true);
        this.presetSectionError.set('');
        this.hostService
            .listAmenityPresets(spaceId)
            .pipe(finalize(() => this.loadingPresets.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    const mapped: HostCustomAmenityPreset[] = [];
                    for (const r of rows) {
                        const m = mapPresetFromDto(r);
                        if (m) {
                            mapped.push(m);
                        }
                    }
                    this.hostCustomPresets.set(sortHostPresetsByLabel(mapped));
                    if (mapped.length === 0) {
                        this.tryMigrateLegacyBrowserPresets(spaceId);
                    }
                },
                error: (err: Error) => {
                    this.hostCustomPresets.set([]);
                    this.presetSectionError.set(err.message || 'Impossibile caricare i set dotazioni.');
                }
            });
    }

    private tryMigrateLegacyBrowserPresets(spaceId: number): void {
        if (this.presetsMutating()) {
            return;
        }
        if (typeof localStorage === 'undefined') {
            return;
        }
        let lsMap: Record<string, unknown>;
        try {
            const raw = localStorage.getItem(HOST_DESKS_LEGACY_LS_CUSTOM_AMENITY_PRESETS);
            if (!raw) {
                return;
            }
            lsMap = JSON.parse(raw) as Record<string, unknown>;
        } catch {
            return;
        }
        const row = lsMap[String(spaceId)];
        const rawList = Array.isArray(row) ? row : [];
        if (!rawList.length) {
            return;
        }
        const payloads = parseLegacyPresetPayloads(rawList);
        if (!payloads.length) {
            return;
        }
        this.presetsMutating.set(true);
        this.runSequentialLegacyCreates(spaceId, payloads, 0, lsMap);
    }

    private runSequentialLegacyCreates(
        spaceId: number,
        payloads: Array<{ label: string; hint?: string; amenities: string[] }>,
        index: number,
        lsMap: Record<string, unknown>
    ): void {
        if (index >= payloads.length) {
            try {
                delete lsMap[String(spaceId)];
                localStorage.setItem(HOST_DESKS_LEGACY_LS_CUSTOM_AMENITY_PRESETS, JSON.stringify(lsMap));
            } catch {
            }
            this.presetsMutating.set(false);
            this.reloadHostPresetsFromApi(spaceId);
            return;
        }
        const body = payloads[index];
        this.hostService
            .createAmenityPreset(spaceId, body)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => this.runSequentialLegacyCreates(spaceId, payloads, index + 1, lsMap),
                error: () => this.runSequentialLegacyCreates(spaceId, payloads, index + 1, lsMap)
            });
    }

    private updateDeskAmenities(desk: Desk, amenities: string[]): void {
        const spaceId = this.h().selectedSpaceId();
        if (!spaceId || desk.roomID == null) {
            return;
        }
        const payload = {
            roomID: desk.roomID,
            code: desk.code,
            amenities,
            spaceID: spaceId
        };
        this.hostService
            .updateDesk(desk.id, payload)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (updatedDesk) => {
                    desk.amenities = updatedDesk.amenities;
                    this.h().desks.update((rows) => [...rows]);
                },
                error: (err: Error) => this.h().onPageError(err.message)
            });
    }
}
