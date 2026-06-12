export function normalizeForSearch(value: string): string {
    return value
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');
}
export function haystackMatchesTokenSearch(haystack: string, rawQuery: string): boolean {
    const tokens = normalizeForSearch(rawQuery)
        .split(/\s+/u)
        .filter(Boolean);
    if (!tokens.length) {
        return true;
    }
    const hay = normalizeForSearch(haystack);
    return tokens.every((tok) => hay.includes(tok));
}
