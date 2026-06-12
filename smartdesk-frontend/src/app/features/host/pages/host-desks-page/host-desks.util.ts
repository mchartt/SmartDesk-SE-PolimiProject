import { Desk, DeskStateCode } from '../../../../core/models';
import { HostAmenityPresetDto, HostRoom } from '../../../../core/services/host.service';
import { nextDeskCodeForRoom } from '../../../../core/utils/room-desk-code.util';

export const HOST_DESKS_ROOM_NAME_MAX_LEN = 80;
export const HOST_DESKS_ROOM_CODE_MAX_LEN = 10;
export const HOST_DESKS_LEGACY_LS_CUSTOM_AMENITY_PRESETS = 'smartdesk.host.custom-amenity-presets.v1';
export const HOST_DESKS_PRESET_LABEL_MAX = 48;
export const HOST_DESKS_PRESET_HINT_MAX = 120;
export const HOST_DESKS_AMENITY_TOKEN_MAX = 12;

export interface DeskRoomSection {
    room: HostRoom | null;
    desks: Desk[];
}

export interface AmenityPreset {
    readonly id: string;
    readonly label: string;
    readonly hint?: string;
    readonly amenities: readonly string[];
}

export interface HostCustomAmenityPreset {
    readonly id: number;
    label: string;
    hint?: string;
    amenities: string[];
}

export type HostPresetFieldValidation =
    | { valid: true; label: string; hint: string }
    | { valid: false; error: string };

export type HostPresetConflict = 'label' | 'amenities' | null;

const DESK_STATE_LABELS: Record<string, string> = {
    AVAILABLE: 'Disponibile',
    BOOKED: 'Prenotata',
    MAINTENANCE: 'Manutenzione',
    PENDING_INSPECTION: 'Da ispezionare',
    RESERVED: 'Riservata',
    DECOMMISSIONED: 'Dismessa'
};

const DESK_STATE_BADGE_TONES: Record<string, string> = {
    AVAILABLE: 'text-bg-success',
    BOOKED: 'text-bg-primary',
    MAINTENANCE: 'text-bg-warning',
    PENDING_INSPECTION: 'text-bg-warning',
    RESERVED: 'text-bg-primary',
    DECOMMISSIONED: 'text-bg-danger'
};

export function buildDeskSections(rooms: HostRoom[], desks: Desk[]): DeskRoomSection[] {
    const map = new Map<number, Desk[]>();
    for (const r of rooms) {
        map.set(r.roomID, []);
    }
    const orphans: Desk[] = [];
    for (const d of desks) {
        const rid = d.roomID;
        if (rid != null && map.has(rid)) {
            map.get(rid)!.push(d);
        }
        else {
            orphans.push(d);
        }
    }
    for (const list of map.values()) {
        list.sort((a, b) => a.code.localeCompare(b.code, 'it', { numeric: true }));
    }
    orphans.sort((a, b) => a.code.localeCompare(b.code, 'it', { numeric: true }));
    const sections: DeskRoomSection[] = rooms.map((room) => ({
        room,
        desks: map.get(room.roomID) ?? []
    }));
    if (orphans.length) {
        sections.push({ room: null, desks: orphans });
    }
    return sections;
}

export function sectionKey(section: DeskRoomSection): string {
    return section.room ? String(section.room.roomID) : 'orphan-desks';
}

export function previewDeskCodeForNewDesk(
    editingDeskId: number | null,
    deskRoomId: number | null,
    rooms: HostRoom[],
    desks: Desk[]
): string {
    if (editingDeskId !== null || deskRoomId === null) {
        return '';
    }
    const room = rooms.find((r) => r.roomID === deskRoomId);
    if (!room) {
        return '';
    }
    const codes = desks.filter((d) => d.roomID === deskRoomId).map((d) => d.code);
    return nextDeskCodeForRoom(room.code, codes);
}

export function maintenanceDesksFrom(desks: Desk[]): Desk[] {
    return desks.filter((d) => d.currentState === 'MAINTENANCE');
}

export function canManageDesks(spaceApproved: boolean | undefined, roomCount: number): boolean {
    return !!(spaceApproved && roomCount > 0);
}

export function deskStateLabel(stateCode: DeskStateCode | string): string {
    return DESK_STATE_LABELS[stateCode] ?? String(stateCode);
}

export function deskStateBadgeClass(stateCode: DeskStateCode | string): string {
    return `badge rounded-pill ${DESK_STATE_BADGE_TONES[stateCode] ?? 'text-bg-secondary'}`;
}

