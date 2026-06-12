export const DESK_AMENITY_LABELS: Record<string, string> = {
    WIFI: 'WiFi',
    DOUBLE_MONITOR: 'Doppio monitor',
    FAST_INTERNET: 'Internet veloce',
    ERGONOMIC_CHAIR: 'Sedia ergonomica',
    STANDING_DESK: 'Scrivania regolabile in altezza',
    ULTRAWIDE_MONITOR: 'Monitor ultrawide',
    SILENT_MOUSE: 'Mouse silenzioso',
    DOCKING_STATION: 'Docking station',
    NATURAL_LIGHT: 'Luce naturale',
    POWER_OUTLET: 'Presa di corrente'
};
export const DESK_AMENITY_PREVIEW_RANK: readonly string[] = [
    'WIFI',
    'FAST_INTERNET',
    'DOUBLE_MONITOR',
    'ULTRAWIDE_MONITOR',
    'DOCKING_STATION',
    'ERGONOMIC_CHAIR',
    'STANDING_DESK',
    'SILENT_MOUSE',
    'NATURAL_LIGHT',
    'POWER_OUTLET'
];
export function amenityPreviewRank(tag: string): number {
    const index = DESK_AMENITY_PREVIEW_RANK.indexOf(tag);
    return index >= 0 ? index : 1000;
}
export function formatAmenityTag(tag: string): string {
    if (!tag) {
        return '';
    }
    const mapped = DESK_AMENITY_LABELS[tag];
    if (mapped) {
        return mapped;
    }
    return tag
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
export function compareAmenityTags(a: string, b: string): number {
    const rankA = amenityPreviewRank(a);
    const rankB = amenityPreviewRank(b);
    if (rankA !== rankB) {
        return rankA - rankB;
    }
    return formatAmenityTag(a).localeCompare(formatAmenityTag(b), 'it', { sensitivity: 'base' });
}
export function sortAmenityPreviewTags(tags: string[]): string[] {
    return [...tags].sort(compareAmenityTags);
}
