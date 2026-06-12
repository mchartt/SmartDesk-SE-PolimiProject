import { localCalendarDateIsoFromDate } from './date.util';
export const SEARCH_CALENDAR_MONTHS_IT = [
    'Gennaio',
    'Febbraio',
    'Marzo',
    'Aprile',
    'Maggio',
    'Giugno',
    'Luglio',
    'Agosto',
    'Settembre',
    'Ottobre',
    'Novembre',
    'Dicembre'
] as const;
export const SEARCH_CALENDAR_DOW_IT = ['L', 'M', 'M', 'G', 'V', 'S', 'D'] as const;
export interface SearchCalendarCell {
    key: string;
    iso: string;
    dayNum: string;
    inMonth: boolean;
    isToday: boolean;
    isSelected: boolean;
    hasBooking: boolean;
}
export function calendarCursorFromIso(iso: string): {
    year: number;
    month: number;
} {
    const parts = iso.split('-').map((p) => Number.parseInt(p, 10));
    if (parts.length === 3 && parts.every((n) => Number.isFinite(n))) {
        return { year: parts[0], month: parts[1] };
    }
    const t = new Date();
    return { year: t.getFullYear(), month: t.getMonth() + 1 };
}
export function buildSearchCalendarCells(year: number, month: number, selectedIso: string, bookingDays: ReadonlySet<string>, todayIso: string): SearchCalendarCell[] {
    const first = new Date(year, month - 1, 1);
    const pad = (first.getDay() + 6) % 7;
    const cells: SearchCalendarCell[] = [];
    const gridStart = new Date(year, month - 1, 1 - pad);
    for (let i = 0; i < 42; i++) {
        const d = new Date(gridStart);
        d.setDate(gridStart.getDate() + i);
        const iso = localCalendarDateIsoFromDate(d);
        const inMonth = d.getMonth() === month - 1;
        cells.push({
            key: `${iso}-${i}`,
            iso,
            dayNum: String(d.getDate()),
            inMonth,
            isToday: iso === todayIso,
            isSelected: iso === selectedIso,
            hasBooking: bookingDays.has(iso)
        });
    }
    return cells;
}
