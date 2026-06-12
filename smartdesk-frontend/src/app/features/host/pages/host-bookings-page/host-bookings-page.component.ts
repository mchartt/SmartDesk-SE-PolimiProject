import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, NgZone, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { finalize } from 'rxjs';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdSearchCalendarFieldComponent } from '../../../../shared/components/sd-search-calendar-field/sd-search-calendar-field.component';
import { HostService } from '../../../../core/services/host.service';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { formatShortDate, formatTimeRange, localCalendarDateIso, localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
export type HostBookingsSection = 'today' | 'future' | 'history';
export interface HostBookingRow {
    bookingID: number;
    bookingCode?: string | null;
    deskID: number;
    deskCode?: string | null;
    spaceName?: string | null;
    buildingName?: string | null;
    workerName?: string | null;
    workerSurname?: string | null;
    workerEmail?: string | null;
    bookedDay?: string | null;
    startTime: string;
    endTime: string;
    status: string;
}
@Component({
    standalone: true,
    imports: [CommonModule, FormsModule, MdbRippleModule, EmptyStateComponent, SdSearchCalendarFieldComponent, SdIconComponent],
    templateUrl: './host-bookings-page.component.html',
    styleUrl: './host-bookings-page.component.scss'
})
export class HostBookingsPageComponent implements OnInit {
    private readonly hostService = inject(HostService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly ngZone = inject(NgZone);
    protected readonly bookings = signal<HostBookingRow[]>([]);
    protected readonly loading = signal(true);
    protected errorMsg = '';
    protected readonly activeSection = signal<HostBookingsSection>('today');
    protected readonly nowMs = signal(Date.now());
    protected readonly historyDeskCode = signal('');
    protected readonly historyDate = signal('');
    protected readonly historyEmail = signal('');
    protected readonly hasHistoryFilters = computed(() => {
        return (this.historyDeskCode().trim().length > 0 ||
            this.historyDate().trim().length > 0 ||
            this.historyEmail().trim().length > 0);
    });
    protected readonly todayBookings = computed(() => {
        const today = localCalendarDateIso();
        const now = this.nowMs();
        return this.bookings()
            .filter((b) => {
            const day = HostBookingsPageComponent.bookingCalendarDay(b);
            if (day !== today || HostBookingsPageComponent.isBookingEnded(b, now)) {
                return false;
            }
            if (HostBookingsPageComponent.isPending(b) && this.isInProgress(b)) {
                return false;
            }
            return true;
        })
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    protected readonly futureBookings = computed(() => {
        const today = localCalendarDateIso();
        return this.bookings()
            .filter((b) => {
            const day = HostBookingsPageComponent.bookingCalendarDay(b);
            return day !== '' && day > today;
        })
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    protected readonly pastBookings = computed(() => {
        const today = localCalendarDateIso();
        const now = this.nowMs();
        return this.bookings()
            .filter((b) => {
            if (HostBookingsPageComponent.isBookingEnded(b, now)) {
                return true;
            }
            const day = HostBookingsPageComponent.bookingCalendarDay(b);
            return day !== '' && day < today;
        })
            .sort((a, b) => new Date(b.endTime).getTime() - new Date(a.endTime).getTime());
    });
    protected readonly filteredPastBookings = computed(() => HostBookingsPageComponent.applyHistoryFilters(this.pastBookings(), this.historyDeskCode(), this.historyDate(), this.historyEmail()));
    protected readonly highlightedHistoryDays = computed(() => {
        const days = new Set<string>();
        for (const b of this.pastBookings()) {
            const day = HostBookingsPageComponent.bookingCalendarDay(b);
            if (day) {
                days.add(day);
            }
        }
        return [...days];
    });
    protected readonly visibleBookings = computed(() => {
        const section = this.activeSection();
        if (section === 'today') {
            return this.todayBookings();
        }
        if (section === 'future') {
            return this.futureBookings();
        }
        return this.filteredPastBookings();
    });
    public ngOnInit(): void {
        this.load();
        this.ngZone.runOutsideAngular(() => {
            const tick = window.setInterval(() => this.nowMs.set(Date.now()), 30000);
            this.destroyRef.onDestroy(() => window.clearInterval(tick));
        });
    }
    protected load(): void {
        this.loading.set(true);
        this.errorMsg = '';
        this.hostService
            .getHostBookings()
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (data) => this.bookings.set(HostBookingsPageComponent.withoutCancelled(data.map((row) => HostBookingsPageComponent.mapRow(row)))),
            error: (err: Error) => (this.errorMsg = err.message)
        });
    }
    protected setSection(section: HostBookingsSection): void {
        this.activeSection.set(section);
    }
    protected resetHistoryFilters(): void {
        this.historyDeskCode.set('');
        this.historyDate.set('');
        this.historyEmail.set('');
    }
    protected workerDisplayName(b: HostBookingRow): string {
        return `${b.workerName ?? ''} ${b.workerSurname ?? ''}`.trim() || 'Lavoratore';
    }
    protected bookingRef(b: HostBookingRow): string {
        const c = b.bookingCode?.trim() ?? '';
        return c.length > 0 ? c : String(b.bookingID);
    }
    protected deskLabel(b: HostBookingRow): string {
        return b.deskCode?.trim() ? b.deskCode.trim() : `#${b.deskID}`;
    }
    protected formatWhen(iso: string): string {
        return formatShortDate(iso);
    }
    protected formatTime(start: string, end: string): string {
        return formatTimeRange(start, end);
    }
    protected statusLabel(status: string): string {
        const s = (status ?? '').toUpperCase();
        if (s === 'CONFIRMED') {
            return 'Confermata';
        }
        if (s === 'PENDING') {
            return 'In attesa';
        }
        if (s === 'CANCELLED') {
            return 'Annullata';
        }
        return status || '—';
    }
    protected statusTone(status: string): 'confirmed' | 'pending' | 'other' {
        const s = (status ?? '').toUpperCase();
        if (s === 'CONFIRMED') {
            return 'confirmed';
        }
        if (s === 'PENDING') {
            return 'pending';
        }
        return 'other';
    }
    protected isInProgress(b: HostBookingRow): boolean {
        const start = new Date(b.startTime).getTime();
        const end = new Date(b.endTime).getTime();
        const now = this.nowMs();
        return start <= now && end > now;
    }
    protected emptyMessage(): string {
        const section = this.activeSection();
        if (section === 'today') {
            return 'Nessuna prenotazione per oggi nei tuoi spazi.';
        }
        if (section === 'future') {
            return 'Nessuna prenotazione futura pianificata.';
        }
        if (this.hasHistoryFilters()) {
            return 'Nessuna prenotazione passata corrisponde ai filtri.';
        }
        return 'Lo storico è vuoto: le prenotazioni concluse appariranno qui.';
    }
    private static mapRow(raw: Record<string, unknown>): HostBookingRow {
        return {
            bookingID: Number(raw['bookingID'] ?? 0),
            bookingCode: (raw['bookingCode'] as string) ?? null,
            deskID: Number(raw['deskID'] ?? 0),
            deskCode: (raw['deskCode'] as string) ?? null,
            spaceName: (raw['spaceName'] as string) ?? null,
            buildingName: (raw['buildingName'] as string) ?? null,
            workerName: (raw['workerName'] as string) ?? null,
            workerSurname: (raw['workerSurname'] as string) ?? null,
            workerEmail: (raw['workerEmail'] as string) ?? null,
            bookedDay: (raw['bookedDay'] as string) ?? null,
            startTime: String(raw['startTime'] ?? ''),
            endTime: String(raw['endTime'] ?? ''),
            status: String(raw['status'] ?? '')
        };
    }
    private static withoutCancelled(rows: HostBookingRow[]): HostBookingRow[] {
        return rows.filter((b) => (b.status || '').toUpperCase() !== 'CANCELLED');
    }
    private static isBookingEnded(b: HostBookingRow, nowMs: number): boolean {
        return new Date(b.endTime).getTime() < nowMs;
    }
    private static isPending(b: HostBookingRow): boolean {
        return (b.status || '').toUpperCase() === 'PENDING';
    }
    private static bookingCalendarDay(b: HostBookingRow): string {
        if (b.startTime) {
            const normalized = b.startTime.replace(/Z$/, '');
            const d = new Date(normalized);
            if (!Number.isNaN(d.getTime())) {
                return localCalendarDateIsoFromDate(d);
            }
        }
        return (b.bookedDay ?? '').trim().slice(0, 10);
    }
    private static applyHistoryFilters(rows: HostBookingRow[], deskCodeRaw: string, dateRaw: string, emailRaw: string): HostBookingRow[] {
        const deskQ = deskCodeRaw.trim().toUpperCase().replace(/^#/, '').replace(/^DESK#/, '');
        const date = dateRaw.trim();
        const email = emailRaw
            .trim()
            .toLowerCase()
            .normalize('NFD')
            .replace(/\p{M}/gu, '');
        if (!deskQ && !date && !email) {
            return rows;
        }
        return rows.filter((b) => HostBookingsPageComponent.matchesHistoryFilters(b, deskQ, date, email));
    }
    private static matchesHistoryFilters(b: HostBookingRow, deskQ: string, date: string, email: string): boolean {
        if (deskQ) {
            const desk = (b.deskCode ?? '').trim().toUpperCase();
            const deskId = String(b.deskID);
            if (!desk.includes(deskQ) && !deskId.includes(deskQ)) {
                return false;
            }
        }
        if (date && HostBookingsPageComponent.bookingCalendarDay(b) !== date) {
            return false;
        }
        if (email) {
            const workerMail = (b.workerEmail ?? '')
                .toLowerCase()
                .normalize('NFD')
                .replace(/\p{M}/gu, '');
            const workerName = `${b.workerName ?? ''} ${b.workerSurname ?? ''}`
                .toLowerCase()
                .normalize('NFD')
                .replace(/\p{M}/gu, '');
            if (!workerMail.includes(email) && !workerName.includes(email)) {
                return false;
            }
        }
        return true;
    }
}
