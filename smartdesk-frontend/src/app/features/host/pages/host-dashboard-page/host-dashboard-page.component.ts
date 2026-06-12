import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { Desk, Space } from '../../../../core/models';
import { HostService } from '../../../../core/services/host.service';
import { RoleRouteTitleBase } from '../../../../shared/base/role-route-title.base';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
@Component({
    standalone: true,
    imports: [CommonModule, RouterLink, SdIconComponent, MdbRippleModule],
    templateUrl: './host-dashboard-page.component.html',
    styleUrl: './host-dashboard-page.component.scss'
})
export class HostDashboardPageComponent extends RoleRouteTitleBase implements OnInit {
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    protected defaultRouteTitle(): string {
        return 'Panoramica host';
    }
    protected defaultDashboardTitle(): string {
        return 'Panoramica host';
    }
    protected readonly spaces = signal<Space[]>([]);
    protected readonly spacesTotal = signal(0);
    protected readonly spacesApproved = signal(0);
    protected readonly spacesPending = signal(0);
    protected readonly desksTotal = signal(0);
    protected readonly techniciansCount = signal(0);
    protected readonly reviewsCount = signal(0);
    protected readonly ticketsOpenCount = signal(0);
    protected readonly ticketsCountIncomplete = signal(false);
    public ngOnInit(): void {
        forkJoin({
            spaces: this.hostService.getSpaces().pipe(catchError(() => of([] as Space[]))),
            technicians: this.hostService.getAllTechnicians().pipe(catchError(() => of([]))),
            reviews: this.hostService.getReviews().pipe(catchError(() => of([])))
        })
            .pipe(switchMap(({ spaces, technicians, reviews }) => {
            this.spaces.set(spaces);
            this.spacesTotal.set(spaces.length);
            this.spacesApproved.set(spaces.filter((s) => s.approved).length);
            this.spacesPending.set(spaces.filter((s) => !s.approved).length);
            this.techniciansCount.set(technicians.length);
            this.reviewsCount.set(reviews.length);
            if (!spaces.length) {
                this.desksTotal.set(0);
                this.ticketsCountIncomplete.set(false);
                return of(0);
            }
            return forkJoin(spaces.map((s) => this.hostService.getDesks(s.spaceID).pipe(map((desks) => ({ desks, ok: true as const })), catchError(() => of({ desks: [] as Desk[], ok: false as const }))))).pipe(switchMap((deskResults) => {
                this.ticketsCountIncomplete.set(deskResults.some((r) => !r.ok));
                const allDesks = deskResults.flatMap((r) => r.desks);
                this.desksTotal.set(allDesks.length);
                if (!allDesks.length) {
                    return of(0);
                }
                return forkJoin(allDesks.map((d) => this.hostService.getDeskTickets(d.id).pipe(catchError(() => of([])), map((rows) => rows.filter((t) => t['status'] === 'OPEN').length)))).pipe(map((counts) => counts.reduce((a, b) => a + b, 0)));
            }));
        }))
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (ticketTotal) => this.ticketsOpenCount.set(ticketTotal),
            error: () => {
                this.spaces.set([]);
                this.spacesTotal.set(0);
                this.spacesApproved.set(0);
                this.spacesPending.set(0);
                this.desksTotal.set(0);
                this.techniciansCount.set(0);
                this.reviewsCount.set(0);
                this.ticketsOpenCount.set(0);
                this.ticketsCountIncomplete.set(false);
            }
        });
    }
}
