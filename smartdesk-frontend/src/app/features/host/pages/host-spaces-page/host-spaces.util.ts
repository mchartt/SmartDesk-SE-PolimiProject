import { Space } from '../../../../core/models';
import { HostSpaceClosureDto, HostSpaceUpsertPayload } from '../../../../core/services/host.service';
import { localCalendarDateIso, pad2 } from '../../../../core/utils/date.util';
import { hmToMinutes } from '../../../../core/utils/time.util';

export type WeekdayKey = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type HoursTimeField = 'open' | 'close';
export type ClosureWorkflow = 'schedule' | 'revoke';

export interface OpeningHoursRow {
    dayKey: WeekdayKey;
    label: string;
    short: string;
    closed: boolean;
    open: string;
    close: string;
    isUndefined?: boolean;
}

export interface ClosureCalCell {
    iso: string;
    dayNum: number;
    inMonth: boolean;
    isPast: boolean;
    isExisting: boolean;
    isPending: boolean;
    isRangeAnchor: boolean;
    isRevokeBand: boolean;
    isWeeklyRecurringClosed: boolean;
}

export interface ClosureUiGroup {
    trackKey: string;
    startIso: string;
    endIso: string;
    ids: number[];
    reasonText: string;
    dateLabelOverride?: string | null;
}

export interface PendingClosureUiGroup {
    trackKey: string;
    startIso: string;
    endIso: string;
    dates: string[];
}

interface ClosureDayBucket {
    iso: string;
    items: HostSpaceClosureDto[];
}

export const JS_DOW_BY_DAYKEY: Record<WeekdayKey, number> = {
    SUNDAY: 0,
    MONDAY: 1,
    TUESDAY: 2,
    WEDNESDAY: 3,
    THURSDAY: 4,
    FRIDAY: 5,
    SATURDAY: 6
};

export const WEEKDAY_META: ReadonlyArray<{
    dayKey: WeekdayKey;
    label: string;
    short: string;
}> = [
    { dayKey: 'MONDAY', label: 'Lunedì', short: 'Lun' },
    { dayKey: 'TUESDAY', label: 'Martedì', short: 'Mar' },
    { dayKey: 'WEDNESDAY', label: 'Mercoledì', short: 'Mer' },
    { dayKey: 'THURSDAY', label: 'Giovedì', short: 'Gio' },
    { dayKey: 'FRIDAY', label: 'Venerdì', short: 'Ven' },
    { dayKey: 'SATURDAY', label: 'Sabato', short: 'Sab' },
    { dayKey: 'SUNDAY', label: 'Domenica', short: 'Dom' }
];

export const HH_MM = /^([01]\d|2[0-3]):[0-5]\d$/;
export const DEFAULT_OPEN = '09:00';
export const DEFAULT_CLOSE = '18:00';

export const HOST_TIME_START_OPTIONS: string[] = (() => {
    const out: string[] = [];
    for (let mins = 8 * 60; mins <= 19 * 60 + 30; mins += 30) {
        out.push(`${String(Math.floor(mins / 60)).padStart(2, '0')}:${String(mins % 60).padStart(2, '0')}`);
    }
    return out;
})();

export const HOST_TIME_END_OPTIONS: string[] = (() => {
    const out: string[] = [];
    for (let mins = 8 * 60 + 30; mins <= 20 * 60; mins += 30) {
        out.push(`${String(Math.floor(mins / 60)).padStart(2, '0')}:${String(mins % 60).padStart(2, '0')}`);
    }
    return out;
})();

export function isoLocalDate(y: number, m1: number, d: number): string {
    return `${y}-${pad2(m1)}-${pad2(d)}`;
}

export function addDaysIso(iso: string, deltaDays: number): string {
    const s = iso.slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) {
        return iso;
    }
    const [y, mo, d] = s.split('-').map(Number);
    const dt = new Date(y, mo - 1, d + deltaDays);
    return isoLocalDate(dt.getFullYear(), dt.getMonth() + 1, dt.getDate());
}

export function jsDowFromIsoLocal(iso: string): number {
    const s = iso.slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) {
        return 0;
    }
    const [y, mo, d] = s.split('-').map(Number);
    return new Date(y, mo - 1, d).getDay();
}

export function isValidClosureIsoDate(iso: string): boolean {
    return /^\d{4}-\d{2}-\d{2}$/.test(iso);
}

export function expandInclusiveRange(lo: string, hi: string): string[] {
    const out: string[] = [];
    let cur = lo;
    let guard = 0;
    while (cur <= hi && guard++ < 370) {
        out.push(cur);
        cur = addDaysIso(cur, 1);
    }
    return out;
}

