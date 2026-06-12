export function pendingSpaceSortKey(row: Record<string, unknown>): number {
    return Number(row['spaceID'] ?? row['id'] ?? 0);
}
export function sortPendingSpacesNewestFirst(rows: Array<Record<string, unknown>>): Array<Record<string, unknown>> {
    return [...rows].sort((a, b) => pendingSpaceSortKey(b) - pendingSpaceSortKey(a));
}
