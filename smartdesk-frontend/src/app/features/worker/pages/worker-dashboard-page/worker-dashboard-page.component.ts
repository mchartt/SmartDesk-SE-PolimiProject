import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Booking } from '../../../../core/models';
import { BookingService } from '../../../../core/services/booking.service';
import { localCalendarDateIso } from '../../../../core/utils/date.util';
import { ItalianCountPhrase } from '../../../../core/utils/italian-count.phrase';
import { NotificationService } from '../../../../core/services/notification.service';
import { TicketService } from '../../../../core/services/ticket.service';
import { RoleRouteTitleBase } from '../../../../shared/base/role-route-title.base';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
@Component({
    standalone: true,
    imports: [RouterLink, SdIconComponent, DatePipe, MdbRippleModule],
    templateUrl: './worker-dashboard-page.component.html',
    styleUrl: './worker-dashboard-page.component.scss'
})
export class WorkerDashboardPageComponent extends RoleRouteTitleBase implements OnInit {
    private readonly destroyRef = inject(DestroyRef);
    private readonly bookingService = inject(BookingService);
    private readonly ticketService = inject(TicketService);
    private readonly notificationService = inject(NotificationService);
    protected readonly user = this.authState.user;
    protected defaultRouteTitle(): string {
        return 'Panoramica lavoratore';
    }
    protected defaultDashboardTitle(): string {
        return 'Benvenuto, hai intenzione di prenotare oggi?';
    }
    protected readonly bookings = signal<Booking[]>([]);
    protected readonly unreadCount = signal(0);
    protected readonly openTicketsCount = signal(0);
    protected readonly todayIso = localCalendarDateIso;
    private static localDayFromStart(iso: string): string {
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) {
            return '';
        }
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }
    protected readonly todayBookings = computed(() => {
        const day = this.todayIso();
        return this.bookings()
            .filter((b) => {
            if ((b.status || '').toUpperCase() === 'CANCELLED') {
                return false;
            }
            const booked = (b.bookedDay || '').trim();
            if (booked === day) {
                return true;
            }
            if (!booked && b.startTime) {
                return WorkerDashboardPageComponent.localDayFromStart(b.startTime) === day;
            }
            return false;
        })
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    protected todayOfficeRoom(booking: Booking): string {
        const space = booking.spaceName?.trim() || '';
        const room = booking.buildingName?.trim() || '';
        if (space && room) {
            return `${space} · ${room}`;
        }
        return space || room || '—';
    }
    protected todayDeskLabel(booking: Booking): string {
        const code = booking.deskCode?.trim();
        return code || String(booking.deskID);
    }
    protected unreadBarPhrase(): string {
        return ItalianCountPhrase.format(this.unreadCount(), 'notifica non letta', 'notifiche non lette');
    }
    protected activeBookingsPhrase(): string {
        return ItalianCountPhrase.format(this.activeBookingsCount(), 'prenotazione attiva', 'prenotazioni attive');
    }
    protected openTicketsPhrase(): string {
        return ItalianCountPhrase.format(this.openTicketsCount(), 'segnalazione aperta', 'segnalazioni aperte');
    }
    protected unreadCardPhrase(): string {
        return ItalianCountPhrase.format(this.unreadCount(), 'non letta', 'non lette');
    }
    protected readonly activeBookingsCount = computed(() => {
        const now = Date.now();
        return this.bookings().filter((b) => {
            if ((b.status || '').toUpperCase() === 'CANCELLED') {
                return false;
            }
            return new Date(b.endTime).getTime() >= now;
        }).length;
    });
    public ngOnInit(): void {
        forkJoin({
            bookings: this.bookingService.getMyBookings().pipe(catchError(() => of([] as Booking[]))),
            tickets: this.ticketService.getMyTickets().pipe(catchError(() => of([]))),
            unread: this.notificationService.getUnreadCount().pipe(catchError(() => of(0)))
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ bookings, tickets, unread }) => {
                this.bookings.set(bookings);
                this.unreadCount.set(unread);
                this.openTicketsCount.set(tickets.filter((t) => !t.isResolved).length);
            },
            error: () => {
                this.bookings.set([]);
                this.unreadCount.set(0);
                this.openTicketsCount.set(0);
            }
        });
    }
}