export function addMinutesToHmClamped(hm: string, delta: number): string | null {
    const mins = hmToMinutes(hm);
    if (!Number.isFinite(mins)) {
        return null;
    }
    const next = Math.min(20 * 60, Math.max(8 * 60, mins + delta));
    return `${String(Math.floor(next / 60)).padStart(2, '0')}:${String(next % 60).padStart(2, '0')}`;
}

export function buildDefaultOpeningHoursRows(): OpeningHoursRow[] {
    return WEEKDAY_META.map(({ dayKey, label, short }) => ({
        dayKey,
        label,
        short,
        closed: true,
        open: DEFAULT_OPEN,
        close: DEFAULT_CLOSE
    }));
}

export function mergeOpeningHoursFromSpace(space: Space): OpeningHoursRow[] {
    const rows = buildDefaultOpeningHoursRows();
    const oh = space.openingHours;
    for (const row of rows) {
        const d = oh[row.dayKey];
        if (d) {
            row.closed = d.closed;
            row.open = d.open || DEFAULT_OPEN;
            row.close = d.close || DEFAULT_CLOSE;
            row.isUndefined = false;
        }
        else {
            row.isUndefined = true;
        }
    }
    return rows;
}

export function applyBusinessHoursPreset(rows: OpeningHoursRow[]): OpeningHoursRow[] {
    return rows.map((row) => {
        const weekend = row.dayKey === 'SATURDAY' || row.dayKey === 'SUNDAY';
        if (weekend) {
            return { ...row, closed: true };
        }
        return { ...row, closed: false, open: DEFAULT_OPEN, close: DEFAULT_CLOSE };
    });
}

export function applyAllDaysOpenPreset(rows: OpeningHoursRow[]): OpeningHoursRow[] {
    return rows.map((row) => ({
        ...row,
        closed: false,
        open: DEFAULT_OPEN,
        close: DEFAULT_CLOSE
    }));
}

export function openingHoursSummary(space: Space): string | null {
    const oh = space.openingHours;
    if (!oh || !Object.keys(oh).length) {
        return null;
    }
    const parts: string[] = [];
    let anyOpen = false;
    for (const { dayKey, short } of WEEKDAY_META) {
        const d = oh[dayKey];
        if (!d || d.closed) {
            continue;
        }
        anyOpen = true;
        parts.push(`${short} ${d.open}–${d.close}`);
    }
    if (!anyOpen) {
        return 'Chiuso tutti i giorni';
    }
    return parts.join(' · ');
}

export function hoursTimeOptions(row: OpeningHoursRow, field: HoursTimeField): string[] {
    if (field === 'open') {
        const closeMins = hmToMinutes(row.close);
        return HOST_TIME_START_OPTIONS.filter((hm) => {
            const mins = hmToMinutes(hm);
            return !Number.isFinite(closeMins) || mins + 30 <= closeMins;
        });
    }
    const openMins = hmToMinutes(row.open);
    return HOST_TIME_END_OPTIONS.filter((hm) => {
        const mins = hmToMinutes(hm);
        return !Number.isFinite(openMins) || mins >= openMins + 30;
    });
}

export type OpeningHoursPayloadResult =
    | { ok: true; openingHours: HostSpaceUpsertPayload['openingHours'] }
    | { ok: false; error: string };

export function buildOpeningHoursPayload(rows: OpeningHoursRow[]): OpeningHoursPayloadResult {
    const openingHours: HostSpaceUpsertPayload['openingHours'] = {};
    for (const row of rows) {
        if (row.closed) {
            openingHours[row.dayKey] = { closed: true };
            continue;
        }
        const open = row.open.trim();
        const close = row.close.trim();
        if (!open || !close) {
            return {
                ok: false,
                error: `Per ${row.label} indica orario di apertura e chiusura oppure segna il giorno come chiuso.`
            };
        }
        if (!HH_MM.test(open) || !HH_MM.test(close)) {
            return {
                ok: false,
                error: `Orario non valido per ${row.label}. Usa il formato HH:mm (es. 09:00).`
            };
        }
        if (open >= close) {
            return {
                ok: false,
                error: `Per ${row.label} l'orario di chiusura deve essere dopo l'apertura.`
            };
        }
        openingHours[row.dayKey] = { closed: false, open, close };
    }
    return { ok: true, openingHours };
}

export function weeklyClosedJsDowSet(openingHoursRows: OpeningHoursRow[]): Set<number> {
    const s = new Set<number>();
    for (const row of openingHoursRows) {
        if (row.closed) {
            const dow = JS_DOW_BY_DAYKEY[row.dayKey];
            if (dow !== undefined) {
                s.add(dow);
            }
        }
    }
    return s;
}

