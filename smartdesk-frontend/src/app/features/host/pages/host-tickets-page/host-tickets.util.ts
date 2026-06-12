import { Desk, Space } from '../../../../core/models';
import type { TicketNoteMessageDto } from '../../../../core/models';
import { HostTechnicianDto } from '../../../../core/services/host.service';
import { normalizeForSearch } from '../../../../core/utils/search.util';

export interface HostSpaceTicketsBundle {
    space: Space;
    desks: Desk[];
    ticketsByDeskId: Map<number, Record<string, unknown>[]>;
}

export type HostTicketsModalStep = 'rooms' | 'desks' | 'detail';

export interface ModalRoomRow {
    key: string;
    label: string;
    meta: string;
    deskCount: number;
}

export interface HostPendingApprovalItem {
    ticket?: Record<string, unknown>;
    desk: Desk;
    space: Space;
}

export interface PendingApprovalRoomGroup {
    roomId: number;
    roomName: string;
    desks: HostPendingApprovalItem[];
}

export const HOST_TICKET_SEVERITY_OPTIONS = [
    { value: 'LOW', label: 'Bassa' },
    { value: 'MEDIUM', label: 'Media' },
    { value: 'HIGH', label: 'Alta' },
    { value: 'CRITICAL', label: 'Critica' }
] as const;

export const HOST_TICKETS_NO_ROOM_ID = -1;

export function ticketStatusUpper(t: Record<string, unknown>): string {
    return String(t['status'] ?? '').toUpperCase();
}

export function isActiveTicket(t: Record<string, unknown>): boolean {
    const s = ticketStatusUpper(t);
    return s === 'OPEN' || s === 'IN_PROGRESS';
}

export function isResolvedTicket(t: Record<string, unknown>): boolean {
    const s = ticketStatusUpper(t);
    return s === 'RESOLVED' || s === 'CLOSED';
}

export function isVerifyingTicket(t: Record<string, unknown>): boolean {
    return ticketStatusUpper(t) === 'VERIFYING';
}

export function unresolvedTicketsForDesk(bundle: HostSpaceTicketsBundle, deskId: number): Record<string, unknown>[] {
    const rows = bundle.ticketsByDeskId.get(deskId) ?? [];
    return rows.filter((t) => isActiveTicket(t));
}

export function verifyingTicketsForDesk(bundle: HostSpaceTicketsBundle, deskId: number): Record<string, unknown>[] {
    const rows = bundle.ticketsByDeskId.get(deskId) ?? [];
    return rows.filter((t) => isVerifyingTicket(t));
}

export function unresolvedTicketCount(bundle: HostSpaceTicketsBundle): number {
    let n = 0;
    for (const d of bundle.desks) {
        n += unresolvedTicketsForDesk(bundle, d.id).length;
    }
    return n;
}

export function verifyingTicketCount(bundle: HostSpaceTicketsBundle): number {
    let n = 0;
    for (const d of bundle.desks) {
        n += verifyingTicketsForDesk(bundle, d.id).length;
    }
    return n;
}

export function ticketNumericId(ticket: Record<string, unknown>): number | null {
    const tid = ticket['ticketID'];
    const n = typeof tid === 'number' ? tid : Number(tid);
    return Number.isFinite(n) ? n : null;
}

export function normalizeTicketSeverity(
    severity: unknown,
    options: ReadonlyArray<{ value: string }> = HOST_TICKET_SEVERITY_OPTIONS
): string {
    const s = typeof severity === 'string' ? severity.trim().toUpperCase() : '';
    return options.some((option) => option.value === s) ? s : 'MEDIUM';
}

export function workerLabel(ticket: Record<string, unknown>): string {
    const fn = (ticket['workerName'] as string | undefined)?.trim() ?? '';
    const ln = (ticket['workerSurname'] as string | undefined)?.trim() ?? '';
    return [fn, ln].filter(Boolean).join(' ').trim();
}

export function assignedTechnicianLabel(ticket: Record<string, unknown>): string {
    const fn = (ticket['assignedTechName'] as string | undefined)?.trim();
    const ln = (ticket['assignedTechSurname'] as string | undefined)?.trim();
    const combined = [fn, ln].filter(Boolean).join(' ').trim();
    if (combined) {
        return combined;
    }
    const id = ticket['assignedTechID'];
    return id != null ? `ID ${id}` : '';
}

export function technicianDisplayCode(t: HostTechnicianDto): string {
    const raw = t.technicianCode?.trim();
    if (raw) {
        return raw;
    }
    return `TC-${String(t.technicianID).padStart(6, '0')}`;
}

