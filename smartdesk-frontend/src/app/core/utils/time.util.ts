const SLOT_TIME_HM = /^([01]?\d|2[0-3]):([0-5]?\d)$/;
function timePartFromIso(raw: string): string | null {
    const match = raw.match(/T(\d{2}):(\d{2})/);
    return match ? `${match[1]}:${match[2]}` : null;
}
export function normalizeSlotTimeHm(raw: string): string {
    const trimmed = raw.trim();
    if (!trimmed) {
        throw new RangeError('Orario slot vuoto (atteso HH:mm)');
    }
    const candidate = trimmed.includes('T') ? timePartFromIso(trimmed) : trimmed;
    if (!candidate) {
        throw new RangeError(`Orario slot non valido (atteso HH:mm): ${raw}`);
    }
    const match = candidate.match(SLOT_TIME_HM);
    if (!match) {
        throw new RangeError(`Orario slot non valido (atteso HH:mm): ${raw}`);
    }
    const h = parseInt(match[1], 10);
    const m = parseInt(match[2], 10);
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}
export function hmToMinutes(hm: string): number {
    const [h, m] = String(hm ?? '').split(':').map((part) => Number(part));
    return Number.isFinite(h) && Number.isFinite(m) ? h * 60 + m : NaN;
}
