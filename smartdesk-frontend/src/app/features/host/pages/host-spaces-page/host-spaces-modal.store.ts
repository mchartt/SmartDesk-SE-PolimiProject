import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, Observable } from 'rxjs';
import { Space } from '../../../../core/models';
import { HostService, HostSpaceClosureDto, HostSpaceUpsertPayload } from '../../../../core/services/host.service';
import { ToastService } from '../../../../core/services/toast.service';
import { localCalendarDateIso } from '../../../../core/utils/date.util';
import {
    applyClosureScheduleRangeSelection,
    buildClosureCalendarRows,
    buildClosureDisplayGroups,
    buildDefaultOpeningHoursRows,
    buildOpeningHoursPayload,
    buildPendingClosureDisplayGroups,
    closureExistingIsoSet,
    mergeOpeningHoursFromSpace,
    weeklyClosedJsDowSet,
    type ClosureCalCell,
    type ClosureUiGroup,
    type OpeningHoursRow,
    type PendingClosureUiGroup
} from './host-spaces.util';

export const HOST_SPACE_NAME_MAX_LEN = 50;
export const HOST_SPACE_DESCRIPTION_MAX_LEN = 500;
export const HOST_CLOSURE_REASON_REQUIRED_MSG =
    'Inserisci la motivazione: è obbligatoria e verrà mostrata ai lavoratori.';

export interface HostSpacesModalHost {
    onError(message: string): void;
    onSuccess(message: string): void;
    onSpacesListReload(): void;
    validateCitySelection(cityTrim: string): boolean;
    confirmRemoveClosures(message: string): Observable<boolean>;
}

export type SaveSpaceResult =
    | { ok: true }
    | { ok: false; error: string };

@Injectable()
export class HostSpacesModalStore {
    private readonly hostService = inject(HostService);
    private readonly toast = inject(ToastService);
    private readonly destroyRef = inject(DestroyRef);

    private host: HostSpacesModalHost | null = null;

    readonly isModalOpen = signal(false);
    readonly closureCalendarYear = signal(new Date().getFullYear());
    readonly closureCalendarMonth = signal(new Date().getMonth() + 1);
    readonly openingHoursVersion = signal(0);
    readonly closureRows = signal<HostSpaceClosureDto[]>([]);
    readonly closurePendingDates = signal<string[]>([]);
    readonly closureSaving = signal(false);
    readonly closureRevoking = signal(false);
    readonly closureWorkflow = signal<'schedule' | 'revoke'>('schedule');
    readonly closureFirstPick = signal<string | null>(null);
    readonly closureRevokeBand = signal<{ start: string; end: string } | null>(null);
    readonly closureMonthPanelOpen = signal(false);
    readonly closureYearPanelOpen = signal(false);

    editingId: number | null = null;
    name = '';
    address = '';
    city = '';
    description = '';
    openingHoursRows: OpeningHoursRow[] = [];
    hoursQuickAction: 'weekdays' | 'allDays' | null = null;
    closureReason = '';
    closureFormError = '';
    closureReasonInvalid = false;
    modalCityBaseline = '';
    private modalInitialSnapshot = '';

    readonly closureCalendarRows = computed(() => {
        this.openingHoursVersion();
        this.closureCalendarYear();
        this.closureCalendarMonth();
        this.closurePendingDates();
        this.closureRows();
        this.closureWorkflow();
        this.closureFirstPick();
        this.closureRevokeBand();
        return buildClosureCalendarRows({
            year: this.closureCalendarYear(),
            month: this.closureCalendarMonth(),
            pendingDates: this.closurePendingDates(),
            closureRows: this.closureRows(),
            workflow: this.closureWorkflow(),
            firstPick: this.closureFirstPick(),
            revokeBand: this.closureRevokeBand(),
            openingHoursRows: this.openingHoursRows
        });
    });

    readonly closureDisplayGroups = computed(() => {
        this.closureRows();
        return buildClosureDisplayGroups(this.closureRows());
    });

    readonly closurePendingDisplayGroups = computed(() => {
        this.closurePendingDates();
        return buildPendingClosureDisplayGroups(this.closurePendingDates());
    });

    bindHost(host: HostSpacesModalHost): void {
        this.host = host;
    }

    private h(): HostSpacesModalHost {
        if (!this.host) {
            throw new Error('HostSpacesModalStore: host not bound');
        }
        return this.host;
    }

