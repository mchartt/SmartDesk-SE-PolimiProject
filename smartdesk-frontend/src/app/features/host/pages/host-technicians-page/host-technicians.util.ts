import { HttpErrorResponse } from '@angular/common/http';
import { Space } from '../../../../core/models';
import { HostTechnicianAssignedSpaceDto, HostTechnicianDto } from '../../../../core/services/host.service';
import { technicianDisplayCode } from '../host-tickets-page/host-tickets.util';

export const HOST_TECH_ASSIGN_SUCCESS_TOAST_MS = 4500;
export const HOST_TECH_RECENT_LIMIT = 3;
export const HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS = 2;

export interface HostTechnicianAssignmentTicketContext {
    spaceID: number;
    ticketId: number;
    ticketCode?: string;
    deskCode?: string;
}

export interface HostTechModalSearchFilters {
    unified: string;
    nome: string;
    cognome: string;
    codice: string;
}

export interface HostTechCreateFormState {
    showErrors: boolean;
    apiFieldErrors: Record<string, string>;
    nome: string;
    cognome: string;
    email: string;
    password: string;
    specialization: string;
}

export interface TechnicianAssignmentDeepLinkResult {
    consumed: boolean;
    spaceId: number | null;
    invalidSpace: boolean;
    ticketContext: HostTechnicianAssignmentTicketContext | null;
    openTechPicker: boolean;
}

export type HostTechEditValidation =
    | { valid: true; name: string; email: string; password?: string }
    | { valid: false; error: string };

export function normalizeTechnicianSearch(raw: string): string {
    return raw
        .trim()
        .toLocaleLowerCase('it-IT')
        .normalize('NFD')
        .replace(/\p{M}/gu, '');
}

export function registeredAtMs(iso: string | null | undefined): number {
    if (!iso) {
        return 0;
    }
    const ms = Date.parse(iso);
    return Number.isFinite(ms) ? ms : 0;
}

export function sortTechniciansRecentFirst(rows: HostTechnicianDto[]): HostTechnicianDto[] {
    return [...rows].sort((a, b) => {
        const tb = registeredAtMs(b.registeredAt);
        const ta = registeredAtMs(a.registeredAt);
        if (tb !== ta) {
            return tb - ta;
        }
        return b.technicianID - a.technicianID;
    });
}

export function nameTokens(name: string): { first: string; rest: string } {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) {
        return { first: '', rest: '' };
    }
    if (parts.length === 1) {
        return { first: parts[0], rest: '' };
    }
    return { first: parts[0], rest: parts.slice(1).join(' ') };
}

export function technicianSpecDisplay(t: HostTechnicianDto): string {
    return (t.specialization ?? '').trim();
}

export function techInitials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) {
        return '?';
    }
    const a = parts[0].charAt(0).toUpperCase();
    const b = parts.length > 1 ? parts[parts.length - 1].charAt(0).toUpperCase() : '';
    return (a + b).slice(0, 2);
}

function technicianSearchHaystack(t: HostTechnicianDto): string {
    return [
        t.name,
        t.email,
        t.specialization ?? '',
        String(t.technicianID),
        technicianDisplayCode(t),
        ...(t.assignedSpaces ?? []).flatMap((s) => [s.name, s.officeCode ?? '', String(s.spaceID)])
    ]
        .join(' ')
        .toLocaleLowerCase('it-IT')
        .normalize('NFD')
        .replace(/\p{M}/gu, '');
}

export function technicianMatchesQuery(t: HostTechnicianDto, q: string): boolean {
    return technicianSearchHaystack(t).includes(q);
}

export function matchesTechnicianCode(t: HostTechnicianDto, raw: string): boolean {
    const code = technicianDisplayCode(t).toLowerCase().replace(/\s/g, '');
    const idDigits = String(t.technicianID);
    return code.includes(raw) || idDigits.includes(raw.replace(/^tc-?/i, ''));
}

export function spaceMatchesPickQuery(s: Space, q: string): boolean {
    const hay = [s.name, s.city, s.address, s.officeCode, String(s.spaceID), s.description]
        .join(' ')
        .toLocaleLowerCase('it-IT')
        .normalize('NFD')
        .replace(/\p{M}/gu, '');
    return hay.includes(q);
}