export function ticketHeading(t: Record<string, unknown>): string {
    const title = (t['title'] as string | undefined)?.trim();
    if (title) {
        return title;
    }
    const code = t['ticketCode'] as string | undefined;
    const id = t['ticketID'] as number | undefined;
    return code ? code : id != null ? `Segnalazione #${id}` : 'Segnalazione';
}

export function ticketCodeSubtitle(t: Record<string, unknown>): string | null {
    const raw = (t['title'] as string | undefined)?.trim();
    if (!raw) {
        return null;
    }
    const code = t['ticketCode'] as string | undefined;
    const id = t['ticketID'] as number | undefined;
    return code ? `ticket#${code}` : id != null ? `Ticket #${id}` : null;
}

export function ticketTooltipTitle(t: Record<string, unknown>): string | null {
    const raw = (t['title'] as string | undefined)?.trim();
    return raw || null;
}

export function ticketRawTitleOrDash(t: Record<string, unknown>): string {
    const raw = (t['title'] as string | undefined)?.trim();
    return raw || '—';
}

export function ticketRefLineOnly(t: Record<string, unknown>): string {
    const code = t['ticketCode'] as string | undefined;
    const id = t['ticketID'] as number | undefined;
    if (code) {
        return `ticket#${code}`;
    }
    return id != null ? `Ticket #${id}` : '—';
}

export function deskRowPrimaryLabel(desk: Desk): string {
    return desk.code.trim() ? desk.code : `Postazione #${desk.id}`;
}

export function workstationSubtitleForDesk(desk: Desk): string {
    const parts: string[] = [];
    const roomTrim = desk.roomName?.trim();
    const buildingTrim = desk.building.trim();
    if (roomTrim) {
        parts.push(`Sala: ${roomTrim}`);
    }
    else {
        parts.push('Senza sala');
    }
    const buildingDistinctFromRoom = !roomTrim || buildingTrim.localeCompare(roomTrim, undefined, { sensitivity: 'base' }) !== 0;
    if (buildingTrim && buildingDistinctFromRoom) {
        parts.push(buildingTrim);
    }
    return parts.join(' · ');
}

export function deskRowSecondaryLabel(desk: Desk, activeTicketCount: number): string {
    const parts: string[] = [];
    if (desk.building.trim()) {
        parts.push(desk.building.trim());
    }
    parts.push(activeTicketCount === 1 ? '1 segnalazione attiva' : `${activeTicketCount} segnalazioni attive`);
    return parts.join(' · ');
}

export function buildRoomsWithUnresolvedTickets(bundle: HostSpaceTicketsBundle): ModalRoomRow[] {
    const withOpen = bundle.desks.filter((d) => unresolvedTicketsForDesk(bundle, d.id).length > 0);
    const rows: ModalRoomRow[] = [];
    const orphanDesks = withOpen.filter((d) => !d.room);
    if (orphanDesks.length) {
        const ticketN = orphanDesks.reduce((s, d) => s + unresolvedTicketsForDesk(bundle, d.id).length, 0);
        rows.push({
            key: 'NO_ROOM',
            label: 'Senza sala',
            meta: `${ticketN} attive · ${orphanDesks.length} postazioni`,
            deskCount: orphanDesks.length
        });
    }
    const byRoomId = new Map<number, Desk[]>();
    for (const d of withOpen) {
        if (!d.room) {
            continue;
        }
        const id = d.room.id;
        const list = byRoomId.get(id) ?? [];
        list.push(d);
        byRoomId.set(id, list);
    }
    const roomRows: ModalRoomRow[] = [];
    for (const [, ds] of byRoomId.entries()) {
        const sample = ds[0]!;
        const r = sample.room!;
        const ticketN = ds.reduce((s, d) => s + unresolvedTicketsForDesk(bundle, d.id).length, 0);
        roomRows.push({
            key: `room-${r.id}`,
            label: r.name,
            meta: `${r.code} · ${ticketN} attive · ${ds.length} postazioni`,
            deskCount: ds.length
        });
    }
    roomRows.sort((a, b) => a.label.localeCompare(b.label, 'it', { sensitivity: 'base' }));
    return [...roomRows, ...rows.filter((x) => x.key === 'NO_ROOM')];
}

export function buildAllVerifyingTickets(bundles: HostSpaceTicketsBundle[]): HostPendingApprovalItem[] {
    const rows: HostPendingApprovalItem[] = [];
    for (const bundle of bundles) {
        for (const desk of bundle.desks) {
            const verifyingTickets = verifyingTicketsForDesk(bundle, desk.id);
            if (verifyingTickets.length > 0) {
                for (const ticket of verifyingTickets) {
                    rows.push({ ticket, desk, space: bundle.space });
                }
            }
            else if (desk.currentState === 'PENDING_INSPECTION') {
                rows.push({ desk, space: bundle.space });
            }
        }
    }
    rows.sort((a, b) => {
        const ta = (a.ticket?.['createdAt'] as string | undefined) ?? '';
        const tb = (b.ticket?.['createdAt'] as string | undefined) ?? '';
        return tb.localeCompare(ta);
    });
    return rows;
}

