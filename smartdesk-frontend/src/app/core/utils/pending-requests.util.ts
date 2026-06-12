import { pendingHostRegisteredAtMs, sortPendingHostsNewestFirst } from './pending-hosts.util';
import { pendingSpaceSortKey, sortPendingSpacesNewestFirst } from './pending-spaces.util';
export type PendingRequestKind = 'host' | 'space';
export interface PendingRequestPreviewRow {
    kind: PendingRequestKind;
    id: number;
    name: string;
    subtitle: string;
    registeredLabel: string;
    sortKey: number;
}
function hostDisplayName(row: Record<string, unknown>): string {
    const parts = [row['name'], row['surname']].map((v) => String(v ?? '').trim()).filter(Boolean);
    return parts.length ? parts.join(' ') : '—';
}
function spaceSubtitle(row: Record<string, unknown>): string {
    const city = String(row['city'] ?? '').trim();
    const host = String(row['hostName'] ?? '').trim();
    const code = String(row['officeCode'] ?? '').trim();
    const bits = [city, host].filter(Boolean);
    if (bits.length) {
        return bits.join(' · ');
    }
    return code || '—';
}
function formatRegisteredLabel(ms: number): string {
    if (!ms) {
        return '—';
    }
    return new Intl.DateTimeFormat('it-IT', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(ms));
}
export function mergePendingRequestsPreview(hosts: Array<Record<string, unknown>>, spaces: Array<Record<string, unknown>>, limit: number): PendingRequestPreviewRow[] {
    const hostRows: PendingRequestPreviewRow[] = sortPendingHostsNewestFirst(hosts).map((row) => {
        const ms = pendingHostRegisteredAtMs(row);
        return {
            kind: 'host',
            id: Number(row['userID'] ?? row['id'] ?? 0),
            name: hostDisplayName(row),
            subtitle: String(row['email'] ?? '—'),
            registeredLabel: formatRegisteredLabel(ms),
            sortKey: ms
        };
    });
    const spaceRows: PendingRequestPreviewRow[] = sortPendingSpacesNewestFirst(spaces).map((row) => ({
        kind: 'space',
        id: Number(row['spaceID'] ?? row['id'] ?? 0),
        name: String(row['name'] ?? '—'),
        subtitle: spaceSubtitle(row),
        registeredLabel: '—',
        sortKey: pendingSpaceSortKey(row)
    }));
    return [...hostRows, ...spaceRows]
        .sort((a, b) => b.sortKey - a.sortKey)
        .slice(0, Math.max(0, limit));
}
