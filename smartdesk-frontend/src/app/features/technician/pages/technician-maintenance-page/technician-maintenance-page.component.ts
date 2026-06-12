import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { TechnicianService } from '../../../../core/services/technician.service';
import { Space, Desk } from '../../../../core/models';
@Component({
    standalone: true,
    imports: [CommonModule, EmptyStateComponent, SdIconComponent, MdbRippleModule],
    templateUrl: './technician-maintenance-page.component.html'
})
export class TechnicianMaintenancePageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);
    private readonly technicianService = inject(TechnicianService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Manutenzione');
    protected spaces = signal<Space[]>([]);
    protected desks = signal<Desk[]>([]);
    protected selectedSpaceId = signal<number | null>(null);
    protected selectedRoomId = signal<number | null>(null);
    protected isRoomDropdownOpen = signal<boolean>(true);
    protected deskSearch = signal('');
    protected bulkSuspendedDeskIds = signal<number[]>([]);
    protected readonly selectedSpace = computed(() => {
        const id = this.selectedSpaceId();
        return this.spaces().find((space) => space.spaceID === id) || null;
    });
    protected readonly rooms = computed(() => {
        const allDesks = this.desks();
        const roomMap = new Map<number, {
            id: number;
            name: string;
            code: string;
        }>();
        for (const d of allDesks) {
            if (d.roomID) {
                roomMap.set(d.roomID, { id: d.roomID, name: d.roomName || 'Senza nome', code: d.roomCode || '' });
            }
        }
        return Array.from(roomMap.values()).sort((a, b) => a.name.localeCompare(b.name));
    });
    protected readonly roomDesks = computed(() => {
        const roomId = this.selectedRoomId();
        const desks = this.desks();
        if (roomId) {
            return desks.filter(d => d.roomID === roomId);
        }
        return desks;
    });
    protected readonly filteredDesks = computed(() => {
        const query = this.deskSearch().trim().toLowerCase();
        const desks = this.roomDesks();
        if (!query) {
            return desks;
        }
        return desks.filter((desk) => this.matchesSearch(desk, query));
    });
    protected readonly maintenanceDeskCount = computed(() => this.roomDesks().filter((desk) => desk.state?.code?.toUpperCase() === 'MAINTENANCE').length);
    protected readonly availableDeskCount = computed(() => this.roomDesks().filter((desk) => desk.state?.code?.toUpperCase() === 'AVAILABLE').length);
    protected loadingSpaces = signal(true);
    protected loadingDesks = signal(false);
    protected isSubmitting = signal(false);
    protected errorMsg = '';
    protected successMsg = '';
    public ngOnInit(): void {
        this.loadSpaces();
    }
    protected loadSpaces(): void {
        this.loadingSpaces.set(true);
        this.errorMsg = '';
        this.technicianService
            .getAssignedSpaces()
            .pipe(catchError(() => of([])), map((rows) => rows.map((r) => new Space(r.spaceID, '', 0, r.name ?? '', '', '', true, [], r.officeCode ?? '', null))), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (spaces) => {
                this.spaces.set(spaces);
                this.loadingSpaces.set(false);
                if (spaces.length > 0) {
                    this.selectedSpaceId.set(spaces[0].spaceID);
                    this.loadDesks();
                }
                else {
                    this.selectedSpaceId.set(null);
                }
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.loadingSpaces.set(false);
            }
        });
    }
    protected loadDesks(): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.desks.set([]);
        const spaceId = this.selectedSpaceId();
        if (!spaceId) {
            return;
        }
        this.loadingDesks.set(true);
        this.technicianService
            .getDesks(spaceId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (desks) => {
                this.desks.set(desks);
                this.loadingDesks.set(false);
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.loadingDesks.set(false);
            }
        });
    }
    protected selectSpace(spaceId: number): void {
        if (this.selectedSpaceId() === spaceId) {
            this.isRoomDropdownOpen.update((open) => !open);
            return;
        }
        this.selectedSpaceId.set(spaceId);
        this.selectedRoomId.set(null);
        this.isRoomDropdownOpen.set(true);
        this.deskSearch.set('');
        this.bulkSuspendedDeskIds.set([]);
        this.loadDesks();
    }
    protected selectRoom(roomId: number | null): void {
        if (this.selectedRoomId() === roomId) {
            return;
        }
        this.selectedRoomId.set(roomId);
        this.deskSearch.set('');
        this.bulkSuspendedDeskIds.set([]);
    }
    protected updateDeskSearch(value: string): void {
        this.deskSearch.set(value);
    }
    protected revertMaintenance(desk: Desk): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.isSubmitting.set(true);
        this.technicianService
            .revertMaintenance(desk.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.successMsg = `${this.deskLabel(desk)} ripristinata.`;
                this.isSubmitting.set(false);
                this.loadDesks();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.isSubmitting.set(false);
            }
        });
    }
    protected isDecommissioned(desk: Desk): boolean {
        return desk.state?.code?.toUpperCase() === 'DECOMMISSIONED';
    }
    protected setMaintenance(desk: Desk): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.isSubmitting.set(true);
        this.technicianService
            .setMaintenance(desk.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.successMsg = `${this.deskLabel(desk)} impostata in manutenzione.`;
                this.isSubmitting.set(false);
                this.loadDesks();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.isSubmitting.set(false);
            }
        });
    }
    protected toggleBulkMaintenance(): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.isSubmitting.set(true);
        const suspendedIds = this.bulkSuspendedDeskIds();
        if (suspendedIds.length > 0) {
            const requests = suspendedIds.map((id) => this.technicianService.revertMaintenance(id));
            if (requests.length === 0) {
                this.bulkSuspendedDeskIds.set([]);
                this.isSubmitting.set(false);
                return;
            }
            forkJoin(requests)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.successMsg = 'Tutte le postazioni sospese sono state ripristinate.';
                    this.bulkSuspendedDeskIds.set([]);
                    this.isSubmitting.set(false);
                    this.loadDesks();
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                    this.isSubmitting.set(false);
                    this.loadDesks();
                }
            });
        }
        else {
            const availableDesks = this.roomDesks().filter((d) => d.state?.code?.toUpperCase() === 'AVAILABLE');
            if (availableDesks.length === 0) {
                this.errorMsg = 'Nessuna postazione disponibile da mettere in manutenzione.';
                this.isSubmitting.set(false);
                return;
            }
            const idsToSuspend = availableDesks.map((d) => d.id);
            const requests = idsToSuspend.map((id) => this.technicianService.setMaintenance(id));
            forkJoin(requests)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.successMsg = 'Tutte le postazioni disponibili sono state messe in manutenzione.';
                    this.bulkSuspendedDeskIds.set(idsToSuspend);
                    this.isSubmitting.set(false);
                    this.loadDesks();
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                    this.isSubmitting.set(false);
                    this.loadDesks();
                }
            });
        }
    }
    protected goToTicket(desk: Desk): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.isSubmitting.set(true);
        forkJoin({
            pending: this.technicianService.getPendingTickets().pipe(catchError(() => of([]))),
            assigned: this.technicianService.getAssignedTickets().pipe(catchError(() => of([])))
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ pending, assigned }) => {
                this.isSubmitting.set(false);
                const allTickets = [...pending, ...assigned];
                const hasOpenTicket = allTickets.some(t => t.deskID === desk.id && t.status !== 'RESOLVED' && t.status !== 'CLOSED');
                if (hasOpenTicket) {
                    this.router.navigate(['/technician/tickets']);
                }
                else {
                    this.errorMsg = `Nessun ticket aperto per la postazione ${desk.code || desk.id}.`;
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
            },
            error: () => {
                this.isSubmitting.set(false);
                this.errorMsg = 'Errore durante la verifica delle segnalazioni.';
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });
    }
    protected deskLabel(desk: Desk): string {
        return desk.code ? `Postazione ${desk.code}` : `Postazione #${desk.id}`;
    }
    protected stateLabel(desk: Desk): string {
        const code = desk.state?.code?.toUpperCase();
        if (code === 'MAINTENANCE')
            return 'In manutenzione';
        if (code === 'PENDING_INSPECTION')
            return 'Da ispezionare';
        if (code === 'DECOMMISSIONED')
            return 'Dismessa';
        if (code === 'BOOKED')
            return 'Prenotata';
        if (code === 'AVAILABLE')
            return 'Disponibile';
        return desk.state?.getName() || code || 'Stato non disponibile';
    }
    protected stateClass(desk: Desk): string {
        const code = desk.state?.code?.toUpperCase() || '';
        const tone: Record<string, string> = {
            AVAILABLE: 'text-bg-success',
            BOOKED: 'text-bg-primary',
            MAINTENANCE: 'text-bg-warning text-dark',
            PENDING_INSPECTION: 'text-bg-secondary',
            DECOMMISSIONED: 'text-bg-secondary'
        };
        return tone[code] ?? 'text-bg-secondary';
    }
    private matchesSearch(desk: Desk, query: string): boolean {
        const terms = [
            desk.code,
            desk.id?.toString(),
            desk.building,
            desk.roomName,
            desk.roomCode,
            desk.state?.code,
            desk.state?.getName(),
            this.stateLabel(desk)
        ];
        return terms.some((term) => (term ?? '').toString().toLowerCase().includes(query));
    }
}