export function buildPendingApprovalDesksByRoom(list: HostPendingApprovalItem[]): PendingApprovalRoomGroup[] {
    const map = new Map<number, PendingApprovalRoomGroup>();
    for (const item of list) {
        const rid = item.desk.roomID ?? HOST_TICKETS_NO_ROOM_ID;
        const rname = item.desk.roomName ?? 'Senza sala';
        if (!map.has(rid)) {
            map.set(rid, { roomId: rid, roomName: rname, desks: [] });
        }
        map.get(rid)!.desks.push(item);
    }
    const arr = Array.from(map.values());
    arr.sort((a, b) => {
        if (a.roomId === HOST_TICKETS_NO_ROOM_ID) {
            return 1;
        }
        if (b.roomId === HOST_TICKETS_NO_ROOM_ID) {
            return -1;
        }
        return a.roomName.localeCompare(b.roomName, 'it', { sensitivity: 'base' });
    });
    for (const r of arr) {
        r.desks.sort((d1, d2) => d1.desk.code.localeCompare(d2.desk.code, 'it', { numeric: true }));
    }
    return arr;
}

export function sortBundlesBySpaceName(bundles: HostSpaceTicketsBundle[]): HostSpaceTicketsBundle[] {
    return [...bundles].sort((a, b) => a.space.name.localeCompare(b.space.name, 'it', { sensitivity: 'base' }));
}

export function desksWithOpenTicketsInRoom(bundle: HostSpaceTicketsBundle, roomKey: string): Desk[] {
    const withOpen = bundle.desks.filter((d) => unresolvedTicketsForDesk(bundle, d.id).length > 0);
    let desks = roomKey === 'NO_ROOM'
        ? withOpen.filter((d) => !d.room)
        : roomKey.startsWith('room-')
            ? withOpen.filter((d) => d.room?.id === Number(roomKey.slice('room-'.length)))
            : [];
    desks = [...desks];
    desks.sort((a, b) => a.code.localeCompare(b.code, 'it', { sensitivity: 'base' }));
    return desks;
}

export function modalSelectedRoomTitle(bundle: HostSpaceTicketsBundle, roomKey: string): string {
    if (roomKey === 'NO_ROOM') {
        return 'Senza sala';
    }
    if (roomKey.startsWith('room-')) {
        const id = Number(roomKey.slice('room-'.length));
        const desk = bundle.desks.find((d) => d.room?.id === id);
        const name = desk?.roomName ?? 'Sala';
        const code = desk?.roomCode ?? '';
        return code ? `${name} (${code})` : name;
    }
    return '';
}

export function filterRoomRowsBySearch(rows: ModalRoomRow[], query: string): ModalRoomRow[] {
    const q = normalizeForSearch(query);
    if (!q) {
        return rows;
    }
    return rows.filter((r) => normalizeForSearch(`${r.label} ${r.meta}`).includes(q));
}

export function filterDesksBySearch(desks: Desk[], query: string): Desk[] {
    const q = normalizeForSearch(query);
    if (!q) {
        return desks;
    }
    return desks.filter((d) => {
        const label = `${d.code} ${d.building} ${d.roomName ?? ''} ${d.id}`;
        return normalizeForSearch(label).includes(q);
    });
}

export function resolvedTicketMatchesModalSearch(t: Record<string, unknown>, q: string): boolean {
    const haystack = normalizeForSearch([
        ticketHeading(t),
        ticketRefLineOnly(t),
        workerLabel(t),
        assignedTechnicianLabel(t),
        (t['deskCode'] as string | undefined) ?? '',
        (t['spaceName'] as string | undefined) ?? '',
        (t['officeCode'] as string | undefined) ?? ''
    ].join(' '));
    return haystack.includes(q);
}

export function verifyingRowMatchesModalSearch(row: HostPendingApprovalItem, q: string): boolean {
    const t = row.ticket;
    const haystack = normalizeForSearch([
        deskRowPrimaryLabel(row.desk),
        row.space.name,
        row.space.officeCode ?? '',
        t ? ticketHeading(t) : '',
        t ? ticketRefLineOnly(t) : '',
        t ? workerLabel(t) : '',
        t ? assignedTechnicianLabel(t) : ''
    ].join(' '));
    return haystack.includes(q);
}

