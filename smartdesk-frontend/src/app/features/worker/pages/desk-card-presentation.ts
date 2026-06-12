import { Desk } from '../../../core/models';
export type DeskCardStatusKind = 'maintenance' | 'cooldown' | 'available' | 'partial' | 'unavailable';
export class DeskCardPresentation {
    public static readonly headingLabel = 'Postazione';
    public static displayCode(desk: Desk): string {
        const trimmed = desk.code?.trim() ?? '';
        return trimmed.length > 0 ? `desk#${trimmed}` : `#${desk.id}`;
    }
    public static resolveStatus(desk: Desk, availabilityKind?: 'available' | 'partial' | 'occupied' | 'maintenance', isCooldown?: boolean): DeskCardStatusKind {
        if (desk.state.code === 'MAINTENANCE') {
            return 'maintenance';
        }
        if (availabilityKind === 'maintenance') {
            return 'maintenance';
        }
        if (isCooldown) {
            return 'cooldown';
        }
        if (availabilityKind === 'partial') {
            return 'partial';
        }
        if (availabilityKind === 'occupied') {
            return 'unavailable';
        }
        if (!desk.isBookable()) {
            return 'unavailable';
        }
        return 'available';
    }
    public static statusLabel(kind: DeskCardStatusKind): string {
        switch (kind) {
            case 'maintenance':
                return 'MANUTENZIONE';
            case 'cooldown':
                return 'COOLDOWN';
            case 'partial':
                return 'PARZIALMENTE';
            case 'unavailable':
                return 'PRENOTATA';
            default:
                return 'DISPONIBILE';
        }
    }
    public static statusLabelSubline(kind: DeskCardStatusKind): string | null {
        return kind === 'partial' ? 'DISPONIBILE' : null;
    }
    public static statusBadgeClass(kind: DeskCardStatusKind): string {
        const base = 'badge rounded-pill px-3 py-2 fw-normal sd-desk-card__status-badge';
        switch (kind) {
            case 'maintenance':
                return `${base} bg-warning text-dark`;
            case 'cooldown':
                return `${base} bg-secondary text-white`;
            case 'partial':
                return `${base} bg-warning text-white sd-desk-card__status-badge--partial`;
            case 'unavailable':
                return `${base} sd-desk-card__status--unavail`;
            default:
                return `${base} bg-success text-white`;
        }
    }
}
