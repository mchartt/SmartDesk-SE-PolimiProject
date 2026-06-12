import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
export class DecommissionedDeskState implements IDeskState {
    public readonly code: DeskStateCode = 'DECOMMISSIONED';
    public getName(): string {
        return 'Dismessa';
    }
    public canBeBooked(): boolean {
        return false;
    }
    public getBadgeClass(): string {
        return 'state-decommissioned';
    }
}