export function filterAssignTechnicians(
    candidates: HostTechnicianDto[],
    searchQuery: string,
    reassigning: boolean,
    currentTechId: number | null
): HostTechnicianDto[] {
    let rows = [...candidates];
    const q = normalizeForSearch(searchQuery);
    if (q) {
        rows = rows.filter((t) => {
            const hay = [
                t.name,
                t.email,
                t.specialization ?? '',
                String(t.technicianID),
                technicianDisplayCode(t),
                ...(t.assignedSpaces ?? []).flatMap((s) => [s.name, s.officeCode ?? '', String(s.spaceID)])
            ]
                .join(' ')
                .toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '');
            return hay.includes(q);
        });
    }
    if (reassigning && currentTechId != null && Number.isFinite(currentTechId)) {
        rows = [...rows].sort((a, b) => {
            if (a.technicianID === currentTechId) {
                return -1;
            }
            if (b.technicianID === currentTechId) {
                return 1;
            }
            return (a.name ?? '').localeCompare(b.name ?? '', 'it', { sensitivity: 'base' });
        });
    }
    return rows;
}

export function approvalSummaryBullets(row: HostPendingApprovalItem): { label: string; value: string }[] {
    const bullets: { label: string; value: string }[] = [
        { label: 'Ufficio', value: row.space.name },
        { label: 'Postazione', value: deskRowPrimaryLabel(row.desk) }
    ];
    if (row.ticket) {
        bullets.push({ label: 'Segnalazione', value: ticketRefLineOnly(row.ticket) || '—' });
        const worker = workerLabel(row.ticket);
        if (worker) {
            bullets.push({ label: 'Segnalato da', value: worker });
        }
        const tech = assignedTechnicianLabel(row.ticket);
        if (tech) {
            bullets.push({ label: 'Riparato da', value: tech });
        }
    }
    return bullets;
}

export function approvalDetailBullets(row: HostPendingApprovalItem): { label: string; value: string }[] {
    return approvalSummaryBullets(row).filter((f) => f.label !== 'Segnalato da');
}

export function approvalResolutionSnippet(ticket: Record<string, unknown>): string | null {
    const raw = (ticket['resolution'] as string | undefined)?.trim();
    if (!raw) {
        return null;
    }
    return raw.length > 160 ? `${raw.slice(0, 157)}…` : raw;
}

export function modalStepHeading(step: HostTicketsModalStep): string {
    switch (step) {
        case 'rooms':
            return 'Sala';
        case 'desks':
            return 'Postazioni';
        case 'detail':
            return 'Dettaglio';
        default:
            return '';
    }
}

export function maintDeskMaintenanceNotice(deskLabel?: string): string {
    const desk = deskLabel?.trim();
    if (desk) {
        return `La postazione ${desk} tornerà in stato manutenzione e il ticket passerà in lavorazione.`;
    }
    return 'La postazione collegata tornerà in stato manutenzione e la segnalazione passerà in lavorazione.';
}

export function ticketNeedsTechnicianAssignment(ticket: Record<string, unknown>): boolean {
    return ticket['status'] === 'OPEN' && ticket['assignedTechID'] == null;
}

export function ticketDescriptionText(ticket: Record<string, unknown>): string {
    const d = ticket['description'];
    return typeof d === 'string' ? d.trim() : '';
}

export function isCurrentAssignTechnician(ticket: Record<string, unknown> | null, tech: HostTechnicianDto): boolean {
    if (!ticket || ticket['assignedTechID'] == null) {
        return false;
    }
    return Number(ticket['assignedTechID']) === tech.technicianID;
}

export function patchTicketInBundle(bundle: HostSpaceTicketsBundle, updated: Record<string, unknown>): HostSpaceTicketsBundle {
    const tid = updated['ticketID'] as number;
    const newMap = new Map(bundle.ticketsByDeskId);
    let changed = false;
    newMap.forEach((tickets, deskId) => {
        const idx = tickets.findIndex((t) => t['ticketID'] === tid);
        if (idx === -1) {
            return;
        }
        const next = [...tickets];
        next[idx] = updated;
        newMap.set(deskId, next);
        changed = true;
    });
    return changed ? { ...bundle, ticketsByDeskId: newMap } : bundle;
}

export function cloneTicketRecord(ticket: Record<string, unknown>): Record<string, unknown> {
    const history = ticket['hostNoteHistory'];
    return {
        ...ticket,
        hostNoteHistory: Array.isArray(history) ? [...history] : []
    };
}

export function appendHostNoteToTicket(
    ticket: Record<string, unknown>,
    body: string,
    authorLabel: string
): Record<string, unknown> {
    const prev = Array.isArray(ticket['hostNoteHistory'])
        ? (ticket['hostNoteHistory'] as TicketNoteMessageDto[])
        : [];
    const note: TicketNoteMessageDto = {
        body,
        createdAt: new Date().toISOString(),
        authorLabel
    };
    return {
        ...ticket,
        hostNoteHistory: [...prev, note]
    };
}
