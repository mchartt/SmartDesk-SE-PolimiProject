import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { formatReviewDate } from '../../../../core/utils/date.util';
import { HostService } from '../../../../core/services/host.service';
import { Review, Space } from '../../../../core/models';
interface HostReviewSpaceSummary {
    spaceID: number;
    spaceDisplayName: string;
    detailLine: string;
    reviews: Review[];
    nuoveCount: number;
    storicoCount: number;
}
@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MdbFormsModule,
        MdbRippleModule,
        MdbCollapseModule,
        SdModalHeaderComponent,
        EmptyStateComponent,
        SdIconComponent
    ],
    templateUrl: './host-reviews-page.component.html',
    styleUrl: './host-reviews-page.component.scss'
})
export class HostReviewsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    protected readonly SNIPPET_LEN = 78;
    protected readonly stars = [1, 2, 3, 4, 5] as const;
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Recensioni');
    protected readonly reviews = signal<Review[]>([]);
    protected readonly hostSpaces = signal<Space[]>([]);
    protected readonly loading = signal(true);
    protected readonly errorMsg = signal('');
    protected readonly spaceSearchQuery = signal('');
    protected readonly officeNuoveQuery = signal('');
    protected readonly officeStoricoQuery = signal('');
    protected readonly officeOverlaySpaceId = signal<number | null>(null);
    protected readonly officeSheetTab = signal<'nuove' | 'storico'>('nuove');
    protected readonly sheetError = signal('');
    private readonly pendingReadReviews = new Map<number, Review>();
    protected readonly spacesById = computed(() => {
        const m = new Map<number, Space>();
        for (const sp of this.hostSpaces()) {
            m.set(sp.spaceID, sp);
        }
        return m;
    });
    protected readonly spaceSummaries = computed((): HostReviewSpaceSummary[] => {
        const catalog = this.spacesById();
        const bySpace = new Map<number, Review[]>();
        for (const r of this.reviews()) {
            const list = bySpace.get(r.spaceID) ?? [];
            list.push(r);
            bySpace.set(r.spaceID, list);
        }
        const out: HostReviewSpaceSummary[] = [];
        for (const [spaceID, list] of bySpace) {
            const first = list[0];
            const spMeta = catalog.get(spaceID);
            const rawName = first.spaceName?.trim() || spMeta?.name?.trim() || '';
            const spaceDisplayName = rawName ? rawName : `Spazio #${spaceID}`;
            const city = first.city?.trim() || spMeta?.city?.trim() || '';
            const code = first.spaceOfficeCode?.trim() || spMeta?.officeCode?.trim() || '';
            const tailParts = [city ? city : null, code ? `Codice ${code}` : null].filter((p): p is string => !!p);
            const detailLine = tailParts.length
                ? tailParts.join(' · ')
                : `${list.length} recension${list.length === 1 ? 'e' : 'i'}`;
            const nuoveCount = list.filter((x) => !x.seenByHost).length;
            const storicoCount = list.filter((x) => x.seenByHost).length;
            out.push({ spaceID, spaceDisplayName, detailLine, reviews: list, nuoveCount, storicoCount });
        }
        out.sort((a, b) => a.spaceDisplayName.localeCompare(b.spaceDisplayName, 'it'));
        return out;
    });
    protected readonly filteredSpaceSummaries = computed(() => {
        const q = HostReviewsPageComponent.normalizeQuery(this.spaceSearchQuery());
        const rows = this.spaceSummaries();
        const catalog = this.spacesById();
        if (!q)
            return rows;
        return rows.filter((row) => {
            const sp = catalog.get(row.spaceID);
            const hay = [
                row.spaceDisplayName,
                row.detailLine,
                String(row.spaceID),
                sp?.name,
                sp?.city,
                sp?.officeCode,
                sp?.address,
                sp?.description
            ]
                .filter((x): x is string => typeof x === 'string' && x.trim().length > 0)
                .join(' ');
            return HostReviewsPageComponent.matchesNormalized(hay, q);
        });
    });
    protected readonly officeModal = computed(() => {
        const id = this.officeOverlaySpaceId();
        if (id == null)
            return null;
        return this.spaceSummaries().find((s) => s.spaceID === id) ?? null;
    });
    protected readonly nuoveInOffice = computed(() => {
        const m = this.officeModal();
        if (!m)
            return [];
        return [...m.reviews].filter((r) => !r.seenByHost).sort(HostReviewsPageComponent.sortReviewsDesc);
    });
    protected readonly storicoInOffice = computed(() => {
        const m = this.officeModal();
        if (!m)
            return [];
        return [...m.reviews].filter((r) => r.seenByHost).sort(HostReviewsPageComponent.sortReviewsDesc);
    });
    protected readonly nuoveInOfficeFiltered = computed(() => {
        const q = this.officeNuoveQuery();
        return this.nuoveInOffice().filter((r) => this.reviewMatchesQuery(r, q));
    });
    protected readonly storicoInOfficeFiltered = computed(() => {
        const q = this.officeStoricoQuery();
        return this.storicoInOffice().filter((r) => this.reviewMatchesQuery(r, q));
    });
    private static normalizeQuery(raw: string): string {
        return raw
            .trim()
            .toLowerCase()
            .normalize('NFD')
            .replace(/\p{M}/gu, '');
    }
    private static matchesNormalized(haystack: string, normalizedNeedle: string): boolean {
        if (!normalizedNeedle)
            return true;
        const h = haystack
            .toLowerCase()
            .normalize('NFD')
            .replace(/\p{M}/gu, '');
        return h.includes(normalizedNeedle);
    }
    private reviewMatchesQuery(r: Review, raw: string): boolean {
        const q = HostReviewsPageComponent.normalizeQuery(raw);
        if (!q)
            return true;
        const hay = [
            this.reviewerLabel(r),
            r.comment ?? '',
            String(r.rating),
            String(r.reviewID)
        ].join(' ');
        return HostReviewsPageComponent.matchesNormalized(hay, q);
    }
    private static sortReviewsDesc(a: Review, b: Review): number {
        return HostReviewsPageComponent.parseCreated(b.createdAt) - HostReviewsPageComponent.parseCreated(a.createdAt);
    }
    private static parseCreated(iso: string | null): number {
        if (!iso)
            return 0;
        const t = Date.parse(iso);
        return Number.isFinite(t) ? t : 0;
    }
    public ngOnInit(): void {
        forkJoin({
            reviews: this.hostService.getReviews(),
            spaces: this.hostService.getSpaces().pipe(catchError(() => of([] as Space[])))
        })
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false)))
            .subscribe({
            next: ({ reviews, spaces }) => {
                this.reviews.set(reviews);
                this.hostSpaces.set(spaces);
                this.errorMsg.set('');
            },
            error: (err: Error) => this.errorMsg.set(err.message ?? 'Errore di caricamento.')
        });
    }
    protected reviewerLabel(r: Review): string {
        const name = r.reviewerFullName.trim();
        return name || `Utente #${r.workerID}`;
    }
    protected snippet(text: string): string {
        const t = (text ?? '').trim();
        if (t.length <= this.SNIPPET_LEN)
            return t;
        return `${t.slice(0, this.SNIPPET_LEN).trimEnd()}…`;
    }
    protected openOffice(spaceID: number, tab: 'nuove' | 'storico'): void {
        this.sheetError.set('');
        this.officeNuoveQuery.set('');
        this.officeStoricoQuery.set('');
        this.officeSheetTab.set(tab);
        this.officeOverlaySpaceId.set(spaceID);
    }
    protected closeOffice(): void {
        if (this.pendingReadReviews.size > 0) {
            this.reviews.update((list) => list.map((r) => this.pendingReadReviews.get(r.reviewID) || r));
            this.pendingReadReviews.clear();
        }
        this.officeOverlaySpaceId.set(null);
        this.sheetError.set('');
        this.officeSheetTab.set('nuove');
        this.officeNuoveQuery.set('');
        this.officeStoricoQuery.set('');
    }
    protected toggleDetail(review: Review): void {
        this.sheetError.set('');
        if (!review.seenByHost && !this.pendingReadReviews.has(review.reviewID)) {
            this.pendingReadReviews.set(review.reviewID, review);
            this.hostService
                .markReviewSeenByHost(review.spaceID, review.reviewID)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: (updated) => {
                    this.pendingReadReviews.set(updated.reviewID, updated);
                },
                error: () => {
                    this.pendingReadReviews.delete(review.reviewID);
                    this.sheetError.set('Impossibile registrare la visualizzazione. Riprova.');
                }
            });
        }
    }
    private mergeReview(updated: Review): void {
        this.reviews.update((list) => list.map((r) => (r.reviewID === updated.reviewID ? updated : r)));
    }
    protected readonly formatReviewDate = formatReviewDate;
}