    isDirty(): boolean {
        return this.isModalOpen() && this.modalFormSnapshot() !== this.modalInitialSnapshot;
    }

    openForCreate(): void {
        this.closeClosureCalMenus();
        this.clearForm();
        this.openingHoursRows = buildDefaultOpeningHoursRows();
        this.hoursQuickAction = null;
        this.isModalOpen.set(true);
        this.touchOpeningHoursCalendar();
        this.modalInitialSnapshot = this.modalFormSnapshot();
    }

    openForEdit(space: Space): void {
        this.closeClosureCalMenus();
        this.editingId = space.spaceID;
        this.name = space.name;
        this.description = space.description;
        this.city = space.city;
        this.address = space.address;
        this.modalCityBaseline = space.city.trim();
        this.openingHoursRows = mergeOpeningHoursFromSpace(space);
        this.hoursQuickAction = null;
        this.isModalOpen.set(true);
        this.resetClosureCalendarToMonthContainingToday();
        this.closurePendingDates.set([]);
        this.closureReason = '';
        this.closureFormError = '';
        this.closureReasonInvalid = false;
        this.resetClosureCalPickers();
        this.touchOpeningHoursCalendar();
        this.loadClosures(space.spaceID);
        this.modalInitialSnapshot = this.modalFormSnapshot();
    }

    close(): void {
        this.closeClosureCalMenus();
        this.isModalOpen.set(false);
        this.clearForm();
    }

    clearForm(): void {
        this.editingId = null;
        this.name = '';
        this.address = '';
        this.city = '';
        this.description = '';
        this.openingHoursRows = [];
        this.hoursQuickAction = null;
        this.modalCityBaseline = '';
        this.modalInitialSnapshot = '';
        this.closureRows.set([]);
        this.closurePendingDates.set([]);
        this.closureReason = '';
        this.closureFormError = '';
        this.closureReasonInvalid = false;
        this.resetClosureCalPickers();
        this.resetClosureCalendarToMonthContainingToday();
        this.touchOpeningHoursCalendar();
    }

