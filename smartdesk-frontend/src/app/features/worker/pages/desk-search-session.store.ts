import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';
import { Desk, SearchCriteria } from '../../../core/models';
import { BookingService, SlotStatus, WorkerSpaceClosure } from '../../../core/services/booking.service';
import {
    SLOT_COUNT,
    SLOT_LABELS,
    mergeAggregateSlotFree,
    rowToFreeMask,
    slotStartLabelsCoveringRange
} from './desk-slot-grid.util';

export type DeskAvailabilityKind = 'available' | 'partial' | 'occupied' | 'maintenance';

export interface DeskViewModel extends Desk {
    isCooldown?: boolean;
    availabilityKind?: DeskAvailabilityKind;
}

export interface DeskSearchSessionHost {
    slotRangeOk(): boolean;
    orderedRange(): [number, number] | null;
    hasCompleteSlotRange(): boolean;
    isSearchSlotUnavailable(idx: number): boolean;
    computeOfficeClosedMask(): boolean[];
    buildDeskSearchCriteria(): SearchCriteria | null;
    reapplySlotRangeAfterOfficeContextChange(): void;
    pruneSelectedAmenityTags(): void;
    onDeskSearchError(message: string): void;
    onDeskSearchToast(showToast: boolean): void;
    rebuildSlotKinds(): void;
    rememberLastSearchSlotRange(): void;
    loadMyBookings(): void;
    markForCheck(): void;
    getLoadDesksParams(): LoadDesksParams;
    onBeforeLoadDesks(): void;
}

export interface LoadAggregateSlotsParams {
    selectedSpaceId: number | null;
    targetDate: string;
}

export interface LoadDesksParams {
    selectedTags: string[];
    includeMaintenance: boolean;
}

@Injectable()
export class DeskSearchSessionStore {
    private readonly bookingService = inject(BookingService);
    private readonly destroyRef = inject(DestroyRef);

    private host: DeskSearchSessionHost | null = null;
    private aggregateSub?: Subscription;
    private aggregateSlotsRequest = 0;
    private refreshSearchAfterSlotsLoad = false;
    private deskSearchSub?: Subscription;
    private deskSearchRequest = 0;

    private readonly _desks = signal<DeskViewModel[]>([]);
    private readonly _officeDesks = signal<Desk[]>([]);
    private readonly _deskSlotMaskById = signal<Map<number, boolean[]>>(new Map());
    private readonly _aggregateSlotFree = signal<boolean[]>([]);
    private readonly _officeClosedSlotMask = signal<boolean[]>(Array(SLOT_COUNT).fill(false));
    private readonly _selectedClosure = signal<WorkerSpaceClosure | null>(null);

    readonly desks = this._desks.asReadonly();
    readonly officeDesks = this._officeDesks.asReadonly();
    readonly deskSlotMaskById = this._deskSlotMaskById.asReadonly();
    readonly aggregateSlotFree = this._aggregateSlotFree.asReadonly();
    readonly officeClosedSlotMask = this._officeClosedSlotMask.asReadonly();
    readonly selectedClosure = this._selectedClosure.asReadonly();

    readonly slotsLoading = signal(false);
    readonly deskResultsLoading = signal(false);
    readonly availabilityShown = signal(false);
    readonly searchOutdated = signal(false);
    readonly waitlistDeskId = signal<number | null>(null);
    readonly queueJoined = signal(false);

    bindHost(host: DeskSearchSessionHost): void {
        this.host = host;
    }

    private h(): DeskSearchSessionHost {
        if (!this.host) {
            throw new Error('DeskSearchSessionStore: host not bound');
        }
        return this.host;
    }

    setDesks(desks: DeskViewModel[]): void {
        this._desks.set(desks);
    }

    clearDesks(): void {
        this._desks.set([]);
    }

    setSelectedClosure(closure: WorkerSpaceClosure | null): void {
        this._selectedClosure.set(closure);
    }

    setOfficeClosedSlotMask(mask: boolean[]): void {
        this._officeClosedSlotMask.set(mask);
    }

    setDeskSlotMask(deskId: number, mask: boolean[]): void {
        const next = new Map(this._deskSlotMaskById());
        next.set(deskId, mask);
        this._deskSlotMaskById.set(next);
    }

    clearDeskSlotMasks(): void {
        this._deskSlotMaskById.set(new Map());
    }

    resetOfficeContext(): void {
        this._officeDesks.set([]);
        this.clearDeskSlotMasks();
        this._aggregateSlotFree.set([]);
        this._officeClosedSlotMask.set(Array(SLOT_COUNT).fill(false));
        this._selectedClosure.set(null);
    }

    resetSessionFlags(): void {
        this.refreshSearchAfterSlotsLoad = false;
        this.queueJoined.set(false);
        this.availabilityShown.set(false);
        this.searchOutdated.set(false);
    }

    resetBookingData(): void {
        this.clearDesks();
        this.resetOfficeContext();
        this.waitlistDeskId.set(null);
    }

    setRefreshSearchAfterSlotsLoad(value: boolean): void {
        this.refreshSearchAfterSlotsLoad = value;
    }

