import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { NotificationService } from '../../../../core/services/notification.service';
import { TechnicianService, TechnicianTicketRow } from '../../../../core/services/technician.service';
import { Technician } from '../../../../core/models';
import { RoleRouteTitleBase } from '../../../../shared/base/role-route-title.base';
import { TicketStatusBadgeComponent } from '../../../../shared/components/ticket-status-badge/ticket-status-badge.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
@Component({
    standalone: true,
    imports: [RouterLink, SdIconComponent, DatePipe, MdbRippleModule, TicketStatusBadgeComponent],
    templateUrl: './technician-dashboard-page.component.html',
    styleUrl: './technician-dashboard-page.component.scss'
})
export class TechnicianDashboardPageComponent extends RoleRouteTitleBase implements OnInit {
    private readonly destroyRef = inject(DestroyRef);
    private readonly technicianService = inject(TechnicianService);
    private readonly notificationService = inject(NotificationService);
    protected defaultRouteTitle(): string {
        return 'Panoramica tecnico';
    }
    protected defaultDashboardTitle(): string {
        return 'Panoramica tecnico';
    }
    protected override resolveDashboardTitle(): string {
        const u = this.authState.user();
        return u instanceof Technician ? `Ciao, ${u.displayName} bentornato` : this.defaultDashboardTitle();
    }
    protected readonly pendingCount = signal(0);
    protected readonly assignedActiveCount = signal(0);
    protected readonly resolvedRecentCount = signal(0);
    protected readonly unreadCount = signal(0);
    protected readonly assignedSpacesCount = signal(0);
    protected readonly activeTickets = signal<TechnicianTicketRow[]>([]);
    public ngOnInit(): void {
        forkJoin({
            pending: this.technicianService.getPendingTickets().pipe(catchError(() => of([]))),
            assigned: this.technicianService.getAssignedTickets().pipe(catchError(() => of([]))),
            unread: this.notificationService.getUnreadCount().pipe(catchError(() => of(0))),
            spaces: this.technicianService.getAssignedSpaces().pipe(catchError(() => of([])))
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ pending, assigned, unread, spaces }) => {
                this.pendingCount.set(pending.length);
                const active = assigned.filter((t) => (t.status || '').toUpperCase() !== 'RESOLVED');
                this.activeTickets.set(active);
                this.assignedActiveCount.set(active.length);
                this.resolvedRecentCount.set(assigned.filter((t) => (t.status || '').toUpperCase() === 'RESOLVED').length);
                this.unreadCount.set(unread);
                this.assignedSpacesCount.set(spaces.length);
            },
            error: () => {
                this.pendingCount.set(0);
                this.activeTickets.set([]);
                this.assignedActiveCount.set(0);
                this.resolvedRecentCount.set(0);
                this.unreadCount.set(0);
                this.assignedSpacesCount.set(0);
            }
        });
    }
}
