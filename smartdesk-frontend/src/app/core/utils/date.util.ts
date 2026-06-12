export function localCalendarDateIsoFromDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}
export function localCalendarDateIso(): string {
    return localCalendarDateIsoFromDate(new Date());
}
export function formatCalendarDayLabel(iso: string): string {
    const trimmed = iso.trim();
    if (!trimmed) {
        return '';
    }
    const parts = trimmed.split('-').map((p) => Number.parseInt(p, 10));
    if (parts.length !== 3 || parts.some((n) => !Number.isFinite(n))) {
        return trimmed;
    }
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    if (Number.isNaN(d.getTime())) {
        return trimmed;
    }
    return d.toLocaleDateString('it-IT', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric'
    });
}
export function formatShortDateTime(iso: string | Date | null | undefined): string {
    if (!iso) {
        return '—';
    }
    const d = iso instanceof Date ? iso : new Date(iso);
    if (Number.isNaN(d.getTime())) {
        return '—';
    }
    return d.toLocaleString('it-IT', {
        dateStyle: 'short',
        timeStyle: 'short'
    });
}
export function formatShortDate(iso: string | Date | null | undefined): string {
    if (!iso)
        return '—';
    const d = iso instanceof Date ? iso : new Date(iso);
    if (Number.isNaN(d.getTime()))
        return '—';
    return d.toLocaleDateString('it-IT', { dateStyle: 'short' });
}
export function formatTimeRange(startIso: string, endIso: string): string {
    const s = new Date(startIso);
    const e = new Date(endIso);
    if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime()))
        return '—';
    const fmt = (d: Date) => d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
    return `${fmt(s)} - ${fmt(e)}`;
}
export function isToday(iso: string): boolean {
    const d = new Date(iso);
    const now = new Date();
    return d.getDate() === now.getDate() &&
        d.getMonth() === now.getMonth() &&
        d.getFullYear() === now.getFullYear();
}
export function pad2(n: number): string {
    return String(n).padStart(2, '0');
}
function formatItalianCalendarDate(d: Date): string {
    return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
}
function formatItalianCalendarDateTime(d: Date): string {
    return `${formatItalianCalendarDate(d)} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}
export function formatReviewDate(iso: string | null | undefined): string {
    if (!iso) {
        return '—';
    }
    try {
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) {
            return iso;
        }
        if (!iso.includes('T')) {
            return formatItalianCalendarDate(d);
        }
        return formatItalianCalendarDateTime(d);
    }
    catch {
        return iso;
    }
}
