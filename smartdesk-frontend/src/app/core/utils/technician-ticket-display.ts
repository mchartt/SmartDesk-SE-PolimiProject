import type { TechnicianTicketRow } from '../services/technician.service';
export function technicianTicketPrimaryHeading(t: TechnicianTicketRow): string {
    const title = (t.title ?? '').trim();
    if (title) {
        return title;
    }
    return t.ticketCode ? `ticket#${t.ticketCode}` : `Ticket #${t.ticketID}`;
}
export function technicianTicketRefSubtitle(t: TechnicianTicketRow): string | null {
    const title = (t.title ?? '').trim();
    if (!title) {
        return null;
    }
    return t.ticketCode ? `ticket#${t.ticketCode}` : `Ticket #${t.ticketID}`;
}
export function technicianTicketTitleTooltip(t: TechnicianTicketRow): string {
    return (t.title ?? '').trim();
}