export function closureDateKey(row: HostSpaceClosureDto): string {
    return String(row.closedDate ?? '')
        .trim()
        .slice(0, 10);
}

export function closureExistingIsoSet(rows: HostSpaceClosureDto[]): Set<string> {
    const s = new Set<string>();
    for (const r of rows) {
        const k = closureDateKey(r);
        if (isValidClosureIsoDate(k)) {
            s.add(k);
        }
    }
    return s;
}

function closureReasonCanon(row: HostSpaceClosureDto): string {
    return (row.reason ?? '').trim();
}

function closureReasonCanonForBucket(bucket: ClosureDayBucket): string {
    const parts = [...new Set(bucket.items.map((r) => closureReasonCanon(r)).filter((t) => t.length > 0))].sort();
    return parts.join('\u0001');
}

function closureBucketsToUiGroup(run: ClosureDayBucket[]): ClosureUiGroup {
    const startIso = run[0]!.iso;
    const endIso = run[run.length - 1]!.iso;
    const ids = [...new Set(run.flatMap((b) => b.items.map((x) => x.id)))];
    const uniq = [
        ...new Set(run.flatMap((b) => b.items).map((row) => (row.reason ?? '').trim()).filter((t) => t.length > 0))
    ];
    const reasonText = uniq.length === 0 ? '—' : uniq.length === 1 ? uniq[0]! : uniq.join(' · ');
    return {
        trackKey: `${startIso}|${endIso}|${ids.join('-')}`,
        startIso,
        endIso,
        ids,
        reasonText,
        dateLabelOverride: null
    };
}

function closureInvalidRowToUiGroup(row: HostSpaceClosureDto): ClosureUiGroup {
    const raw = String(row.closedDate ?? '').trim();
    const label = raw.length > 0 ? `Data non valida (${raw.length > 48 ? `${raw.slice(0, 48)}…` : raw})` : 'Data non valida (mancante)';
    return {
        trackKey: `invalid|${row.id}`,
        startIso: '',
        endIso: '',
        ids: [row.id],
        reasonText: (row.reason ?? '').trim() || '—',
        dateLabelOverride: label
    };
}

export function buildClosureDisplayGroups(rows: HostSpaceClosureDto[]): ClosureUiGroup[] {
    if (!rows.length) {
        return [];
    }
    const valid: HostSpaceClosureDto[] = [];
    const invalid: HostSpaceClosureDto[] = [];
    for (const r of rows) {
        const k = closureDateKey(r);
        if (isValidClosureIsoDate(k)) {
            valid.push(r);
        }
        else {
            invalid.push(r);
        }
    }
    const byIso = new Map<string, HostSpaceClosureDto[]>();
    for (const r of valid) {
        const k = closureDateKey(r);
        const list = byIso.get(k);
        if (list) {
            list.push(r);
        }
        else {
            byIso.set(k, [r]);
        }
    }
    const buckets: ClosureDayBucket[] = [...byIso.keys()]
        .sort((a, b) => a.localeCompare(b))
        .map((iso) => ({ iso, items: byIso.get(iso)! }));
    const out: ClosureUiGroup[] = [];
    if (buckets.length) {
        let run: ClosureDayBucket[] = [buckets[0]!];
        const runCanon = () => closureReasonCanonForBucket(run[0]!);
        for (let i = 1; i < buckets.length; i++) {
            const b = buckets[i]!;
            const prev = run[run.length - 1]!;
            const consecutive = b.iso === addDaysIso(prev.iso, 1);
            const sameReasonCanon = closureReasonCanonForBucket(b) === runCanon();
            if (consecutive && sameReasonCanon) {
                run.push(b);
            }
            else {
                out.push(closureBucketsToUiGroup(run));
                run = [b];
            }
        }
        out.push(closureBucketsToUiGroup(run));
    }
    for (const r of invalid) {
        out.push(closureInvalidRowToUiGroup(r));
    }
    return out;
}

function pendingRunToGroup(run: string[]): PendingClosureUiGroup {
    const startIso = run[0]!;
    const endIso = run[run.length - 1]!;
    return {
        trackKey: `${startIso}|${endIso}`,
        startIso,
        endIso,
        dates: [...run]
    };
}

