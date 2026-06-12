import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, ViewEncapsulation, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { ActivatedRoute } from '@angular/router';
import { filter, finalize, switchMap } from 'rxjs/operators';
import { Review } from '../../../../core/models';
import { AdminService } from '../../../../core/services/admin.service';
import { formatReviewDate } from '../../../../core/utils/date.util';
import { normalizeForSearch } from '../../../../core/utils/search.util';
import { ReviewService } from '../../../../core/services/review.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
export type AdminSpaceRow = {
    spaceID: number;
    officeCode: string;
    name: string;
    city: string;
    address?: string;
    approved: boolean;
    deskCount: number;
    hostName?: string;
};
@Component({
    selector: 'app-admin-reviews-page',
    standalone: true,
    encapsulation: ViewEncapsulation.None,
    imports: [CommonModule, FormsModule, MdbFormsModule, EmptyStateComponent, MdbRippleModule, MdbCollapseModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './admin-reviews-page.component.html',
    styleUrl: './admin-reviews-page.component.scss'
})
export class AdminReviewsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    private readonly reviewService = inject(ReviewService);
    private readonly confirmService = inject(ConfirmModalService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Recensioni uffici');
    protected readonly spaces = signal<AdminSpaceRow[]>([]);
    protected readonly loadingSpaces = signal(false);
    protected readonly reviews = signal<Review[]>([]);
    protected readonly loadingReviews = signal(false);
    protected readonly errorMsg = signal('');
    protected readonly pickerCity = signal('');
    protected readonly pickerCitySearchQuery = signal('');
    protected readonly pickerSearchQuery = signal('');
    protected readonly spacePickerOpen = signal(false);
    protected readonly selectedSpace = signal<AdminSpaceRow | null>(null);
    protected readonly stars = [1, 2, 3, 4, 5] as const;
    protected readonly cities = computed(() => {
        const set = new Set<string>();
        for (const s of this.spaces()) {
            const c = (s.city ?? '').trim();
            if (c)
                set.add(c);
        }
        return [...set].sort((a, b) => a.localeCompare(b, 'it'));
    });
    protected readonly filteredPickerCities = computed(() => {
        const all = this.cities();
        const raw = this.pickerCitySearchQuery().trim();
        if (!raw) {
            return all;
        }
        const tokens = raw
            .split(/\s+/u)
            .map((t) => normalizeForSearch(t))
            .filter(Boolean);
        if (!tokens.length) {
            return all;
        }
        return all.filter((city) => {
            const hay = normalizeForSearch(city);
            return tokens.every((tok) => hay.includes(tok));
        });
    });
    protected readonly filteredPickerSpaces = computed(() => {
        const city = this.pickerCity().trim();
        const raw = this.pickerSearchQuery().trim();
        let rows = this.spaces().filter((s) => (s.city ?? '').trim().toLowerCase() === city.toLowerCase());
        if (!raw) {
            return [...rows].sort((a, b) => a.name.localeCompare(b.name, 'it'));
        }
        const tokens = raw
            .split(/\s+/u)
            .map((t) => normalizeForSearch(t))
            .filter(Boolean);
        if (!tokens.length) {
            return [...rows].sort((a, b) => a.name.localeCompare(b.name, 'it'));
        }
        rows = rows.filter((s) => {
            const hay = normalizeForSearch([s.name, s.city, s.address ?? '', s.hostName ?? '', s.officeCode ?? '', String(s.spaceID), String(s.deskCount)].join(' '));
            return tokens.every((tok) => hay.includes(tok));
        });
        return rows.sort((a, b) => a.name.localeCompare(b.name, 'it'));
    });
    protected readonly selectedSpaceLabel = computed(() => {
        const s = this.selectedSpace();
        if (!s)
            return '';
        const code = (s.officeCode ?? '').trim();
        const parts = [s.name, s.city].filter(Boolean);
        const head = parts.length ? parts.join(' · ') : 'Ufficio';
        return code ? `${head} · codice ${code}` : `${head} (#${s.spaceID})`;
    });
    public ngOnInit(): void {
        this.loadSpaces();
    }
    protected selectPickerCity(city: string): void {
        this.pickerCity.set(city);
        this.pickerSearchQuery.set('');
    }
    protected toggleSpacePicker(ev: MouseEvent): void {
        ev.stopPropagation();
        if (this.loadingSpaces() || !this.spaces().length) {
            return;
        }
        const willOpen = !this.spacePickerOpen();
        if (willOpen) {
            this.pickerCitySearchQuery.set('');
            const sel = this.selectedSpace();
            const cFromSel = sel?.city?.trim();
            if (cFromSel) {
                this.pickerCity.set(cFromSel);
            }
            else if (!this.pickerCity().trim() && this.cities().length) {
                this.pickerCity.set(this.cities()[0]);
            }
        }
        this.spacePickerOpen.update((v) => !v);
    }
    protected closeSpacePicker(): void {
        this.spacePickerOpen.set(false);
    }
    protected pickSpace(space: AdminSpaceRow): void {
        this.selectedSpace.set(space);
        this.closeSpacePicker();
        this.loadReviews();
    }
    private loadSpaces(): void {
        this.loadingSpaces.set(true);
        this.errorMsg.set('');
        this.adminService
            .getSpaces()
            .pipe(finalize(() => this.loadingSpaces.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => this.spaces.set(rows.map((raw) => ({
                spaceID: Number(raw['spaceID']),
                officeCode: String(raw['officeCode'] ?? '').trim(),
                name: String(raw['name'] ?? ''),
                city: String(raw['city'] ?? ''),
                address: raw['address'] != null ? String(raw['address']) : '',
                approved: Boolean(raw['approved']),
                deskCount: Number(raw['deskCount'] ?? 0),
                hostName: raw['hostName'] != null ? String(raw['hostName']) : ''
            }))),
            error: (err: Error) => {
                this.spaces.set([]);
                this.errorMsg.set(err.message);
            }
        });
    }
    protected loadReviews(): void {
        const s = this.selectedSpace();
        if (!s)
            return;
        this.loadingReviews.set(true);
        this.errorMsg.set('');
        this.reviewService
            .getAdminSpaceReviews(s.spaceID)
            .pipe(finalize(() => this.loadingReviews.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => this.reviews.set(rows),
            error: (err: Error) => this.errorMsg.set(err.message)
        });
    }
    protected deleteReview(r: Review): void {
        this.confirmService
            .confirm({
            title: 'Elimina recensione',
            message: `Eliminare la recensione #${r.reviewID}? L’azione non può essere annullata.`,
            confirmLabel: 'Elimina',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg.set('');
            return this.reviewService.deleteReviewAsAdmin(r.reviewID);
        }))
            .subscribe({
            next: () => {
                this.loadReviews();
            },
            error: (err: Error) => this.errorMsg.set(err.message)
        });
    }
    @HostListener('document:keydown.escape')
    protected onEscape(): void {
        if (this.spacePickerOpen()) {
            this.closeSpacePicker();
        }
    }
    protected readonly formatReviewDate = formatReviewDate;
}