export function filterTechniciansBySearch(rows: HostTechnicianDto[], rawQuery: string): HostTechnicianDto[] {
    const q = normalizeTechnicianSearch(rawQuery);
    if (!q) {
        return rows;
    }
    return rows.filter((t) => technicianMatchesQuery(t, q));
}

export function filterDashboardTechnicians(
    sorted: HostTechnicianDto[],
    quickQuery: string,
    recentLimit: number = HOST_TECH_RECENT_LIMIT
): HostTechnicianDto[] {
    if (sorted.length > recentLimit) {
        return sorted.slice(0, recentLimit);
    }
    return filterTechniciansBySearch(sorted, quickQuery);
}

export function filterModalTechnicians(
    rows: HostTechnicianDto[],
    filters: HostTechModalSearchFilters
): HostTechnicianDto[] {
    const u = normalizeTechnicianSearch(filters.unified);
    const n = normalizeTechnicianSearch(filters.nome);
    const c = normalizeTechnicianSearch(filters.cognome);
    const code = filters.codice.trim().toLowerCase().replace(/\s/g, '');
    if (!u && !n && !c && !code) {
        return rows;
    }
    return rows.filter((t) => {
        if (u && !technicianMatchesQuery(t, u)) {
            return false;
        }
        if (code && !matchesTechnicianCode(t, code)) {
            return false;
        }
        if (!n && !c) {
            return true;
        }
        const nt = nameTokens(t.name);
        if (n && !normalizeTechnicianSearch(nt.first).includes(n)) {
            return false;
        }
        if (c && (!nt.rest || !normalizeTechnicianSearch(nt.rest).includes(c))) {
            return false;
        }
        return true;
    });
}

export function techMgmtModalFiltersActive(filters: HostTechModalSearchFilters): boolean {
    return !!(
        filters.unified.trim() ||
        filters.nome.trim() ||
        filters.cognome.trim() ||
        filters.codice.trim()
    );
}

export function showDashboardInlineSearch(totalCount: number, recentLimit: number = HOST_TECH_RECENT_LIMIT): boolean {
    return totalCount <= recentLimit;
}

export function showSeeAllTechniciansCta(totalCount: number, recentLimit: number = HOST_TECH_RECENT_LIMIT): boolean {
    return totalCount > recentLimit;
}

export function technicianCountForSpace(allTechnicians: HostTechnicianDto[], spaceId: number): number {
    return allTechnicians.filter((t) => (t.assignedSpaces ?? []).some((s) => s.spaceID === spaceId)).length;
}

export function spacesAssignableFor(t: HostTechnicianDto, approvedSpaces: Space[]): Space[] {
    const ids = new Set((t.assignedSpaces ?? []).map((s) => s.spaceID));
    return approvedSpaces.filter((s) => !ids.has(s.spaceID));
}

export function assignableSpaceCount(t: HostTechnicianDto, approvedSpaces: Space[]): number {
    return spacesAssignableFor(t, approvedSpaces).length;
}

export function spacesPickFilteredList(
    technician: HostTechnicianDto | null | undefined,
    approvedSpaces: Space[],
    rawQuery: string
): Space[] {
    if (!technician) {
        return [];
    }
    const assigned = new Set((technician.assignedSpaces ?? []).map((s) => s.spaceID));
    let rows = approvedSpaces.filter((s) => !assigned.has(s.spaceID));
    const q = normalizeTechnicianSearch(rawQuery);
    if (!q) {
        return rows;
    }
    return rows.filter((s) => spaceMatchesPickQuery(s, q));
}

export function filterAssignableSpaces(approvedSpaces: Space[], rawQuery: string): Space[] {
    const q = normalizeTechnicianSearch(rawQuery);
    if (!q) {
        return approvedSpaces;
    }
    return approvedSpaces.filter((s) => spaceMatchesPickQuery(s, q));
}

export function spaceCardAriaLabel(space: Space, techCount: number): string {
    const unit = techCount === 1 ? 'tecnico assegnato' : 'tecnici assegnati';
    return `Spazio ${space.name}, ${techCount} ${unit}.`;
}

export function spacePickTechnicianLabel(t: HostTechnicianDto | null | undefined): string {
    if (!t) {
        return '';
    }
    return `${t.name} (${technicianDisplayCode(t)})`;
}

