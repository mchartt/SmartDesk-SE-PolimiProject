import { SlotStatus } from '../../../core/services/booking.service';
import { hmToMinutes, normalizeSlotTimeHm } from '../../../core/utils/time.util';

export const SLOT_LABELS: string[] = (() => {
    const out: string[] = [];
    let mins = 8 * 60;
    for (let i = 0; i < 24; i++) {
        const h = Math.floor(mins / 60);
        const m = mins % 60;
        out.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
        mins += 30;
    }
    return out;
})();

export const SLOT_END_LABELS = SLOT_LABELS.map((_, i) => {
    const mins = 8 * 60 + (i + 1) * 30;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
});

export const SLOT_COUNT = SLOT_LABELS.length;
export const LAST_SLOT_START_MINS = 19 * 60 + 30;

export function parseHmToMins(hm: string): number {
    const t = hm?.trim() ?? '';
    if (!t) {
        return NaN;
    }
    const head = t.split(/\s+/u)[0] ?? t;
    const parts = head.split(':').filter((p) => p !== '');
    if (parts.length < 2) {
        return NaN;
    }
    const h = Number(parts[0]);
    const m = Number(parts[1]);
    if (!Number.isFinite(h) || !Number.isFinite(m)) {
        return NaN;
    }
    return h * 60 + m;
}

export function addMinutesToHm(hm: string, delta: number): string | null {
    const mins = parseHmToMins(hm);
    if (!Number.isFinite(mins)) {
        return null;
    }
    const next = mins + delta;
    if (next < 8 * 60 || next > 20 * 60) {
        return null;
    }
    return minutesToHm(next);
}

export function minutesToHm(total: number): string {
    const h = Math.floor(total / 60);
    const m = total % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

export function formatDuration(totalMins: number): string {
    const h = Math.floor(totalMins / 60);
    const m = totalMins % 60;
    if (h === 0) {
        return `${m}min`;
    }
    if (m === 0) {
        return `${h}h`;
    }
    return `${h}h ${m}min`;
}

export function snapMinutesToNearestGrid(mins: number): number {
    return Math.round(mins / 30) * 30;
}

export function snapStartHm(raw: string): string {
    const parts = raw.trim().split(':');
    const h = Number(parts[0]);
    const m = Number(parts[1] ?? 0);
    if (!Number.isFinite(h) || !Number.isFinite(m)) {
        return '08:00';
    }
    const snapped = Math.min(LAST_SLOT_START_MINS, Math.max(8 * 60, snapMinutesToNearestGrid(h * 60 + m)));
    return minutesToHm(snapped);
}

export function snapEndHmExclusive(raw: string): string {
    const parts = raw.trim().split(':');
    const h = Number(parts[0]);
    const m = Number(parts[1] ?? 0);
    if (!Number.isFinite(h) || !Number.isFinite(m)) {
        return '18:00';
    }
    const snapped = Math.min(20 * 60, Math.max(8 * 60 + 30, snapMinutesToNearestGrid(h * 60 + m)));
    return minutesToHm(snapped);
}

export function endHmAfterSlot(endIdx: number): string {
    const [h, m] = SLOT_LABELS[endIdx].split(':').map((x) => Number(x));
    const mins = h * 60 + m + 30;
    return minutesToHm(mins);
}

export function normalizeApiTime(t: string): string {
    const [hs, ms] = t.trim().split(':');
    const h = parseInt(hs, 10);
    const m = parseInt(ms ?? '0', 10);
    if (Number.isNaN(h)) {
        return t.trim().slice(0, 5);
    }
    return `${String(h).padStart(2, '0')}:${String(Number.isNaN(m) ? 0 : m).padStart(2, '0')}`;
}

export function rowToFreeMask(row: SlotStatus[]): boolean[] {
    const mask = Array(SLOT_COUNT).fill(false);
    for (const cell of row) {
        const label = normalizeApiTime(cell.time);
        const idx = SLOT_LABELS.indexOf(label);
        if (idx >= 0 &&
            String(cell.status ?? '')
                .trim()
                .toLowerCase() === 'free') {
            mask[idx] = true;
        }
    }
    return mask;
}

export function mergeAggregateSlotFree(matrix: SlotStatus[][]): boolean[] {
    const agg = Array(SLOT_COUNT).fill(false);
    for (const row of matrix) {
        const mask = rowToFreeMask(row);
        for (let i = 0; i < SLOT_COUNT; i++) {
            if (mask[i]) {
                agg[i] = true;
            }
        }
    }
    return agg;
}

export function extractIsoTimeHm(iso: string): string {
    const tail = iso.includes('T') ? (iso.split('T')[1] ?? '') : iso;
    return tail.slice(0, 5);
}

export function slotStartLabelsCoveringRange(startIso: string, endIso: string): string[] {
    const startM = hmToMinutes(extractIsoTimeHm(startIso));
    const endM = hmToMinutes(extractIsoTimeHm(endIso));
    if (!Number.isFinite(startM) || !Number.isFinite(endM) || endM <= startM) {
        return [];
    }
    const out: string[] = [];
    for (let m = startM; m < endM; m += 30) {
        out.push(minutesToHm(m));
    }
    return out;
}

export function slotsRangeHasBusyApi(slots: SlotStatus[], startIso: string, endIso: string): boolean {
    const labels = slotStartLabelsCoveringRange(startIso, endIso);
    if (labels.length === 0) {
        return true;
    }
    const byHm = new Map<string, SlotStatus>();
    for (const cell of slots) {
        try {
            byHm.set(normalizeSlotTimeHm(cell.time), cell);
        }
        catch {
        }
    }
    for (const label of labels) {
        const cell = byHm.get(label);
        if (!cell) {
            continue;
        }
        const st = String(cell.status ?? '')
            .trim()
            .toLowerCase();
        if (st === 'busy' || st === 'occupied') {
            return true;
        }
    }
    return false;
}