    saveSpace(): SaveSpaceResult {
        const payloadBase = {
            name: this.name.trim(),
            address: this.address.trim(),
            city: this.city.trim(),
            description: this.description.trim()
        };
        if (!payloadBase.name || !payloadBase.address || !payloadBase.city) {
            return { ok: false, error: 'Nome, indirizzo e città sono obbligatori.' };
        }
        if (payloadBase.name.length > HOST_SPACE_NAME_MAX_LEN) {
            return { ok: false, error: `Il nome dello spazio non può superare i ${HOST_SPACE_NAME_MAX_LEN} caratteri.` };
        }
        if (payloadBase.description.length > HOST_SPACE_DESCRIPTION_MAX_LEN) {
            return {
                ok: false,
                error: `La descrizione non può superare i ${HOST_SPACE_DESCRIPTION_MAX_LEN} caratteri.`
            };
        }
        const hoursResult = buildOpeningHoursPayload(this.openingHoursRows);
        if (!hoursResult.ok) {
            return { ok: false, error: hoursResult.error };
        }
        if (!this.h().validateCitySelection(payloadBase.city)) {
            return { ok: false, error: '' };
        }
        const payload: HostSpaceUpsertPayload = {
            ...payloadBase,
            city: this.city.trim(),
            openingHours: hoursResult.openingHours
        };
        const isCreate = this.editingId === null;
        const spaceId = this.editingId;
        const req$ = isCreate ? this.hostService.createSpace(payload) : this.hostService.updateSpace(spaceId!, payload);
        req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.close();
                this.h().onSuccess(
                    isCreate
                        ? 'La richiesta è stata inviata all’amministratore ed è in attesa di approvazione.'
                        : 'Spazio aggiornato con successo.'
                );
                this.h().onSpacesListReload();
            },
            error: (err: Error) => this.h().onError(err.message)
        });
        return { ok: true };
    }

    onClosureReasonChange(value: string): void {
        this.closureReason = value;
        if (this.isClosureReasonEmpty(value)) {
            if (this.closureReasonInvalid) {
                this.closureFormError = HOST_CLOSURE_REASON_REQUIRED_MSG;
            }
            return;
        }
        this.closureReasonInvalid = false;
        if (this.closureFormError === HOST_CLOSURE_REASON_REQUIRED_MSG) {
            this.closureFormError = '';
        }
    }

    setClosureWorkflow(mode: 'schedule' | 'revoke'): void {
        this.closureWorkflow.set(mode);
        this.closureFirstPick.set(null);
        this.closureRevokeBand.set(null);
        this.closureFormError = '';
        this.closureReasonInvalid = false;
        this.h().onError('');
    }

    clearClosureCalPickers(): void {
        this.closureFirstPick.set(null);
        this.closureRevokeBand.set(null);
    }

    resetClosureCalPickers(): void {
        this.closureWorkflow.set('schedule');
        this.clearClosureCalPickers();
    }

    onClosureCalendarDayClick(cell: ClosureCalCell): void {
        this.closeClosureCalMenus();
        if (!cell.inMonth) {
            return;
        }
        const iso = cell.iso;
        if (this.closureWorkflow() === 'schedule') {
            if (cell.isPast || cell.isExisting) {
                return;
            }
            if (cell.isWeeklyRecurringClosed && !cell.isPending) {
                return;
            }
            const first = this.closureFirstPick();
            if (!first) {
                this.closureFirstPick.set(iso);
                this.h().onError('');
                return;
            }
            const lo = first < iso ? first : iso;
            const hi = first < iso ? iso : first;
            const existing = closureExistingIsoSet(this.closureRows());
            const today = localCalendarDateIso();
            const weekly = weeklyClosedJsDowSet(this.openingHoursRows);
            this.closurePendingDates.update((prev) =>
                applyClosureScheduleRangeSelection(prev, lo, hi, today, existing, weekly)
            );
            this.closureFirstPick.set(null);
            this.h().onError('');
            return;
        }
        const band = this.closureRevokeBand();
        if (band !== null) {
            if (iso >= band.start && iso <= band.end) {
                this.closureRevokeBand.set(null);
                this.closureFirstPick.set(null);
                this.h().onError('');
                return;
            }
            this.closureRevokeBand.set(null);
            this.closureFirstPick.set(iso);
            this.h().onError('');
            return;
        }
        const firstR = this.closureFirstPick();
        if (firstR === iso) {
            this.closureFirstPick.set(null);
            this.h().onError('');
            return;
        }
        if (firstR) {
            const lo = firstR < iso ? firstR : iso;
            const hi = firstR < iso ? iso : firstR;
            this.closureRevokeBand.set({ start: lo, end: hi });
            this.closureFirstPick.set(null);
            this.h().onError('');
            return;
        }
        this.closureFirstPick.set(iso);
        this.closureRevokeBand.set(null);
        this.h().onError('');
    }

    removeClosurePendingGroup(group: PendingClosureUiGroup): void {
        const dropKeys = new Set(group.dates.map((d) => d.trim().slice(0, 10)));
        this.closurePendingDates.update((arr) => arr.filter((x) => !dropKeys.has(x.trim().slice(0, 10))));
    }

    submitClosures(): void {
        const spaceId = this.editingId;
        if (spaceId === null) {
            return;
        }
        const dates = [...this.closurePendingDates()];
        if (!dates.length) {
            this.closureFormError = 'Seleziona almeno un giorno nel calendario.';
            this.closureReasonInvalid = false;
            return;
        }
        if (this.isClosureReasonEmpty()) {
            this.closureFormError = HOST_CLOSURE_REASON_REQUIRED_MSG;
            this.closureReasonInvalid = true;
            return;
        }
        const reason = this.closureReason.trim();
        this.closureFormError = '';
        this.closureReasonInvalid = false;
        this.closureSaving.set(true);
        this.hostService
            .createSpaceClosures(spaceId, { dates, reason })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (created) => {
                this.closureSaving.set(false);
                this.closurePendingDates.set([]);
                this.closureReason = '';
                this.clearClosureCalPickers();
                this.h().onSuccess(
                    created.length > 0
                        ? `Registrate ${created.length} chiusura/e. Le prenotazioni nei giorni indicati sono state annullate e i lavoratori notificati.`
                        : 'Nessuna nuova chiusura (date già registrate). Eventuali prenotazioni attive su quei giorni sono state comunque annullate.'
                );
                this.toast.success(created.length > 0 ? `Registrate ${created.length} chiusura/e.` : 'Chiusure aggiornate.');
                this.loadClosures(spaceId);
                this.h().onSpacesListReload();
            },
            error: (err: Error) => {
                this.closureSaving.set(false);
                this.closureFormError = err.message;
                this.toast.error(err.message);
            }
        });
    }

    submitRevokeClosures(): void {
        const spaceId = this.editingId;
        const band = this.closureRevokeBand();
        if (spaceId === null || !band) {
            return;
        }
        const rows = this.closureRows().filter((r) => {
            const k = String(r.closedDate ?? '').trim().slice(0, 10);
            if (!/^\d{4}-\d{2}-\d{2}$/.test(k)) {
                return false;
            }
            return k >= band.start && k <= band.end;
        });
        const revokeIds = [...new Set(rows.map((r) => r.id))];
        if (!revokeIds.length) {
            this.h().onError('Nessuna chiusura registrata in questo intervallo.');
            return;
        }
        this.h().onError('');
        this.closureRevoking.set(true);
        this.h().onSuccess('');
        forkJoin(revokeIds.map((id) => this.hostService.deleteSpaceClosure(spaceId, id)))
            .pipe(finalize(() => this.closureRevoking.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.closureRevokeBand.set(null);
                this.closureFirstPick.set(null);
                this.h().onSuccess('');
                this.loadClosures(spaceId);
                this.h().onSpacesListReload();
            },
            error: (err: Error) => this.h().onError(err.message)
        });
    }

    removeClosureGroup(group: ClosureUiGroup, rangeLabel: string): void {
        const spaceId = this.editingId;
        if (spaceId === null || !group.ids.length) {
            return;
        }
        const n = group.ids.length;
        const message =
            n === 1
                ? `Rimuovere la chiusura del ${rangeLabel}? Non riattiva le prenotazioni già annullate.`
                : `Rimuovere le ${n} chiusure nell'intervallo ${rangeLabel}? Non riattiva le prenotazioni già annullate.`;
        this.h()
            .confirmRemoveClosures(message)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
            if (!ok) {
                return;
            }
            const ids = [...new Set(group.ids)];
            if (!ids.length) {
                return;
            }
            forkJoin(ids.map((id) => this.hostService.deleteSpaceClosure(spaceId, id)))
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.h().onSuccess('');
                    this.loadClosures(spaceId);
                    this.h().onSpacesListReload();
                },
                error: (err: Error) => this.h().onError(err.message)
            });
        });
    }

    pickClosureMonth(value: number): void {
        this.closureCalendarMonth.set(value);
        this.closureMonthPanelOpen.set(false);
    }

    pickClosureYear(value: number): void {
        this.closureCalendarYear.set(value);
        this.closureYearPanelOpen.set(false);
    }

    toggleClosureMonthMenu(): void {
        this.closureYearPanelOpen.set(false);
        this.closureMonthPanelOpen.update((v) => !v);
    }

    toggleClosureYearMenu(): void {
        this.closureMonthPanelOpen.set(false);
        this.closureYearPanelOpen.update((v) => !v);
    }

    closeClosureCalMenus(): void {
        this.closureMonthPanelOpen.set(false);
        this.closureYearPanelOpen.set(false);
    }

    touchOpeningHoursCalendar(): void {
        this.openingHoursVersion.update((v) => v + 1);
    }

    matchesEditingSpace(spaceId: number): boolean {
        return this.editingId === spaceId;
    }

    private loadClosures(spaceId: number): void {
        this.hostService
            .getSpaceClosures(spaceId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => this.closureRows.set(rows),
            error: (err: Error) => {
                this.closureRows.set([]);
                this.h().onError(err.message);
            }
        });
    }

    private resetClosureCalendarToMonthContainingToday(): void {
        const t = new Date();
        this.closureCalendarYear.set(t.getFullYear());
        this.closureCalendarMonth.set(t.getMonth() + 1);
    }

    private isClosureReasonEmpty(value?: string): boolean {
        return !(value ?? this.closureReason ?? '').trim();
    }

    private modalFormSnapshot(): string {
        return JSON.stringify({
            editingId: this.editingId,
            name: this.name.trim(),
            address: this.address.trim(),
            city: this.city.trim(),
            description: this.description.trim(),
            openingHoursRows: this.openingHoursRows.map((row) => ({
                dayKey: row.dayKey,
                closed: row.closed,
                open: row.open,
                close: row.close
            })),
            closureReason: this.closureReason.trim(),
            closurePendingDates: this.closurePendingDates()
        });
    }
}
