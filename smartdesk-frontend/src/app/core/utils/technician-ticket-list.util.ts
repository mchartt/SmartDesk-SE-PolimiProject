import type { TechnicianTicketRow } from '../services/technician.service';
import { ticketStatusLabel as sharedTicketStatusLabel } from './ticket-status-display.util';
export type TechnicianSpaceFilterValue = 'all' | 'out-of-scope' | number;
export function filterTechnicianTicketsBySpace(tickets: TechnicianTicketRow[], filter: TechnicianSpaceFilterValue, assignedSpaceIds: Set<number>): TechnicianTicketRow[] {
    if (filter === 'all') {
        return tickets;
    }
    if (filter === 'out-of-scope') {
        return tickets.filter((t) => {
            const sid = t.spaceID ?? null;
            return sid == null || !assignedSpaceIds.has(sid);
        });
    }
    return tickets.filter((t) => t.spaceID === filter);
}
export function technicianDeskLabel(t: TechnicianTicketRow): string {
    return t.deskCode ? `desk#${t.deskCode}` : t.deskID != null ? `Desk #${t.deskID}` : 'Desk';
}
export function technicianSpaceCaption(t: TechnicianTicketRow): string {
    const name = (t.spaceName ?? '').trim();
    const code = (t.officeCode ?? '').trim();
    if (name && code) {
        return `${name} · ${code}`;
    }
    if (name) {
        return name;
    }
    if (code) {
        return `Ufficio ${code}`;
    }
    return '';
}
export function technicianTicketStatusLabel(status: string | undefined): string {
    return sharedTicketStatusLabel(status);
}
export function technicianTicketMatchesSearchQuery(t: TechnicianTicketRow, raw: string): boolean {
    const q = raw
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/\p{M}/gu, '')
        .replace(/^ticket#/, '')
        .replace(/^#/, '');
    if (!q) {
        return true;
    }
    const hay = [
        (t.title ?? '').trim(),
        (t.ticketCode ?? '').trim(),
        t.ticketCode ? `ticket#${t.ticketCode}` : '',
        String(t.ticketID)
    ]
        .join(' ')
        .toLowerCase()
        .normalize('NFD')
        .replace(/\p{M}/gu, '');
    return hay.includes(q);
}
export function technicianTicketSeverityLabel(severity: string | null | undefined): string {
    const s = (severity ?? '').toUpperCase();
    if (s === 'CRITICAL') {
        return 'Critica';
    }
    if (s === 'HIGH') {
        return 'Alta';
    }
    if (s === 'MEDIUM') {
        return 'Media';
    }
    if (s === 'LOW') {
        return 'Bassa';
    }
    return severity ?? '';
}
