import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, NgZone, signal, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Booking } from '../../../../core/models';
import { finalize } from 'rxjs/operators';
import { BookingService } from '../../../../core/services/booking.service';
import { ReviewService } from '../../../../core/services/review.service';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdSearchCalendarFieldComponent } from '../../../../shared/components/sd-search-calendar-field/sd-search-calendar-field.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { bookingPublicRef } from '../../../../core/utils/booking.util';
import { localCalendarDateIso, localCalendarDateIsoFromDate } from '../../../../core/utils/date.util';
@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        RouterLink,
        MdbFormsModule,
        MdbModalModule,
        MdbRippleModule,
        SdModalHeaderComponent,
        SdSearchCalendarFieldComponent,
        SdIconComponent
    ],
    templateUrl: './worker-bookings-page.component.html',
    styleUrl: './worker-bookings-page.component.scss'
})
export class WorkerBookingsPageComponent implements OnInit {
    @ViewChild('bookingSearchModal')
    private readonly bookingSearchModal!: TemplateRef<unknown>;
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly ngZone = inject(NgZone);
    private readonly bookingService = inject(BookingService);
    private readonly reviewService = inject(ReviewService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly modalService = inject(MdbModalService);
    private searchModalRef: MdbModalRef<unknown> | null = null;
    protected readonly reviewedBookingIds = signal<Set<number>>(new Set());
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Le mie prenotazioni');
    protected bookings = signal<Booking[]>([]);
    protected readonly searchCode = signal('');
    protected readonly searchDate = signal('');
    protected searchCodeDraft = '';
    protected searchDateDraft = '';
    protected readonly clearingHistory = signal(false);
    protected readonly nowMs = signal(Date.now());
    protected readonly leavingBookingId = signal<number | null>(null);
    protected readonly hasActiveSearch = computed(() => this.searchCode().trim().length > 0 || this.searchDate().trim().length > 0);
    protected readonly todayBookings = computed(() => {
        const today = localCalendarDateIso();
        const now = this.nowMs();
        return this.bookings()
            .filter((b) => {
            const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
            return day === today && !WorkerBookingsPageComponent.isBookingEnded(b, now);
        })
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    protected readonly pastBookings = computed(() => {
        const today = localCalendarDateIso();
        const now = this.nowMs();
        return this.bookings()
            .filter((b) => {
            if (WorkerBookingsPageComponent.isBookingEnded(b, now)) {
                return true;
            }
            const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
            return day !== '' && day < today;
        })
            .sort((a, b) => new Date(b.endTime).getTime() - new Date(a.endTime).getTime());
    });
    protected readonly futureBookings = computed(() => {
        const today = localCalendarDateIso();
        return this.bookings()
            .filter((b) => {
            const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
            return day !== '' && day > today;
        })
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    });
    protected readonly filteredTodayBookings = computed(() => WorkerBookingsPageComponent.applySearchFilter(this.todayBookings(), this.searchCode(), this.searchDate()));
    protected readonly filteredFutureBookings = computed(() => WorkerBookingsPageComponent.applySearchFilter(this.futureBookings(), this.searchCode(), this.searchDate()));
    protected readonly filteredPastBookings = computed(() => WorkerBookingsPageComponent.applySearchFilter(this.pastBookings(), this.searchCode(), this.searchDate()));
    protected readonly searchResultCount = computed(() => {
        if (!this.hasActiveSearch()) {
            return 0;
        }
        return (this.filteredTodayBookings().length +
            this.filteredFutureBookings().length +
            this.filteredPastBookings().length);
    });
    protected errorMsg = '';
    protected readonly loading = signal(false);
    public ngOnInit(): void {
        this.load();
        this.loadReviewFlags();
        this.ngZone.runOutsideAngular(() => {
            const tick = window.setInterval(() => this.nowMs.set(Date.now()), 30000);
            this.destroyRef.onDestroy(() => window.clearInterval(tick));
        });
    }
    protected openSearchModal(): void {
        this.searchCodeDraft = this.searchCode();
        this.searchDateDraft = this.searchDate();
        this.searchModalRef?.close();
        this.searchModalRef = this.modalService.open(this.bookingSearchModal, {
            modalClass: 'modal-dialog-centered',
            ignoreBackdropClick: false
        });
    }
    protected closeSearchModal(): void {
        this.searchModalRef?.close();
        this.searchModalRef = null;
    }
    protected applySearchFromModal(): void {
        this.searchCode.set(this.searchCodeDraft.trim());
        this.searchDate.set(this.searchDateDraft.trim());
        this.closeSearchModal();
    }
    protected clearSearchDraftInModal(): void {
        this.searchCodeDraft = '';
        this.searchDateDraft = '';
    }
    protected highlightedBookingDays(): readonly string[] {
        return [...this.bookingDaysSet()];
    }
    private bookingDaysSet(): Set<string> {
        const set = new Set<string>();
        for (const b of this.bookings()) {
            const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
            if (day) {
                set.add(day);
            }
        }
        return set;
    }
    protected get canApplySearchFromModal(): boolean {
        return this.searchCodeDraft.trim().length > 0 || this.searchDateDraft.trim().length > 0;
    }
    protected resetSearch(): void {
        this.searchCode.set('');
        this.searchDate.set('');
        this.searchCodeDraft = '';
        this.searchDateDraft = '';
        this.closeSearchModal();
    }
    protected hasReviewFor(bookingId: number): boolean {
        return this.reviewedBookingIds().has(bookingId);
    }
    protected isPastBooking(b: Booking): boolean {
        return WorkerBookingsPageComponent.isBookingEnded(b, this.nowMs());
    }
    protected isBookingInProgress(b: Booking): boolean {
        return WorkerBookingsPageComponent.isBookingInProgress(b, this.nowMs());
    }
    protected canCancelBooking(b: Booking): boolean {
        return !this.isPastBooking(b) && !this.isBookingInProgress(b);
    }
    protected bookingActionMode(b: Booking): 'cancel' | 'leave' {
        return this.isBookingInProgress(b) ? 'leave' : 'cancel';
    }
    protected readonly bookingPublicRef = bookingPublicRef;
    protected clearPastHistory(): void {
        if (this.clearingHistory() || !this.pastBookings().length) {
            return;
        }
        this.confirmService
            .confirm({
            title: 'Svuota storico',
            message: 'Vuoi rimuovere tutte le prenotazioni passate dal tuo profilo? Le recensioni collegate verranno eliminate. L’azione non può essere annullata.',
            confirmLabel: 'Svuota storico',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((confirmed) => {
            if (!confirmed) {
                return;
            }
            this.errorMsg = '';
            this.clearingHistory.set(true);
            this.bookingService
                .clearPastBookingHistory()
                .pipe(finalize(() => this.clearingHistory.set(false)), takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    const today = localCalendarDateIso();
                    this.bookings.update((rows) => rows.filter((b) => {
                        const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
                        return day === '' || day >= today;
                    }));
                    this.loadReviewFlags();
                },
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }
    private loadReviewFlags(): void {
        this.reviewService
            .getMyReviewHistory()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (reviews) => this.reviewedBookingIds.set(new Set(reviews.map((r) => r.bookingID))),
            error: () => this.reviewedBookingIds.set(new Set())
        });
    }
    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        this.bookingService
            .getMyBookings()
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                this.bookings.set(WorkerBookingsPageComponent.withoutCancelled(rows));
            },
            error: (err: Error) => {
                this.bookings.set([]);
                this.errorMsg = err.message;
            }
        });
    }
    protected leaveDesk(bookingId: number): void {
        this.confirmService
            .confirm({
            title: 'Lascia postazione',
            message: 'Vuoi terminare la sessione adesso? La prenotazione andrà nello storico con l’orario di uscita effettivo.',
            confirmLabel: 'Lascia postazione',
            cancelLabel: 'Resta in sede',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((confirmed) => {
            if (!confirmed) {
                return;
            }
            this.errorMsg = '';
            this.leavingBookingId.set(bookingId);
            this.bookingService
                .leaveDesk(bookingId)
                .pipe(finalize(() => this.leavingBookingId.set(null)), takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: (updated) => {
                    this.bookings.update((current) => current.map((b) => (b.bookingID === bookingId ? updated : b)));
                    this.nowMs.set(Date.now());
                },
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }
    protected cancelBooking(bookingId: number): void {
        this.confirmService
            .confirm({
            title: 'Annulla prenotazione',
            message: 'Sei sicuro di voler annullare questa prenotazione? L’azione non può essere annullata.',
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
            this.bookingService
                .cancelBooking(bookingId)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.bookings.update((current) => current.filter((b) => b.bookingID !== bookingId));
                },
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }
    private static isBookingEnded(b: Booking, nowMs: number): boolean {
        return new Date(b.endTime).getTime() < nowMs;
    }
    private static isBookingInProgress(b: Booking, nowMs: number): boolean {
        const start = new Date(b.startTime).getTime();
        const end = new Date(b.endTime).getTime();
        return start <= nowMs && end > nowMs;
    }
    private static withoutCancelled(rows: Booking[]): Booking[] {
        return rows.filter((b) => (b.status || '').toUpperCase() !== 'CANCELLED');
    }
    private static applySearchFilter(rows: Booking[], codeRaw: string, dateRaw: string): Booking[] {
        const code = codeRaw.trim().toUpperCase();
        const date = dateRaw.trim();
        if (!code && !date) {
            return rows;
        }
        return rows.filter((b) => WorkerBookingsPageComponent.bookingMatchesSearch(b, code, date));
    }
    private static bookingMatchesSearch(b: Booking, code: string, date: string): boolean {
        if (code) {
            const ref = (b.bookingCode?.trim() || String(b.bookingID)).toUpperCase();
            const idPart = String(b.bookingID);
            if (!ref.includes(code) && !idPart.includes(code)) {
                return false;
            }
        }
        if (date) {
            const day = WorkerBookingsPageComponent.bookingCalendarDay(b);
            if (day !== date) {
                return false;
            }
        }
        return true;
    }
    private static bookingCalendarDay(b: Booking): string {
        if (b.startTime) {
            const fromStart = WorkerBookingsPageComponent.localDayFromStart(b.startTime);
            if (fromStart) {
                return fromStart;
            }
        }
        return (b.bookedDay || '').trim();
    }
    private static localDayFromStart(iso: string): string {
        const normalized = iso.replace(/Z$/, '');
        const d = new Date(normalized);
        if (Number.isNaN(d.getTime())) {
            return '';
        }
        return localCalendarDateIsoFromDate(d);
    }
}
