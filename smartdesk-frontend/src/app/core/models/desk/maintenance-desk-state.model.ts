import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
export class MaintenanceDeskState implements IDeskState {
    public readonly code: DeskStateCode = 'MAINTENANCE';
    public readonly user?: string = undefined;
    public getName(): string {
        return 'In Manutenzione';
    }
    public canBeBooked(): boolean {
        return false;
    }
    public getBadgeClass(): string {
        return 'state-maintenance';
    }
}
