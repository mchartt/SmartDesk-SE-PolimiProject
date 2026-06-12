import type { TicketNoteMessageDto } from '../models';
export type TicketChatMessage = {
    id: string;
    role: 'worker' | 'technician' | 'host';
    label: string;
    body: string;
    at: string | null;
};
export type TicketChatViewer = 'worker' | 'technician' | 'host';
type ChatRole = TicketChatMessage['role'];
const ROLE_LABEL: Record<ChatRole, string> = {
    worker: 'Utente',
    technician: 'Tecnico',
    host: 'Host'
};
export type TicketChatSource = {
    ticketID: number;
    description?: string | null;
    createdAt?: string | null;
    technicianNote?: string | null;
    technicianNoteHistory?: TicketNoteMessageDto[] | null;
    workerNoteHistory?: TicketNoteMessageDto[] | null;
    hostNoteHistory?: TicketNoteMessageDto[] | null;
    resolution?: string | null;
    resolvedAt?: string | null;
    workerName?: string | null;
    workerSurname?: string | null;
    assignedTechName?: string | null;
    assignedTechSurname?: string | null;
};
function parseTime(iso: string | null | undefined): number {
    if (!iso) {
        return 0;
    }
    const t = new Date(iso).getTime();
    return Number.isFinite(t) ? t : 0;
}
function formatPersonName(first?: string | null, last?: string | null): string {
    return `${(first ?? '').trim()} ${(last ?? '').trim()}`.trim();
}
function extractRolePart(label: string): string {
    const idx = label.indexOf('·');
    return idx >= 0 ? label.slice(idx + 1).trim() : label.trim();
}
function normalizeAuthorLabel(raw: string | undefined, role: ChatRole, fallback?: {
    first?: string | null;
    last?: string | null;
}): string {
    const roleName = ROLE_LABEL[role];
    let label = (raw ?? '').trim().replace(/\bLavoratore\b/g, 'Utente');
    if (!label) {
        const person = formatPersonName(fallback?.first, fallback?.last);
        return person ? `${person} · ${roleName}` : roleName;
    }
    if (label === 'Tu') {
        return `Tu · ${roleName}`;
    }
    if (!label.includes('·')) {
        const person = formatPersonName(fallback?.first, fallback?.last);
        if (person) {
            return `${person} · ${roleName}`;
        }
        if (label === roleName || label === 'Host' || label === 'Tecnico' || label === 'Utente') {
            return roleName;
        }
    }
    return label;
}
function formatAuthorDisplay(viewer: TicketChatViewer, role: ChatRole, authorLabel?: string | null, fallback?: {
    first?: string | null;
    last?: string | null;
}): string {
    const normalized = normalizeAuthorLabel(authorLabel ?? undefined, role, fallback);
    if (viewer === role) {
        const rolePart = extractRolePart(normalized) || ROLE_LABEL[role];
        return `Tu · ${rolePart}`;
    }
    return normalized;
}
export function buildTicketChatMessages(ticket: TicketChatSource, viewer: TicketChatViewer = 'worker'): TicketChatMessage[] {
    const messages: TicketChatMessage[] = [];
    const workerFallback = { first: ticket.workerName, last: ticket.workerSurname };
    const techFallback = { first: ticket.assignedTechName, last: ticket.assignedTechSurname };
    const desc = (ticket.description ?? '').trim();
    if (desc) {
        messages.push({
            id: `worker-desc-${ticket.ticketID}`,
            role: 'worker',
            label: formatAuthorDisplay(viewer, 'worker', null, workerFallback),
            body: desc,
            at: ticket.createdAt || null
        });
    }
    const workerHistory = ticket.workerNoteHistory ?? [];
    workerHistory.forEach((row, index) => {
        const body = (row.body ?? '').trim();
        if (!body) {
            return;
        }
        messages.push({
            id: `worker-note-${ticket.ticketID}-${index}`,
            role: 'worker',
            label: formatAuthorDisplay(viewer, 'worker', row.authorLabel, workerFallback),
            body,
            at: row.createdAt ?? null
        });
    });
    const techHistory = ticket.technicianNoteHistory ?? [];
    techHistory.forEach((row, index) => {
        const body = (row.body ?? '').trim();
        if (!body) {
            return;
        }
        messages.push({
            id: `tech-${ticket.ticketID}-${index}`,
            role: 'technician',
            label: formatAuthorDisplay(viewer, 'technician', row.authorLabel, techFallback),
            body,
            at: row.createdAt ?? null
        });
    });
    const hostHistory = ticket.hostNoteHistory ?? [];
    hostHistory.forEach((row, index) => {
        const body = (row.body ?? '').trim();
        if (!body) {
            return;
        }
        messages.push({
            id: `host-${ticket.ticketID}-${index}`,
            role: 'host',
            label: formatAuthorDisplay(viewer, 'host', row.authorLabel),
            body,
            at: row.createdAt ?? null
        });
    });
    if (techHistory.length === 0) {
        const legacy = (ticket.technicianNote ?? '').trim();
        if (legacy) {
            messages.push({
                id: `tech-legacy-${ticket.ticketID}`,
                role: 'technician',
                label: formatAuthorDisplay(viewer, 'technician', null, techFallback),
                body: legacy,
                at: ticket.resolvedAt ?? ticket.createdAt ?? null
            });
        }
    }
    const resolution = (ticket.resolution ?? '').trim();
    const lastTech = messages.filter((m) => m.role === 'technician').at(-1)?.body ?? '';
    if (resolution && resolution !== lastTech) {
        messages.push({
            id: `resolution-${ticket.ticketID}`,
            role: 'technician',
            label: formatAuthorDisplay(viewer, 'technician', null, techFallback),
            body: resolution,
            at: ticket.resolvedAt ?? null
        });
    }
    return messages.sort((a, b) => parseTime(a.at) - parseTime(b.at));
}
export function incomingTicketChatMessages(ticket: TicketChatSource): TicketChatMessage[] {
    return buildTicketChatMessages(ticket, 'technician').filter((m) => m.role !== 'technician' && !m.id.startsWith('worker-desc-'));
}
export function workerIncomingChatMessages(ticket: TicketChatSource): TicketChatMessage[] {
    return buildTicketChatMessages(ticket, 'worker').filter((m) => m.role !== 'worker' && !m.id.startsWith('worker-desc-'));
}
export function incomingChatMessageSignature(messages: TicketChatMessage[]): string {
    return messages.map((m) => m.id).join('\u241f');
}
export function countUnseenIncomingChatMessages(ticket: TicketChatSource, seenSignature: string | null | undefined): number {
    const incoming = incomingTicketChatMessages(ticket);
    if (!seenSignature) {
        return incoming.length;
    }
    const seen = new Set(seenSignature.split('\u241f').filter(Boolean));
    return incoming.filter((m) => !seen.has(m.id)).length;
}
export function ticketNoteSignature(ticket: TicketChatSource): string {
    const workerHist = (ticket.workerNoteHistory ?? []).map((n) => n.body).join('\u241e');
    const techHist = (ticket.technicianNoteHistory ?? []).map((n) => n.body).join('\u241e');
    const hostHist = (ticket.hostNoteHistory ?? []).map((n) => n.body).join('\u241e');
    return `${ticket.technicianNote ?? ''}\u241f${techHist}\u241f${workerHist}\u241f${hostHist}\u241f${ticket.resolution ?? ''}`;
}
export function loadChatSeenSignaturesFromStorage(storageKey: string): Record<number, string> {
    try {
        const raw = sessionStorage.getItem(storageKey);
        if (!raw) {
            return {};
        }
        const parsed = JSON.parse(raw) as Record<string, string>;
        const out: Record<number, string> = {};
        for (const [key, value] of Object.entries(parsed)) {
            const id = Number(key);
            if (Number.isFinite(id) && typeof value === 'string') {
                out[id] = value;
            }
        }
        return out;
    }
    catch {
        return {};
    }
}