export function buildPendingClosureDisplayGroups(pendingDates: string[]): PendingClosureUiGroup[] {
    const raw = pendingDates;
    if (!raw.length) {
        return [];
    }
    const key = (s: string) => s.trim().slice(0, 10);
    const validKeys = [...new Set(raw.map(key))]
        .filter((k) => isValidClosureIsoDate(k))
        .sort((a, b) => a.localeCompare(b));
    const out: PendingClosureUiGroup[] = [];
    if (validKeys.length) {
        let run: string[] = [validKeys[0]!];
        for (let i = 1; i < validKeys.length; i++) {
            const cur = validKeys[i]!;
            const prev = run[run.length - 1]!;
            if (cur === addDaysIso(prev, 1)) {
                run.push(cur);
            }
            else {
                out.push(pendingRunToGroup(run));
                run = [cur];
            }
        }
        out.push(pendingRunToGroup(run));
    }
    for (const d of [...new Set(raw)]) {
        if (!isValidClosureIsoDate(key(d))) {
            out.push({
                trackKey: `pending-invalid|${d}`,
                startIso: d,
                endIso: d,
                dates: [d]
            });
        }
    }
    return out;
}

export interface BuildClosureCalendarRowsInput {
    year: number;
    month: number;
    pendingDates: string[];
    closureRows: HostSpaceClosureDto[];
    workflow: ClosureWorkflow;
    firstPick: string | null;
    revokeBand: { start: string; end: string } | null;
    openingHoursRows: OpeningHoursRow[];
    todayIso?: string;
}

export function buildClosureCalendarRows(input: BuildClosureCalendarRowsInput): ClosureCalCell[][] {
    const {
        year: y,
        month: m,
        pendingDates,
        closureRows,
        workflow: wf,
        firstPick: anchor,
        revokeBand,
        openingHoursRows
    } = input;
    const pending = new Set(pendingDates);
    const existing = closureExistingIsoSet(closureRows);
    const todayIso = input.todayIso ?? localCalendarDateIso();
    const weeklyClosedDows = weeklyClosedJsDowSet(openingHoursRows);
    const firstDow = new Date(y, m - 1, 1).getDay();
    const mondayIndex = (firstDow + 6) % 7;
    const dim = new Date(y, m, 0).getDate();
    const totalCells = Math.ceil((mondayIndex + dim) / 7) * 7;
    const flat: ClosureCalCell[] = [];
    for (let i = 0; i < totalCells; i++) {
        const dnum = i - mondayIndex + 1;
        const c = new Date(y, m - 1, dnum);
        const cy = c.getFullYear();
        const cm = c.getMonth() + 1;
        const cd = c.getDate();
        const iso = isoLocalDate(cy, cm, cd);
        const inMonth = cy === y && cm === m;
        const isPending = pending.has(iso);
        const jsDow = c.getDay();
        const isWeeklyRecurringClosed = inMonth && weeklyClosedDows.has(jsDow) && !existing.has(iso);
        const isRangeAnchor = anchor !== null &&
            anchor === iso &&
            ((wf === 'schedule' &&
                inMonth &&
                !existing.has(iso) &&
                iso >= todayIso &&
                (!weeklyClosedDows.has(jsDow) || pending.has(iso))) ||
                (wf === 'revoke' && inMonth));
        const isRevokeBand = wf === 'revoke' &&
            revokeBand !== null &&
            inMonth &&
            iso >= revokeBand.start &&
            iso <= revokeBand.end;
        flat.push({
            iso,
            dayNum: cd,
            inMonth,
            isPast: iso < todayIso,
            isExisting: existing.has(iso),
            isPending,
            isRangeAnchor,
            isRevokeBand,
            isWeeklyRecurringClosed
        });
    }
    const rows: ClosureCalCell[][] = [];
    for (let r = 0; r < flat.length; r += 7) {
        rows.push(flat.slice(r, r + 7));
    }
    return rows;
}

export function applyClosureScheduleRangeSelection(
    prevPending: string[],
    lo: string,
    hi: string,
    today: string,
    existing: Set<string>,
    weekly: Set<number>
): string[] {
    const set = new Set(prevPending.map((x) => x.trim().slice(0, 10)));
    const eligible: string[] = [];
    for (const d of expandInclusiveRange(lo, hi)) {
        if (d < today) {
            continue;
        }
        if (existing.has(d)) {
            continue;
        }
        const dow = jsDowFromIsoLocal(d);
        if (weekly.has(dow) && !set.has(d)) {
            continue;
        }
        eligible.push(d);
    }
    if (!eligible.length) {
        return [...set].sort();
    }
    const pendingInEligible = eligible.filter((d) => set.has(d));
    const allPending = pendingInEligible.length === eligible.length;
    const nonePending = pendingInEligible.length === 0;
    if (allPending) {
        for (const d of eligible) {
            set.delete(d);
        }
    }
    else if (nonePending) {
        for (const d of eligible) {
            set.add(d);
        }
    }
    else {
        for (const d of pendingInEligible) {
            set.delete(d);
        }
    }
    return [...set].sort();
}