    isRefreshSearchAfterSlotsLoad(): boolean {
        return this.refreshSearchAfterSlotsLoad;
    }

    fetchDeskSearchResults(showToast: boolean): void {
        const criteria = this.h().buildDeskSearchCriteria();
        if (!criteria) {
            return;
        }
        const requestId = ++this.deskSearchRequest;
        this.deskResultsLoading.set(true);
        this.deskSearchSub?.unsubscribe();
        this.deskSearchSub = this.bookingService
            .searchDesks(criteria)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rows) => {
                    if (requestId !== this.deskSearchRequest) {
                        return;
                    }
                    if (this._officeDesks().length > 0) {
                        this.loadDesks(this.h().getLoadDesksParams());
                    }
                    else {
                        this.applyDeskSearchResultsFromApi(rows);
                    }
                    this.deskResultsLoading.set(false);
                    if (showToast) {
                        this.h().onDeskSearchToast(showToast);
                    }
                    this.h().markForCheck();
                },
                error: (err: Error) => {
                    if (requestId !== this.deskSearchRequest) {
                        return;
                    }
                    this.deskResultsLoading.set(false);
                    this.h().onDeskSearchError(err.message || 'Impossibile caricare le postazioni.');
                    this.clearDesks();
                    this.h().markForCheck();
                }
            });
    }

    applyDeskSearchResultsFromApi(rows: Desk[]): void {
        this._desks.set(rows.map((desk) => {
            let availabilityKind: DeskAvailabilityKind = 'available';
            if (desk.state.code === 'MAINTENANCE') {
                availabilityKind = 'maintenance';
            }
            else if (!desk.isBookable()) {
                availabilityKind = 'occupied';
            }
            return Object.assign(desk, { isCooldown: false, availabilityKind }) as DeskViewModel;
        }));
    }

    runAvailabilitySearch(showToast: boolean): void {
        this.availabilityShown.set(true);
        this.searchOutdated.set(false);
        this.h().rememberLastSearchSlotRange();
        this.h().rebuildSlotKinds();
        this.h().loadMyBookings();
        this.deskSearchSub?.unsubscribe();
        this.deskResultsLoading.set(false);
        if (this.slotsLoading() || this.aggregateSlotMasksPending()) {
            this.refreshSearchAfterSlotsLoad = true;
            this.h().markForCheck();
            return;
        }
        this.loadDesks(this.h().getLoadDesksParams());
        if (showToast) {
            this.h().onDeskSearchToast(showToast);
        }
        this.h().markForCheck();
    }

    completeAggregateSlotsLoad(): void {
        const refineFromSlots = this.refreshSearchAfterSlotsLoad;
        this.h().reapplySlotRangeAfterOfficeContextChange();
        this.h().pruneSelectedAmenityTags();
        this.refreshSearchAfterSlotsLoad = false;
        if (this.availabilityShown() && refineFromSlots && !this.searchOutdated()) {
            this.loadDesks(this.h().getLoadDesksParams());
            this.searchOutdated.set(false);
        }
        this.h().markForCheck();
    }

    loadAggregateSlots(params: LoadAggregateSlotsParams): void {
        this.aggregateSub?.unsubscribe();
        if (params.selectedSpaceId === null) {
            this._aggregateSlotFree.set([]);
            this._officeClosedSlotMask.set(Array(SLOT_COUNT).fill(false));
            this._selectedClosure.set(null);
            this.clearDeskSlotMasks();
            this.slotsLoading.set(false);
            return;
        }
        const requestId = ++this.aggregateSlotsRequest;
        this.slotsLoading.set(true);
        this.clearDeskSlotMasks();
        this.aggregateSub = this.bookingService
            .listDesksInSpace(params.selectedSpaceId)
            .pipe(switchMap((desks) => {
                const closure$ = this.bookingService
                    .getSpaceClosure(params.selectedSpaceId!, params.targetDate)
                    .pipe(catchError(() => of(null)));
                if (!desks.length) {
                    return forkJoin({ closure: closure$, matrix: of([] as SlotStatus[][]) }).pipe(map(({ closure, matrix }) => ({ desks, matrix, closure })));
                }
                const matrix$ = forkJoin(desks.map((d) => this.bookingService.getSlotAvailability(d.id, params.targetDate).pipe(catchError(() => of([])))));
                return forkJoin({ closure: closure$, matrix: matrix$ }).pipe(map(({ closure, matrix }) => ({ desks, matrix, closure })));
            }), finalize(() => {
                if (requestId === this.aggregateSlotsRequest) {
                    this.slotsLoading.set(false);
                }
            }), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: ({ desks, matrix, closure }) => {
                    if (requestId !== this.aggregateSlotsRequest) {
                        return;
                    }
                    this._officeDesks.set(desks);
                    const wait = desks.find((d) => d.state.code !== 'MAINTENANCE')?.id ?? (desks.length ? desks[0].id : null);
                    this.waitlistDeskId.set(wait);
                    this._selectedClosure.set(closure);
                    this._officeClosedSlotMask.set(this.h().computeOfficeClosedMask());
                    this.clearDeskSlotMasks();
                    const maskMap = new Map<number, boolean[]>();
                    desks.forEach((desk, index) => {
                        maskMap.set(desk.id, rowToFreeMask(matrix[index] ?? []));
                    });
                    this._deskSlotMaskById.set(maskMap);
                    const candidateRows = desks
                        .filter((d) => d.state.code !== 'MAINTENANCE')
                        .map((d) => matrix[desks.indexOf(d)] ?? []);
                    this._aggregateSlotFree.set(candidateRows.length
                        ? mergeAggregateSlotFree(candidateRows)
                        : Array(SLOT_COUNT).fill(false));
                    this.completeAggregateSlotsLoad();
                    this.h().markForCheck();
                },
                error: () => {
                    if (requestId !== this.aggregateSlotsRequest) {
                        return;
                    }
                    this.clearDeskSlotMasks();
                    this._aggregateSlotFree.set(Array(SLOT_COUNT).fill(false));
                    this._selectedClosure.set(null);
                    this.h().rebuildSlotKinds();
                    this.refreshSearchAfterSlotsLoad = false;
                    if (this.availabilityShown()) {
                        this.refreshDesksIfReady(this.h().getLoadDesksParams());
                    }
                    this.h().markForCheck();
                }
            });
    }

    refreshDesksIfReady(params: LoadDesksParams): void {
        if (this.searchOutdated()) {
            return;
        }
        if (!this.availabilityShown()) {
            this.clearDesks();
            this.h().markForCheck();
            return;
        }
        if (this.slotsLoading() || this.aggregateSlotMasksPending()) {
            return;
        }
        if (!this.h().hasCompleteSlotRange()) {
            this.clearDesks();
            return;
        }
        if (this.h().slotRangeOk()) {
            this.loadDesks(params);
        }
    }

    aggregateSlotMasksPending(): boolean {
        return this._officeDesks().length > 0 && this._deskSlotMaskById().size === 0;
    }

    loadDesks(params: LoadDesksParams): void {
        if (this.searchOutdated()) {
            return;
        }
        if (!this.h().slotRangeOk()) {
            return;
        }
        const span = this.h().orderedRange();
        if (!span) {
            this.clearDesks();
            return;
        }
        this.h().onBeforeLoadDesks();
        const [from, to] = span;
        const required = params.selectedTags;
        const visible = this._officeDesks().filter((desk) => {
            if (!params.includeMaintenance && desk.state.code === 'MAINTENANCE') {
                return false;
            }
            if (!required.length) {
                return true;
            }
            return required.every((tag) => desk.amenities.includes(tag));
        });
        this._desks.set(visible.map((desk) => this.buildDeskViewModel(desk, from, to)));
        this.h().markForCheck();
    }

    buildDeskViewModel(desk: Desk, from: number, to: number): DeskViewModel {
        const mask = this._deskSlotMaskById().get(desk.id) ?? Array(SLOT_COUNT).fill(false);
        const availabilityKind = this.computeDeskAvailabilityKind(desk, mask, from, to);
        return Object.assign(desk, { isCooldown: false, availabilityKind }) as DeskViewModel;
    }

    applyLocalBookingToDeskMask(deskId: number, range: {
        start: string;
        end: string;
    }): void {
        const existing = this._deskSlotMaskById().get(deskId);
        const next = existing ? [...existing] : Array(SLOT_COUNT).fill(true);
        for (const label of slotStartLabelsCoveringRange(range.start, range.end)) {
            const idx = SLOT_LABELS.indexOf(label);
            if (idx >= 0) {
                next[idx] = false;
            }
        }
        this.setDeskSlotMask(deskId, next);
    }

    refreshDeskCardAvailability(deskId: number): void {
        const span = this.h().orderedRange();
        if (!span || !this.availabilityShown()) {
            return;
        }
        const [from, to] = span;
        const base = this._officeDesks().find((d) => d.id === deskId);
        if (!base) {
            return;
        }
        const vm = this.buildDeskViewModel(base, from, to);
        const desks = this._desks();
        const idx = desks.findIndex((d) => d.id === deskId);
        if (idx >= 0) {
            this._desks.set(desks.map((d, i) => (i === idx ? vm : d)));
        }
        this.h().markForCheck();
    }

    computeDeskAvailabilityKind(desk: Desk, mask: boolean[], from: number, to: number): DeskAvailabilityKind {
        if (desk.state.code === 'MAINTENANCE') {
            return 'maintenance';
        }
        let freeCount = 0;
        let openSlotsInRange = 0;
        for (let i = from; i <= to; i++) {
            if (this.h().isSearchSlotUnavailable(i)) {
                continue;
            }
            openSlotsInRange++;
            if (mask[i]) {
                freeCount++;
            }
        }
        if (openSlotsInRange === 0) {
            return 'occupied';
        }
        if (freeCount === openSlotsInRange && desk.isBookable()) {
            return 'available';
        }
        if (freeCount > 0) {
            return 'partial';
        }
        return 'occupied';
    }
}
