import { ChangeDetectorRef, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { TechnicianAssignedSpaceRow, TechnicianService, TechnicianTicketRow } from '../../../../core/services/technician.service';
import { technicianTicketPrimaryHeading, technicianTicketRefSubtitle, technicianTicketTitleTooltip } from '../../../../core/utils/technician-ticket-display';
import { TechnicianSpaceFilterCardsComponent } from '../../components/technician-space-filter-cards/technician-space-filter-cards.component';
import type { TechnicianSpaceFilterValue } from '../../../../core/utils/technician-ticket-list.util';
import { filterTechnicianTicketsBySpace, technicianDeskLabel, technicianSpaceCaption } from '../../../../core/utils/technician-ticket-list.util';
import { formatShortDateTime } from '../../../../core/utils/date.util';
import { ToastService } from '../../../../core/services/toast.service';
@Component({
    standalone: true,
    imports: [
        CommonModule,
        EmptyStateComponent,
        TechnicianSpaceFilterCardsComponent,
        SdIconComponent,
        SdModalHeaderComponent,
        MdbRippleModule
    ],
    templateUrl: './technician-pending-tickets-page.component.html',
    styleUrl: './technician-pending-tickets-page.component.scss'
})
export class TechnicianPendingTicketsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly technicianService = inject(TechnicianService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly toast = inject(ToastService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Segnalazioni in attesa');
    protected readonly ticketsAll = signal<TechnicianTicketRow[]>([]);
    protected readonly assignedSpaces = signal<TechnicianAssignedSpaceRow[]>([]);
    protected readonly spaceFilter = signal<TechnicianSpaceFilterValue>('all');
    protected readonly detailTicket = signal<TechnicianTicketRow | null>(null);
    protected readonly claimingTicketId = signal<number | null>(null);
    protected readonly assignedSpaceIdSet = computed(() => new Set(this.assignedSpaces().map((s) => s.spaceID)));
    protected readonly visibleTickets = computed(() => filterTechnicianTicketsBySpace(this.ticketsAll(), this.spaceFilter(), this.assignedSpaceIdSet()));
    protected readonly listEmptyAfterFilter = computed(() => this.ticketsAll().length > 0 && this.visibleTickets().length === 0);
    protected errorMsg = '';
    protected readonly loading = signal(false);
    public ngOnInit(): void {
        this.load();
    }
    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        forkJoin({
            spaces: this.technicianService.getAssignedSpaces().pipe(catchError(() => of([]))),
            tickets: this.technicianService.getPendingTickets().pipe(catchError(() => of([])))
        })
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: ({ spaces, tickets }) => {
                this.assignedSpaces.set(spaces);
                this.ticketsAll.set(tickets);
                this.spaceFilter.set('all');
                const open = this.detailTicket();
                if (open) {
                    const updated = tickets.find((t) => t.ticketID === open.ticketID);
                    this.detailTicket.set(updated ?? null);
                }
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.ticketsAll.set([]);
                this.assignedSpaces.set([]);
                this.detailTicket.set(null);
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
    protected onSpaceFilterChange(value: TechnicianSpaceFilterValue): void {
        this.spaceFilter.set(value);
    }
    protected ticketPrimaryHeading(t: TechnicianTicketRow): string {
        return technicianTicketPrimaryHeading(t);
    }
    protected ticketRefSubtitle(t: TechnicianTicketRow): string | null {
        return technicianTicketRefSubtitle(t);
    }
    protected ticketTitleTooltip(t: TechnicianTicketRow): string {
        return technicianTicketTitleTooltip(t);
    }
    protected deskLabel(t: TechnicianTicketRow): string {
        return technicianDeskLabel(t);
    }
    protected spaceCaption(t: TechnicianTicketRow): string {
        return technicianSpaceCaption(t);
    }
    protected formatTicketWhen(iso: string | null | undefined): string {
        return formatShortDateTime(iso);
    }
    protected ticketDescriptionText(ticket: TechnicianTicketRow): string {
        return (ticket.description ?? '').trim();
    }
    protected ticketDescriptionPreview(ticket: TechnicianTicketRow): string | null {
        const text = this.ticketDescriptionText(ticket);
        if (!text) {
            return null;
        }
        return text.length > 120 ? `${text.slice(0, 120)}…` : text;
    }
    protected openTicketDetail(ticket: TechnicianTicketRow): void {
        this.detailTicket.set(ticket);
    }
    protected closeTicketDetail(): void {
        this.detailTicket.set(null);
    }
    protected claimTicket(ticketId: number): void {
        if (this.claimingTicketId() !== null) {
            return;
        }
        this.errorMsg = '';
        this.claimingTicketId.set(ticketId);
        this.technicianService
            .updateStatus(ticketId, 'IN_PROGRESS', 'Prendo in carico la segnalazione.')
            .pipe(finalize(() => this.claimingTicketId.set(null)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.detailTicket.set(null);
                this.toast.success('Segnalazione presa in carico.');
                this.load();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.toast.error(err.message);
            }
        });
    }
}
