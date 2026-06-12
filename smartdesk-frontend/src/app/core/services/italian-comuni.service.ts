import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of, shareReplay, tap } from 'rxjs';
export interface ItalianComuneRecord {
    label: string;
    n: string;
    s: string;
}
@Injectable({ providedIn: 'root' })
export class ItalianComuniService {
    private readonly http = inject(HttpClient);
    private cache: ItalianComuneRecord[] | null = null;
    private labelSet = new Set<string>();
    private pending$: Observable<ItalianComuneRecord[]> | null = null;
    ensureLoaded(): Observable<ItalianComuneRecord[]> {
        if (this.cache)
            return of(this.cache);
        if (!this.pending$) {
            this.pending$ = this.http.get<ItalianComuneRecord[]>('/data/italian-comuni.json').pipe(tap((rows) => {
                this.cache = rows;
                this.labelSet = new Set(rows.map((r) => r.label));
            }), shareReplay({ bufferSize: 1, refCount: false }));
        }
        return this.pending$;
    }
    isLoaded(): boolean {
        return this.cache !== null;
    }
    normalizeForMatch(text: string): string {
        return text
            .normalize('NFD')
            .replace(/\p{M}/gu, '')
            .toLowerCase()
            .trim();
    }
    filter(query: string, limit = 14): ItalianComuneRecord[] {
        if (!this.cache)
            return [];
        const q = this.normalizeForMatch(query);
        if (!q)
            return [];
        const siglaQuery = query.trim().toUpperCase();
        return this.cache
            .filter((row) => {
            if (this.normalizeForMatch(row.label).includes(q))
                return true;
            if (this.normalizeForMatch(row.n).includes(q))
                return true;
            if (row.s.toUpperCase() === siglaQuery)
                return true;
            return false;
        })
            .slice(0, limit);
    }
    hasExactLabel(value: string): boolean {
        return this.labelSet.has(value.trim());
    }
    tryResolveUniqueNome(input: string): string | null {
        if (!this.cache)
            return null;
        const t = input.trim();
        if (!t)
            return null;
        if (this.labelSet.has(t))
            return t;
        const q = this.normalizeForMatch(t);
        const hits = this.cache.filter((row) => this.normalizeForMatch(row.n) === q);
        if (hits.length === 1)
            return hits[0].label;
        return null;
    }
}
