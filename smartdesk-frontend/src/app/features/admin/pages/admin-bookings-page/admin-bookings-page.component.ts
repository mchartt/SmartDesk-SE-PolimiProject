import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Booking } from '../../../../core/models';
import { AdminService } from '../../../../core/services/admin.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdSearchCalendarFieldComponent } from '../../../../shared/components/sd-search-calendar-field/sd-search-calendar-field.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { bookingPublicRef } from '../../../../core/utils/booking.util';
import { localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
@Component({
    standalone: true,
    imports: [CommonModule, FormsModule, EmptyStateComponent, SdSearchCalendarFieldComponent, SdIconComponent, MdbRippleModule],
    templateUrl: './admin-bookings-page.component.html',
    styleUrl: './admin-bookings-page.component.scss'
})
export class AdminBookingsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    private readonly confirmService = inject(ConfirmModalService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Prenotazioni');
    protected readonly bookings = signal<Booking[]>([]);
    protected readonly loading = signal(false);
    protected readonly cancellingId = signal<number | null>(null);
    protected errorMsg = '';
    protected filterStatus = signal<string>('ALL');
    protected filterDate = signal<string>('');
    protected filterQuery = signal<string>('');
    protected readonly highlightedBookingDays = computed(() => {
        const days = new Set<string>();
        for (const b of this.bookings()) {
            const day = this.bookingCalendarDay(b);
            if (day) {
                days.add(day);
            }
        }
        return [...days];
    });
    protected readonly filteredBookings = computed(() => {
        const q = this.filterQuery().trim().toLowerCase();
        const date = this.filterDate().trim();
        return this.bookings().filter((b) => {
            const statusOk = this.filterStatus() === 'ALL' || b.status === this.filterStatus();
            if (!statusOk) {
                return false;
            }
            if (date && this.bookingCalendarDay(b) !== date) {
                return false;
            }
            if (!q) {
                return true;
            }
            const haystack = [
                this.bookingPublicRef(b),
                b.workerName,
                b.workerEmail,
                b.spaceName,
                b.city,
                b.deskCode,
                String(b.deskID)
            ]
                .join(' ')
                .toLowerCase();
            return haystack.includes(q);
        });
    });
    public ngOnInit(): void {
        this.load();
    }
    protected readonly bookingPublicRef = bookingPublicRef;
    private bookingCalendarDay(b: Booking): string {
        if (b.startTime) {
            const normalized = b.startTime.replace(/Z$/, '');
            const d = new Date(normalized);
            if (!Number.isNaN(d.getTime())) {
                return localCalendarDateIsoFromDate(d);
            }
        }
        return (b.bookedDay ?? '').trim().slice(0, 10);
    }
    protected statusLabel(status: string): string {
        switch (status) {
            case 'CONFIRMED':
                return 'Confermata';
            case 'CANCELLED':
                return 'Annullata';
            case 'PENDING':
                return 'In attesa';
            default:
                return status;
        }
    }
    protected statusBadgeClass(status: string): string {
        switch (status) {
            case 'CONFIRMED':
                return 'badge-success';
            case 'CANCELLED':
                return 'badge-secondary';
            case 'PENDING':
                return 'badge-warning text-dark';
            default:
                return 'badge-light text-dark border';
        }
    }
    protected canCancel(booking: Booking): boolean {
        return booking.status !== 'CANCELLED';
    }
    protected cancelBooking(booking: Booking): void {
        if (!this.canCancel(booking) || this.cancellingId() != null) {
            return;
        }
        const ref = this.bookingPublicRef(booking);
        this.confirmService
            .confirm({
            title: 'Annulla prenotazione',
            message: `Annullare la prenotazione #${ref}? La postazione tornerà disponibile e l'azione non può essere annullata.`,
            confirmLabel: 'Annulla prenotazione',
            cancelLabel: 'Mantieni',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((confirmed) => {
            if (!confirmed) {
                return;
            }
            this.errorMsg = '';
            this.cancellingId.set(booking.bookingID);
            this.adminService
                .cancelBooking(booking.bookingID)
                .pipe(finalize(() => this.cancellingId.set(null)), takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => this.bookings.update((rows) => rows.filter((b) => b.bookingID !== booking.bookingID)),
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }
    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        this.adminService
            .getBookings()
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => this.bookings.set(rows),
            error: (err: Error) => {
                this.bookings.set([]);
                this.errorMsg = err.message;
            }
        });
    }
}