export function assignSpacePickerPrimary(selectedSpace: Space | null | undefined): string {
    return selectedSpace?.name ?? 'Scegli spazio';
}

export function assignSpacePickerSecondary(selectedSpace: Space | null | undefined): string {
    if (!selectedSpace) {
        return '';
    }
    const bits = [selectedSpace.officeCode?.trim(), selectedSpace.city?.trim()].filter(Boolean);
    return bits.length ? bits.join(' · ') : (selectedSpace.address ?? '');
}

export function assignTechPickerPrimary(technician: HostTechnicianDto | null | undefined): string {
    return technician?.name ?? 'Scegli tecnico';
}

export function assignTechPickerSecondary(technician: HostTechnicianDto | null | undefined): string {
    if (!technician) {
        return '';
    }
    const spec = (technician.specialization ?? '').trim();
    return spec ? `${technicianDisplayCode(technician)} · ${spec}` : technicianDisplayCode(technician);
}

export function visibleAssignedSpaces(
    t: HostTechnicianDto,
    maxChips: number = HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS
): HostTechnicianAssignedSpaceDto[] {
    return (t.assignedSpaces ?? []).slice(0, maxChips);
}

export function hiddenAssignedSpacesCount(
    t: HostTechnicianDto,
    maxChips: number = HOST_TECH_MAX_VISIBLE_ASSIGNED_SPACE_CHIPS
): number {
    const total = (t.assignedSpaces ?? []).length;
    return Math.max(0, total - maxChips);
}

export function normalizeTechnicianRow(r: HostTechnicianDto): HostTechnicianDto {
    return {
        ...r,
        assignedSpaces: r.assignedSpaces ?? [],
        profileVersion: r.profileVersion ?? 0,
        registeredAt: r.registeredAt ?? null
    };
}

export function isSpaceApproved(approvedSpaces: Space[], spaceId: number | null): boolean {
    if (spaceId == null) {
        return false;
    }
    return approvedSpaces.some((s) => s.spaceID === spaceId);
}

export function emailLooksValid(raw: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(raw.trim());
}

export function passwordMeetsRules(p: string): boolean {
    if (p.length < 8) {
        return false;
    }
    if (!/\d/.test(p)) {
        return false;
    }
    if (!/[^a-zA-Z0-9\s]/.test(p)) {
        return false;
    }
    return true;
}

export function buildCreateTechnicianDisplayName(nome: string, cognome: string): string {
    return `${nome} ${cognome}`.replace(/\s+/g, ' ').trim();
}

export function isCreateTechnicianFormValid(state: HostTechCreateFormState): boolean {
    const nome = state.nome.trim();
    const cognome = state.cognome.trim();
    const emailTrim = state.email.trim();
    const specTrim = state.specialization.trim();
    return !!(
        nome &&
        cognome &&
        emailTrim &&
        emailLooksValid(emailTrim) &&
        passwordMeetsRules(state.password) &&
        specTrim
    );
}

export function createNomeInvalid(state: HostTechCreateFormState): boolean {
    if (state.apiFieldErrors['name']) {
        return true;
    }
    return state.showErrors && !state.nome.trim();
}

export function createCognomeInvalid(state: HostTechCreateFormState): boolean {
    if (state.apiFieldErrors['name']) {
        return true;
    }
    return state.showErrors && !state.cognome.trim();
}

export function createEmailInvalid(state: HostTechCreateFormState): boolean {
    if (state.apiFieldErrors['email']) {
        return true;
    }
    if (!state.showErrors) {
        return false;
    }
    const e = state.email.trim();
    return !e || !emailLooksValid(e);
}

export function createPasswordInvalid(state: HostTechCreateFormState): boolean {
    if (state.apiFieldErrors['password']) {
        return true;
    }
    return state.showErrors && !passwordMeetsRules(state.password);
}

export function createSpecializationInvalid(state: HostTechCreateFormState): boolean {
    if (state.apiFieldErrors['specialization']) {
        return true;
    }
    return state.showErrors && !state.specialization.trim();
}

export function createNomeInlineError(state: HostTechCreateFormState): string | null {
    const api = state.apiFieldErrors['name'];
    if (api) {
        return api;
    }
    if (state.showErrors && !state.nome.trim()) {
        return 'Campo obbligatorio.';
    }
    return null;
}

