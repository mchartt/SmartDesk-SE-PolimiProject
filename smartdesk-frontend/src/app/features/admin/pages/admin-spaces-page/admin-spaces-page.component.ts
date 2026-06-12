import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { EMPTY } from 'rxjs';
import { filter, switchMap } from 'rxjs/operators';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { AdminService } from '../../../../core/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { haystackMatchesTokenSearch, normalizeForSearch } from '../../../../core/utils/search.util';
type SpaceRow = {
    spaceID?: number;
    id?: number;
    name: string;
    city?: string;
    address?: string;
    approved: boolean;
    deskCount?: number;
    hostName?: string;
    hostGivenName?: string;
    hostFamilyName?: string;
    hostEmail?: string;
    hostVatNumber?: string;
    averageReviewRating?: number | null;
    description?: string;
    officeCode?: string;
};
type CityTile = {
    cityKey: string;
    label: string;
    count: number;
};
const CITY_NONE_KEY = '__NONE__';
function cityGroupKey(city: string | undefined): string {
    const t = (city ?? '').trim();
    return t ? t.toLowerCase() : CITY_NONE_KEY;
}
function cityDisplayLabel(city: string | undefined): string {
    const t = (city ?? '').trim();
    return t ? t : 'Senza città';
}
@Component({
    standalone: true,
    imports: [CommonModule, FormsModule, MdbFormsModule, EmptyStateComponent, MdbCollapseModule, MdbRippleModule, SdIconComponent],
    templateUrl: './admin-spaces-page.component.html',
    styleUrl: './admin-spaces-page.component.scss'
})
export class AdminSpacesPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    private readonly notifications = inject(NotificationService);
    private readonly confirmService = inject(ConfirmModalService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Spazi');
    protected spaces = signal<SpaceRow[]>([]);
    protected errorMsg = '';
    protected readonly mainCityFilterQuery = signal('');
    protected readonly mainOfficeFilterQuery = signal('');
    protected readonly expandedSpaceDetailIds = signal(new Set<number>());
    protected readonly ratingStarIndexes = [1, 2, 3, 4, 5] as const;
    protected readonly cityTiles = computed((): CityTile[] => {
        const map = new Map<string, {
            label: string;
            count: number;
        }>();
        for (const s of this.spaces()) {
            const key = cityGroupKey(s.city);
            const label = cityDisplayLabel(s.city);
            const cur = map.get(key);
            if (cur) {
                cur.count += 1;
            }
            else {
                map.set(key, { label, count: 1 });
            }
        }
        const rows: CityTile[] = [...map.entries()].map(([cityKey, v]) => ({
            cityKey,
            label: v.label,
            count: v.count
        }));
        rows.sort((a, b) => {
            if (a.cityKey === CITY_NONE_KEY) {
                return 1;
            }
            if (b.cityKey === CITY_NONE_KEY) {
                return -1;
            }
            return a.label.localeCompare(b.label, 'it', { sensitivity: 'base' });
        });
        return rows;
    });
    protected readonly visibleCityTiles = computed(() => {
        const tiles = this.cityTiles();
        const cityRaw = this.mainCityFilterQuery().trim();
        const officeRaw = this.mainOfficeFilterQuery().trim();
        let filtered = tiles;
        if (cityRaw) {
            const tokens = normalizeForSearch(cityRaw)
                .split(/\s+/u)
                .filter(Boolean);
            if (tokens.length) {
                filtered = filtered.filter((t: CityTile) => {
                    const hay = normalizeForSearch(`${t.label} ${t.cityKey}`);
                    return tokens.every((tok) => hay.includes(tok));
                });
            }
        }
        if (officeRaw) {
            const matchingCityKeys = new Set<string>();
            for (const s of this.spaces()) {
                if (AdminSpacesPageComponent.matchesOfficeNameQuickSearch(s, officeRaw)) {
                    matchingCityKeys.add(cityGroupKey(s.city));
                }
            }
            filtered = filtered.filter((t: CityTile) => matchingCityKeys.has(t.cityKey));
        }
        return filtered;
    });
    protected readonly hasMainFilterNoResults = computed(() => this.spaces().length > 0 && this.cityTiles().length > 0 && this.visibleCityTiles().length === 0);
    protected getSpacesForCity(cityKey: string): SpaceRow[] {
        const query = this.mainOfficeFilterQuery().trim();
        return this.spaces()
            .filter((s) => cityGroupKey(s.city) === cityKey)
            .filter((s) => AdminSpacesPageComponent.matchesSpaceSearch(s, query))
            .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '', 'it', { sensitivity: 'base' }));
    }
    public ngOnInit(): void {
        this.load();
    }
    private static matchesOfficeNameQuickSearch(space: SpaceRow, rawQuery: string): boolean {
        const q = rawQuery.trim();
        if (!q) {
            return true;
        }
        return haystackMatchesTokenSearch([space.name, space.officeCode ?? ''].join(' '), q);
    }
    private static matchesSpaceSearch(space: SpaceRow, rawQuery: string): boolean {
        const q = rawQuery.trim();
        if (!q) {
            return true;
        }
        return haystackMatchesTokenSearch([
            space.name,
            space.city ?? '',
            space.address ?? '',
            space.hostName ?? '',
            space.hostGivenName ?? '',
            space.hostFamilyName ?? '',
            space.hostEmail ?? '',
            space.hostVatNumber ?? '',
            space.officeCode ?? '',
            String(space.deskCount ?? 0),
            String(space.spaceID ?? space.id ?? ''),
            space.description ?? '',
            space.averageReviewRating != null ? String(space.averageReviewRating) : ''
        ].join(' '), q);
    }
    protected load(): void {
        this.errorMsg = '';
        this.adminService
            .getApprovedSpacesEnriched()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows: unknown) => {
                const mapped = (rows as Record<string, unknown>[]).map((raw) => {
                    let avg: number | null = null;
                    const rv = raw['averageReviewRating'];
                    if (rv != null && rv !== '') {
                        const n = Number(rv);
                        if (Number.isFinite(n)) {
                            avg = n;
                        }
                    }
                    return {
                        spaceID: raw['spaceID'] != null ? Number(raw['spaceID']) : undefined,
                        id: raw['id'] != null ? Number(raw['id']) : undefined,
                        name: String(raw['name'] ?? ''),
                        city: raw['city'] != null ? String(raw['city']) : '',
                        address: raw['address'] != null ? String(raw['address']) : '',
                        approved: Boolean(raw['approved']),
                        deskCount: Number(raw['deskCount'] ?? 0),
                        hostName: raw['hostName'] != null ? String(raw['hostName']) : '',
                        hostGivenName: raw['hostGivenName'] != null ? String(raw['hostGivenName']) : '',
                        hostFamilyName: raw['hostFamilyName'] != null ? String(raw['hostFamilyName']) : '',
                        hostEmail: raw['hostEmail'] != null ? String(raw['hostEmail']) : '',
                        hostVatNumber: raw['hostVatNumber'] != null ? String(raw['hostVatNumber']) : '',
                        averageReviewRating: avg,
                        description: raw['description'] != null ? String(raw['description']) : '',
                        officeCode: raw['officeCode'] != null ? String(raw['officeCode']).trim() : ''
                    };
                });
                this.spaces.set(mapped);
            },
            error: (err: Error) => {
                this.spaces.set([]);
                this.errorMsg = err.message;
            }
        });
    }
    protected spaceTrackId(space: SpaceRow): string {
        const n = space.spaceID ?? space.id;
        return n != null ? String(n) : space.name + (space.address ?? '');
    }
    protected numericSpaceId(space: SpaceRow): number {
        return Number(space.spaceID ?? space.id ?? 0);
    }
    protected toggleSpaceDetails(space: SpaceRow): void {
        const id = this.numericSpaceId(space);
        if (!id) {
            return;
        }
        this.expandedSpaceDetailIds.update((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            }
            else {
                next.add(id);
            }
            return next;
        });
    }
    protected spaceDetailsExpanded(space: SpaceRow): boolean {
        return this.expandedSpaceDetailIds().has(this.numericSpaceId(space));
    }
    protected starFillPercent(starIndex: number, rating: number | null | undefined): number {
        if (rating == null || rating <= 0) {
            return 0;
        }
        const r = Math.min(5, Math.max(0, rating));
        const slice = r - (starIndex - 1);
        return Math.min(100, Math.max(0, slice * 100));
    }
    protected formatRatingIt(r: number | null | undefined): string {
        if (r == null) {
            return '';
        }
        return r.toLocaleString('it-IT', { minimumFractionDigits: 1, maximumFractionDigits: 2 });
    }
    protected hostEmailDisplay(space: SpaceRow): string {
        const e = (space.hostEmail ?? '').trim();
        return e || '—';
    }
    protected hostGivenDisplay(space: SpaceRow): string {
        const g = (space.hostGivenName ?? '').trim();
        if (g) {
            return g;
        }
        return this.hostNameFallbackPart(space, 0);
    }
    protected hostFamilyDisplay(space: SpaceRow): string {
        const f = (space.hostFamilyName ?? '').trim();
        if (f) {
            return f;
        }
        return this.hostNameFallbackPart(space, 1);
    }
    private hostNameFallbackPart(space: SpaceRow, index: 0 | 1): string {
        const raw = (space.hostName ?? '').trim();
        if (!raw) {
            return '—';
        }
        const parts = raw.split(/\s+/u).filter(Boolean);
        if (!parts.length) {
            return '—';
        }
        if (index === 0) {
            return parts[0] ?? '—';
        }
        return parts.length > 1 ? parts.slice(1).join(' ') : '—';
    }
    protected descriptionDisplay(space: SpaceRow): string {
        const d = (space.description ?? '').trim();
        return d || '—';
    }
    protected vatDisplay(space: SpaceRow): string {
        const v = (space.hostVatNumber ?? '').trim();
        return v || '—';
    }
    protected moderate(space: SpaceRow, action: 'APPROVE' | 'REJECT' | 'FORCE_CLOSE'): void {
        const name = space.name ?? '';
        const titles: Record<typeof action, string> = {
            APPROVE: 'Approva spazio',
            REJECT: 'Rifiuta spazio',
            FORCE_CLOSE: 'Chiusura forzata spazio'
        };
        const messages: Record<typeof action, string> = {
            APPROVE: `Approvare lo spazio «${name}»?`,
            REJECT: `Rifiutare lo spazio «${name}»?`,
            FORCE_CLOSE: `Forzare la chiusura dello spazio «${name}»? Le prenotazioni attive potrebbero essere compromesse.`
        };
        const labels: Record<typeof action, string> = {
            APPROVE: 'Approva',
            REJECT: 'Rifiuta',
            FORCE_CLOSE: 'Chiudi'
        };
        const variants: Record<typeof action, 'success' | 'danger' | 'warning'> = {
            APPROVE: 'success',
            REJECT: 'danger',
            FORCE_CLOSE: 'warning'
        };
        this.confirmService
            .confirm({
            title: titles[action],
            message: messages[action],
            confirmLabel: labels[action],
            cancelLabel: 'Annulla',
            variant: variants[action]
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg = '';
            const spaceId = space.id ?? space.spaceID;
            if (spaceId == null) {
                this.errorMsg = 'ID spazio non valido.';
                return EMPTY;
            }
            return this.adminService.approveSpace(spaceId, action);
        }))
            .subscribe({
            next: () => {
                this.notifications.requestRefresh();
                this.load();
            },
            error: (err: Error) => (this.errorMsg = err.message)
        });
    }
}
