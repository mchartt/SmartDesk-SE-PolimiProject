export function pendingHostRegisteredAtMs(row: Record<string, unknown>): number {
    const r = row['registeredAt'];
    if (r == null)
        return 0;
    if (typeof r === 'string') {
        const t = Date.parse(r);
        return Number.isNaN(t) ? 0 : t;
    }
    if (Array.isArray(r)) {
        const y = Number(r[0]);
        const mo = Number(r[1] ?? 1);
        const d = Number(r[2] ?? 1);
        const hh = Number(r[3] ?? 0);
        const mi = Number(r[4] ?? 0);
        const s = Number(r[5] ?? 0);
        if (!y)
            return 0;
        return new Date(y, mo - 1, d, hh, mi, s).getTime();
    }
    return 0;
}
export function sortPendingHostsNewestFirst(rows: Array<Record<string, unknown>>): Array<Record<string, unknown>> {
    return [...rows].sort((a, b) => pendingHostRegisteredAtMs(b) - pendingHostRegisteredAtMs(a));
}