export function closureScheduleCellDisabled(cell: ClosureCalCell): boolean {
    if (!cell.inMonth || cell.isPast || cell.isExisting) {
        return true;
    }
    return cell.isWeeklyRecurringClosed && !cell.isPending;
}

export function closureRevokeCellDisabled(cell: ClosureCalCell): boolean {
    return !cell.inMonth;
}

export function closureCellDisabled(cell: ClosureCalCell, workflow: ClosureWorkflow): boolean {
    return workflow === 'schedule' ? closureScheduleCellDisabled(cell) : closureRevokeCellDisabled(cell);
}

export function closureRevokeCountInBand(
    band: { start: string; end: string } | null,
    rows: HostSpaceClosureDto[]
): number {
    if (!band) {
        return 0;
    }
    return rows.filter((r) => {
        const k = closureDateKey(r);
        if (!isValidClosureIsoDate(k)) {
            return false;
        }
        return k >= band.start && k <= band.end;
    }).length;
}

export function closureRevokeCanSubmit(
    band: { start: string; end: string } | null,
    rows: HostSpaceClosureDto[]
): boolean {
    return band !== null && closureRevokeCountInBand(band, rows) > 0;
}

export function closureCalendarDayAria(cell: ClosureCalCell, workflow: ClosureWorkflow): string | null {
    if (!cell.inMonth) {
        return null;
    }
    const label = formatItDate(cell.iso);
    if (workflow === 'revoke') {
        if (cell.isRevokeBand) {
            return `${label}, nell'intervallo di revoca selezionato, tocca per deselezionare`;
        }
        if (cell.isExisting) {
            return `${label}, chiusura registrata, scegli estremi dell'intervallo da revocare`;
        }
        return `${label}, scegli intervallo revoca`;
    }
    if (cell.isExisting) {
        return `${label}, già chiusura registrata`;
    }
    if (cell.isPast) {
        return `${label}, giorno non selezionabile`;
    }
    if (cell.isWeeklyRecurringClosed && !cell.isPending) {
        return `${label}, chiuso nell'orario settimanale, non selezionabile`;
    }
    if (cell.isPending) {
        const extra = cell.isWeeklyRecurringClosed ? `, anche chiuso nell'orario settimanale` : '';
        return `${label}, in coda per nuova chiusura${extra}, primo estremo dell'intervallo`;
    }
    if (cell.isWeeklyRecurringClosed) {
        return `${label}, chiuso nell'orario settimanale`;
    }
    return `${label}, selezione intervallo nuove chiusure`;
}

export function closureMonthOptions(): { value: number; label: string }[] {
    const fmt = new Intl.DateTimeFormat('it-IT', { month: 'long' });
    return Array.from({ length: 12 }, (_, i) => ({
        value: i + 1,
        label: fmt.format(new Date(2020, i, 1))
    }));
}

export function closureYearOptions(now: Date = new Date()): number[] {
    const y = now.getFullYear();
    return [y, y + 1, y + 2, y + 3];
}

export function closureCalendarHeading(year: number, month: number): string {
    return new Intl.DateTimeFormat('it-IT', { month: 'long', year: 'numeric' }).format(new Date(year, month - 1, 1));
}

export function navigateClosureMonth(year: number, month: number, delta: number): { year: number; month: number } {
    let y = year;
    let mo = month + delta;
    while (mo < 1) {
        mo += 12;
        y--;
    }
    while (mo > 12) {
        mo -= 12;
        y++;
    }
    return { year: y, month: mo };
}

export function formatItDate(iso: string): string {
    const s = iso?.slice(0, 10) ?? '';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) {
        return iso;
    }
    const [y, m, d] = s.split('-').map(Number);
    try {
        return new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(y, m - 1, d));
    }
    catch {
        return s;
    }
}

export function formatItDateRange(startIso: string, endIso: string): string {
    if (startIso === endIso) {
        return formatItDate(startIso);
    }
    return `${formatItDate(startIso)} – ${formatItDate(endIso)}`;
}

export function pendingChipRemoveLabel(g: PendingClosureUiGroup): string {
    return g.startIso === g.endIso
        ? `Rimuovi ${formatItDate(g.startIso)}`
        : `Rimuovi intervallo ${formatItDateRange(g.startIso, g.endIso)}`;
}