export function createCognomeInlineError(state: HostTechCreateFormState): string | null {
    if (state.showErrors && !state.cognome.trim()) {
        return 'Campo obbligatorio.';
    }
    return null;
}

export function createEmailInlineError(state: HostTechCreateFormState): string | null {
    const api = state.apiFieldErrors['email'];
    if (api) {
        return api;
    }
    if (!state.showErrors) {
        return null;
    }
    const e = state.email.trim();
    if (!e) {
        return 'Campo obbligatorio.';
    }
    if (!emailLooksValid(e)) {
        return "Inserisci un'email valida (es. nome@dominio.it).";
    }
    return null;
}

export function createPasswordInlineError(state: HostTechCreateFormState): string | null {
    const api = state.apiFieldErrors['password'];
    if (api) {
        return api;
    }
    if (!state.showErrors) {
        return null;
    }
    if (!passwordMeetsRules(state.password)) {
        return 'Almeno 8 caratteri, inclusi un numero e un simbolo.';
    }
    return null;
}

export function createSpecializationInlineError(state: HostTechCreateFormState): string | null {
    const api = state.apiFieldErrors['specialization'];
    if (api) {
        return api;
    }
    if (state.showErrors && !state.specialization.trim()) {
        return 'Campo obbligatorio.';
    }
    return null;
}

export function validateEditTechnicianFields(
    name: string,
    email: string,
    password: string
): HostTechEditValidation {
    const trimmedName = name.trim();
    const trimmedEmail = email.trim();
    if (!trimmedName || !trimmedEmail) {
        return { valid: false, error: 'Nome e email sono obbligatori.' };
    }
    const pw = password.trim();
    if (pw && !passwordMeetsRules(pw)) {
        return {
            valid: false,
            error: 'La nuova password deve avere almeno 8 caratteri, un numero e un simbolo.'
        };
    }
    return { valid: true, name: trimmedName, email: trimmedEmail, ...(pw ? { password: pw } : {}) };
}

export function fieldErrorsFromValidationResponse(err: unknown): Record<string, string> | null {
    if (!(err instanceof HttpErrorResponse) || err.status !== 400) {
        return null;
    }
    const body = err.error as {
        code?: string;
        fieldErrors?: Array<{ field?: string; message?: string }>;
    } | null;
    if (body?.code !== 'VALIDATION_ERROR' || !Array.isArray(body.fieldErrors)) {
        return null;
    }
    const out: Record<string, string> = {};
    for (const fe of body.fieldErrors) {
        if (fe.field && fe.message) {
            out[fe.field] = fe.message;
        }
    }
    return Object.keys(out).length ? out : null;
}

export function parseTechnicianAssignmentDeepLink(
    params: {
        assignSpace?: string | null;
        spaceId?: string | null;
        ticketId?: string | null;
        ticketCode?: string | null;
        deskCode?: string | null;
        pickTech?: string | null;
    },
    approvedSpaceIds: readonly number[]
): TechnicianAssignmentDeepLinkResult {
    const rawSpace = params.assignSpace ?? params.spaceId;
    if (!rawSpace?.trim()) {
        return { consumed: false, spaceId: null, invalidSpace: false, ticketContext: null, openTechPicker: false };
    }
    const spaceId = Number(rawSpace);
    if (!Number.isFinite(spaceId)) {
        return { consumed: false, spaceId: null, invalidSpace: false, ticketContext: null, openTechPicker: false };
    }
    if (!approvedSpaceIds.includes(spaceId)) {
        return { consumed: true, spaceId, invalidSpace: true, ticketContext: null, openTechPicker: false };
    }
    const ticketRaw = params.ticketId;
    const ticketId = ticketRaw != null && ticketRaw !== '' ? Number(ticketRaw) : NaN;
    const tc = (params.ticketCode ?? '').trim();
    const dc = (params.deskCode ?? '').trim();
    const ticketContext: HostTechnicianAssignmentTicketContext | null = Number.isFinite(ticketId)
        ? { spaceID: spaceId, ticketId, ticketCode: tc || undefined, deskCode: dc || undefined }
        : null;
    return {
        consumed: true,
        spaceId,
        invalidSpace: false,
        ticketContext,
        openTechPicker: params.pickTech === '1'
    };
}

export { technicianDisplayCode };