export function canEditDesk(state: DeskStateCode): boolean {
    return state !== 'DECOMMISSIONED';
}

export function formatDeskTitle(code: string): string {
    const c = code.trim();
    return c.length ? `Desk ${c}` : 'Desk';
}

export function deskCardRoomName(desk: Desk, sectionRoom: HostRoom | null): string {
    return desk.roomName?.trim() || sectionRoom?.name?.trim() || '—';
}

export function filterDesksInSection(section: DeskRoomSection, query: string): Desk[] {
    const q = query.trim().toLowerCase();
    const list = section.desks;
    if (!q) {
        return list;
    }
    return list.filter((d) => {
        const title = formatDeskTitle(d.code).toLowerCase();
        const code = d.code.toLowerCase();
        return code.includes(q) || title.includes(q);
    });
}

export function normalizeAmenityToken(raw: string, maxLen = HOST_DESKS_AMENITY_TOKEN_MAX): string {
    return raw.trim().toUpperCase().slice(0, maxLen);
}

export function normalizePresetAmenitiesList(values: string[], maxLen = HOST_DESKS_AMENITY_TOKEN_MAX): string[] {
    const out: string[] = [];
    const seen = new Set<string>();
    for (const raw of values) {
        const t = normalizeAmenityToken(String(raw), maxLen);
        if (!t || seen.has(t)) {
            continue;
        }
        seen.add(t);
        out.push(t);
    }
    return out;
}

export function normalizePresetLabelKey(label: string): string {
    return label.trim().toLowerCase();
}

export function presetAmenitiesSignature(amenities: string[]): string {
    const norm = normalizePresetAmenitiesList(amenities);
    return [...norm].sort((a, b) => a.localeCompare(b, 'it', { numeric: true })).join('\u0001');
}

export function findPresetConflict(
    presets: HostCustomAmenityPreset[],
    label: string,
    amenities: string[],
    excludePresetId?: number
): HostPresetConflict {
    const lk = normalizePresetLabelKey(label);
    const sig = presetAmenitiesSignature(amenities);
    let labelHit = false;
    let amenitiesHit = false;
    for (const p of presets) {
        if (excludePresetId !== undefined && p.id === excludePresetId) {
            continue;
        }
        if (normalizePresetLabelKey(p.label) === lk) {
            labelHit = true;
        }
        if (presetAmenitiesSignature(p.amenities) === sig) {
            amenitiesHit = true;
        }
    }
    if (labelHit) {
        return 'label';
    }
    if (amenitiesHit) {
        return 'amenities';
    }
    return null;
}

export function validateHostPresetFormFields(
    labelRaw: string,
    hintRaw: string,
    amenitiesCount: number,
    limits: {
        labelMax?: number;
        hintMax?: number;
    } = {}
): HostPresetFieldValidation {
    const labelMax = limits.labelMax ?? HOST_DESKS_PRESET_LABEL_MAX;
    const hintMax = limits.hintMax ?? HOST_DESKS_PRESET_HINT_MAX;
    const label = labelRaw.trim();
    if (!label) {
        return { valid: false, error: 'Indica un nome per il set.' };
    }
    if (label.length > labelMax) {
        return { valid: false, error: `Nome set troppo lungo (max ${labelMax} caratteri).` };
    }
    const hint = hintRaw.trim();
    if (!hint) {
        return { valid: false, error: 'Indica una descrizione breve per il set.' };
    }
    if (hint.length > hintMax) {
        return { valid: false, error: `Descrizione troppo lunga (max ${hintMax} caratteri).` };
    }
    if (amenitiesCount < 1) {
        return { valid: false, error: 'Aggiungi almeno una dotazione al set.' };
    }
    return { valid: true, label, hint };
}

export function isHostPresetFormFilled(label: string, hint: string, amenitiesCount: number): boolean {
    return label.trim().length > 0 && hint.trim().length > 0 && amenitiesCount > 0;
}

export function mapPresetFromDto(dto: HostAmenityPresetDto): HostCustomAmenityPreset | null {
    const id = dto.presetID;
    if (id == null) {
        return null;
    }
    const hint = dto.hint?.trim();
    return {
        id,
        label: dto.label.trim(),
        hint: hint ? hint : undefined,
        amenities: normalizePresetAmenitiesList(dto.amenities ?? [])
    };
}

