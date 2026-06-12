import { CommonModule, formatDate } from '@angular/common';
import { Component, DestroyRef, TemplateRef, ViewChild, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbTabsModule, MdbTabChange } from 'mdb-angular-ui-kit/tabs';
import { filter, switchMap } from 'rxjs/operators';
import { Booking } from '../../../../core/models';
import { Review } from '../../../../core/models';
import { formatReviewDate } from '../../../../core/utils/date.util';
import { BookingService } from '../../../../core/services/booking.service';
import { ReviewService } from '../../../../core/services/review.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { bookingPublicRef } from '../../../../core/utils/booking.util';
import { normalizeForSearch } from '../../../../core/utils/search.util';
@Component({
    selector: 'app-worker-reviews-page',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MdbFormsModule,
        MdbModalModule,
        MdbRippleModule,
        MdbCollapseModule,
        MdbTabsModule,
        SdModalHeaderComponent,
        SdIconComponent
    ],
    templateUrl: './worker-reviews-page.component.html',
    styleUrl: './worker-reviews-page.component.scss'
})
export class WorkerReviewsPageComponent implements OnInit {
    @ViewChild('bookingPickerModal')
    private readonly bookingPickerModal!: TemplateRef<unknown>;
    private readonly route = inject(ActivatedRoute);
    private readonly bookingService = inject(BookingService);
    private readonly reviewService = inject(ReviewService);
    private readonly toast = inject(ToastService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly modalService = inject(MdbModalService);
    private bookingPickerModalRef: MdbModalRef<unknown> | null = null;
    protected readonly COMMENT_MIN = 50;
    protected readonly COMMENT_MAX = 250;
    protected readonly stars = [1, 2, 3, 4, 5] as const;
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Recensioni');
    protected readonly completedBookings = signal<Booking[]>([]);
    protected readonly loadingEligible = signal(false);
    protected readonly historyReviews = signal<Review[]>([]);
    protected readonly historyLoading = signal(false);
    protected readonly eligibleLoadError = signal('');
    protected readonly historyLoadError = signal('');
    protected readonly submitError = signal('');
    protected readonly successMsg = signal('');
    protected readonly rating = signal(5);
    protected readonly hoverStar = signal<number | null>(null);
    protected readonly officeFilterId = signal<number | 'all'>('all');
    protected readonly searchQuery = signal('');
    protected readonly activeHistoryTab = signal<'all' | 'new' | 'old'>('all');
    protected readonly detailReview = signal<Review | null>(null);
    protected readonly editingReviewId = signal<number | null>(null);
    protected readonly deletingReviewId = signal<number | null>(null);
    protected readonly bookingId = signal<number | null>(null);
    protected readonly commentText = signal('');
    protected readonly effectiveStars = computed(() => this.hoverStar() ?? this.rating());
    private isCurrentWeek(isoDate: string | null): boolean {
        if (!isoDate)
            return false;
        const d = new Date(isoDate);
        const now = new Date();
        const diff = now.getTime() - d.getTime();
        const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;
        return diff < sevenDaysMs;
    }
    protected readonly filteredHistory = computed(() => {
        let rows = [...this.historyReviews()];
        const oid = this.officeFilterId();
        if (oid !== 'all') {
            rows = rows.filter((r) => r.spaceID === oid);
        }
        const raw = this.searchQuery().trim();
        if (!raw) {
            return rows;
        }
        const tokens = raw
            .split(/\s+/u)
            .map((t) => normalizeForSearch(t))
            .filter(Boolean);
        if (!tokens.length) {
            return rows;
        }
        return rows.filter((r) => {
            const hay = normalizeForSearch([
                r.comment,
                r.spaceName,
                r.city,
                r.spaceOfficeCode,
                String(r.bookingID),
                r.bookingCode ?? '',
                String(r.rating),
                String(r.reviewID),
                String(r.spaceID),
                this.locationLine(r)
            ].join(' '));
            return tokens.every((tok) => hay.includes(tok));
        });
    });
    protected readonly newReviews = computed(() => {
        return this.filteredHistory().filter((r) => this.isCurrentWeek(r.createdAt));
    });
    protected readonly oldReviews = computed(() => {
        return this.filteredHistory().filter((r) => !this.isCurrentWeek(r.createdAt));
    });
    protected readonly displayedHistoryReviews = computed(() => {
        switch (this.activeHistoryTab()) {
            case 'new':
                return this.newReviews();
            case 'old':
                return this.oldReviews();
            default:
                return this.filteredHistory();
        }
    });
    protected readonly selectedBookingLabel = computed(() => {
        const id = this.bookingId();
        if (id == null)
            return '';
        const b = this.completedBookings().find((x) => Number(x.bookingID) === id);
        if (!b)
            return '';
        const desk = b.deskCode || b.deskID;
        const place = b.spaceName || b.buildingName || 'Spazio';
        const end = WorkerReviewsPageComponent.safeFormatShort(b.endTime);
        const ref = b.bookingCode.trim() || String(b.bookingID);
        return `#${ref} · ${place} · desk ${desk} · fine ${end}`;
    });
    private static safeFormatShort(iso: string): string {
        const raw = (iso ?? '').trim();
        if (!raw)
            return '—';
        try {
            return formatDate(raw, 'short', 'it-IT');
        }
        catch {
            return raw;
        }
    }
    protected readonly officeOptions = computed(() => {
        const map = new Map<number, string>();
        for (const r of this.historyReviews()) {
            const parts = [r.spaceName?.trim(), r.city?.trim()].filter((p): p is string => !!p);
            const base = parts.length ? parts.join(' · ') : `Ufficio #${r.spaceID}`;
            const code = r.spaceOfficeCode?.trim();
            const label = code ? `${base} · codice ${code}` : base;
            map.set(r.spaceID, label);
        }
        return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1], 'it'));
    });
    constructor() {
        this.destroyRef.onDestroy(() => {
            this.closeBookingPicker();
        });
    }
    protected startEdit(review: Review): void {
        this.editingReviewId.set(review.reviewID);
        this.bookingId.set(review.bookingID);
        this.rating.set(review.rating);
        this.commentText.set(review.comment);
        this.successMsg.set('');
        this.submitError.set('');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    protected cancelEdit(): void {
        this.editingReviewId.set(null);
        this.bookingId.set(null);
        this.rating.set(5);
        this.commentText.set('');
    }
    protected onMdbTabChange(change: MdbTabChange): void {
        const tabs: Array<'all' | 'new' | 'old'> = ['all', 'new', 'old'];
        this.activeHistoryTab.set(tabs[change.index] ?? 'all');
    }
    protected readonly bookingPublicRef = bookingPublicRef;
    protected readonly reviewBookingPublicRef = bookingPublicRef;
    protected deleteReview(id: number): void {
        this.confirmService
            .confirm({
            title: 'Elimina recensione',
            message: 'Eliminare questa recensione? L\'azione non può essere annullata.',
            confirmLabel: 'Elimina',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.deletingReviewId.set(id);
            return this.reviewService.deleteReview(id);
        }))
            .subscribe({
            next: () => {
                this.successMsg.set('Recensione eliminata.');
                this.reloadHistory();
                this.reloadEligible();
                this.deletingReviewId.set(null);
            },
            error: (err: Error) => {
                this.submitError.set(err.message);
                this.deletingReviewId.set(null);
            }
        });
    }
    public ngOnInit(): void {
        this.reloadEligible();
        this.reloadHistory();
    }
    protected openBookingPicker(ev: MouseEvent): void {
        ev.stopPropagation();
        if (this.editingReviewId() || !this.completedBookings().length) {
            return;
        }
        this.bookingPickerModalRef?.close();
        this.bookingPickerModalRef = this.modalService.open(this.bookingPickerModal, {
            modalClass: 'modal-dialog-centered modal-dialog-scrollable',
            ignoreBackdropClick: false
        });
    }
    protected closeBookingPicker(): void {
        this.bookingPickerModalRef?.close();
        this.bookingPickerModalRef = null;
    }
    protected pickBooking(b: Booking): void {
        const id = Number(b.bookingID);
        if (!Number.isFinite(id)) {
            return;
        }
        this.bookingId.set(id);
        this.closeBookingPicker();
    }
    protected isBookingOptionSelected(b: Booking): boolean {
        return this.bookingId() === Number(b.bookingID);
    }
    protected canSubmitNow(): boolean {
        const id = this.bookingId();
        const c = this.commentText().trim();
        const rt = this.rating();
        return (id != null &&
            c.length >= this.COMMENT_MIN &&
            c.length <= this.COMMENT_MAX &&
            rt >= 1 &&
            rt <= 5);
    }
    protected reloadEligible(): void {
        this.loadingEligible.set(true);
        this.eligibleLoadError.set('');
        this.bookingService
            .getMyReviewEligibleBookings()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                this.completedBookings.set(rows);
                this.loadingEligible.set(false);
                const sel = this.bookingId();
                if (sel != null && !rows.some((b) => Number(b.bookingID) === sel)) {
                    this.bookingId.set(null);
                }
            },
            error: (err: Error) => {
                this.eligibleLoadError.set(err.message);
                this.completedBookings.set([]);
                this.loadingEligible.set(false);
            }
        });
    }
    protected reloadHistory(): void {
        this.historyLoading.set(true);
        this.historyLoadError.set('');
        this.reviewService
            .getMyReviewHistory()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                this.historyReviews.set(rows);
                this.historyLoading.set(false);
            },
            error: (err: Error) => {
                this.historyReviews.set([]);
                this.historyLoading.set(false);
                this.historyLoadError.set(err.message);
            }
        });
    }
    protected setRating(value: number): void {
        this.rating.set(Math.max(1, Math.min(5, value)));
    }
    protected onStarsKeydown(ev: KeyboardEvent): void {
        if (ev.key === 'ArrowRight' || ev.key === 'ArrowUp') {
            ev.preventDefault();
            this.setRating(Math.min(5, this.rating() + 1));
        }
        else if (ev.key === 'ArrowLeft' || ev.key === 'ArrowDown') {
            ev.preventDefault();
            this.setRating(Math.max(1, this.rating() - 1));
        }
    }
    protected submit(): void {
        const bid = this.bookingId();
        const eid = this.editingReviewId();
        if (!this.canSubmitNow() || bid == null)
            return;
        this.submitError.set('');
        this.successMsg.set('');
        const obs = eid
            ? this.reviewService.updateReview(eid, { rating: this.rating(), comment: this.commentText().trim() })
            : this.reviewService.leaveReview({ bookingID: bid, rating: this.rating(), comment: this.commentText().trim() });
        obs.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                const msg = eid ? 'Recensione aggiornata.' : 'Recensione inviata.';
                this.successMsg.set(msg);
                this.toast.success(msg);
                this.commentText.set('');
                this.bookingId.set(null);
                this.editingReviewId.set(null);
                this.rating.set(5);
                this.reloadEligible();
                this.reloadHistory();
            },
            error: (err: Error) => {
                this.submitError.set(err.message);
                this.toast.error(err.message || 'Impossibile inviare la recensione.');
            }
        });
    }
    protected locationLine(review: Review): string {
        const parts = [review.spaceName?.trim(), review.city?.trim()].filter((p): p is string => !!p);
        const place = parts.length ? parts.join(' · ') : `Spazio #${review.spaceID}`;
        const code = review.spaceOfficeCode?.trim();
        return code ? `${place} · codice ${code}` : place;
    }
    protected readonly formatReviewDate = formatReviewDate;
}
