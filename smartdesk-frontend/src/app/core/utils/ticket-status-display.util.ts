import type { SdIconName } from '../../shared/icons/sd-icon/sd-icon.component';
export function normalizeTicketStatus(status: string | undefined): string {
    return (status ?? '').trim().toUpperCase();
}
export function ticketStatusLabel(status: string | undefined): string {
    const s = normalizeTicketStatus(status);
    switch (s) {
        case 'OPEN':
            return 'In attesa';
        case 'IN_PROGRESS':
            return 'In corso';
        case 'VERIFYING':
            return 'In verifica host';
        case 'RESOLVED':
        case 'CLOSED':
            return 'Risoluzione confermata';
        default:
            return status?.trim() || '—';
    }
}
export function ticketStatusIcon(status: string | undefined): SdIconName {
    switch (normalizeTicketStatus(status)) {
        case 'OPEN':
            return 'clock';
        case 'IN_PROGRESS':
            return 'wrench';
        case 'VERIFYING':
            return 'eye';
        case 'RESOLVED':
        case 'CLOSED':
            return 'circle-check';
        default:
            return 'ticket';
    }
}
export function ticketStatusBadgeClass(status: string | undefined, compact = false): string {
    const base = compact
        ? 'sd-ticket-status-badge badge rounded-pill d-inline-flex align-items-center gap-1 border-0 px-2 py-1 small fw-semibold'
        : 'sd-ticket-status-badge badge rounded-pill d-inline-flex align-items-center gap-1 border-0 px-3 py-2 fw-semibold';
    switch (normalizeTicketStatus(status)) {
        case 'OPEN':
            return `${base} bg-warning-subtle text-warning-emphasis`;
        case 'IN_PROGRESS':
            return `${base} bg-primary-subtle text-primary`;
        case 'VERIFYING':
            return `${base} bg-info-subtle text-info-emphasis`;
        case 'RESOLVED':
        case 'CLOSED':
            return `${base} bg-success-subtle text-success`;
        default:
            return `${base} bg-secondary-subtle text-secondary`;
    }
}
export function ticketStatusHint(status: string | undefined): string {
    const s = normalizeTicketStatus(status);
    switch (s) {
        case 'OPEN':
            return 'In attesa che un tecnico prenda in carico la segnalazione.';
        case 'IN_PROGRESS':
            return 'Un tecnico sta lavorando alla risoluzione del problema.';
        case 'VERIFYING':
            return 'Il tecnico ha completato la riparazione; l\'host deve confermare.';
        case 'RESOLVED':
        case 'CLOSED':
            return 'L\'host ha confermato: la segnalazione è chiusa definitivamente.';
        default:
            return 'Stato in aggiornamento.';
    }
}