export function mapPresetApiError(err: unknown): string {
    const msg = err instanceof Error ? err.message : '';
    if (msg.includes('same label')) {
        return 'Esiste già un set con questo nome (anche su altro dispositivo).';
    }
    if (msg.includes('same amenity')) {
        return 'Esiste già un set con lo stesso elenco di dotazioni.';
    }
    if (msg.includes('Impossibile')) {
        return 'Operazione non riuscita. Controlla la connessione e riprova.';
    }
    return msg || 'Operazione non riuscita.';
}

export function sortHostPresetsByLabel(presets: HostCustomAmenityPreset[]): HostCustomAmenityPreset[] {
    return [...presets].sort((a, b) => a.label.localeCompare(b.label, 'it', { sensitivity: 'base', numeric: true }));
}

export function buildHostPresetSummaryText(sorted: HostCustomAmenityPreset[]): string {
    const total = sorted.length;
    if (!total) {
        return '';
    }
    const shown = sorted.slice(0, 2).map((p) => p.label);
    if (total <= 2) {
        return shown.join(' · ');
    }
    return `${shown.join(' · ')} · +${total - 2}`;
}

export function filterHostCustomPresets(presets: HostCustomAmenityPreset[], query: string): HostCustomAmenityPreset[] {
    const q = query.trim().toLowerCase();
    if (!q) {
        return presets;
    }
    return presets.filter(
        (p) =>
            p.label.toLowerCase().includes(q) ||
            (p.hint?.toLowerCase().includes(q) ?? false) ||
            p.amenities.some((a) => a.toLowerCase().includes(q))
    );
}

export function customPresetsToAmenityPresets(custom: HostCustomAmenityPreset[]): AmenityPreset[] {
    return custom.map((p) => ({
        id: `custom:${p.id}`,
        label: p.label,
        hint: p.hint,
        amenities: p.amenities
    }));
}

export function filterPresetsForDeskApply(custom: HostCustomAmenityPreset[], query: string): AmenityPreset[] {
    const merged = customPresetsToAmenityPresets(custom);
    const q = query.trim().toLowerCase();
    if (!q) {
        return merged;
    }
    return merged.filter((p) => {
        const inLabel = p.label.toLowerCase().includes(q);
        const inHint = p.hint?.toLowerCase().includes(q) ?? false;
        const inAmenity = p.amenities.some((a) => a.toLowerCase().includes(q));
        return inLabel || inHint || inAmenity;
    });
}

export function presetFullyApplied(desk: Desk, preset: AmenityPreset): boolean {
    const deskUpper = new Set(desk.amenities.map((a) => a.toUpperCase()));
    return preset.amenities.every((a) => deskUpper.has(normalizeAmenityToken(a)));
}

export function amenitiesToAddFromPreset(desk: Desk, preset: AmenityPreset): string[] {
    const deskUpper = new Set(desk.amenities.map((a) => a.toUpperCase()));
    return preset.amenities
        .map((a) => normalizeAmenityToken(a))
        .filter((a) => a.length > 0 && !deskUpper.has(a));
}

export function validateRoomFormFields(
    nameRaw: string,
    codeRaw: string,
    limits: { nameMax?: number } = {}
): { valid: true; name: string; code: string } | { valid: false; error: string } {
    const nameMax = limits.nameMax ?? HOST_DESKS_ROOM_NAME_MAX_LEN;
    const name = nameRaw.trim();
    const code = codeRaw.trim().toUpperCase();
    if (!name || name.length > nameMax) {
        return { valid: false, error: `Indica un nome stanza (max ${nameMax} caratteri).` };
    }
    if (!/^[A-Z0-9]{2,10}$/.test(code)) {
        return {
            valid: false,
            error: 'Codice stanza: 2–10 caratteri tra lettere maiuscole e numeri (es. TA, SR01).'
        };
    }
    return { valid: true, name, code };
}

export function parseLegacyPresetPayloads(rawList: unknown[]): Array<{
    label: string;
    hint?: string;
    amenities: string[];
}> {
    const payloads: Array<{ label: string; hint?: string; amenities: string[] }> = [];
    for (const item of rawList) {
        const o = item as Record<string, unknown>;
        const label = String(o['label'] ?? '').trim();
        const amenities = Array.isArray(o['amenities'])
            ? normalizePresetAmenitiesList(o['amenities'].map((x) => String(x)))
            : [];
        const hintRaw = o['hint'];
        const hint =
            hintRaw != null && String(hintRaw).trim().length > 0 ? String(hintRaw).trim() : undefined;
        if (!label || !amenities.length) {
            continue;
        }
        payloads.push({ label, hint, amenities });
    }
    return payloads;
}
