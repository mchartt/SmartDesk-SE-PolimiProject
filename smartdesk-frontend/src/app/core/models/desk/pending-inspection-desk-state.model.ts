import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
export class PendingInspectionDeskState implements IDeskState {
    public readonly code: DeskStateCode = 'PENDING_INSPECTION';
    public getName(): string {
        return 'In attesa di ispezione';
    }
    public canBeBooked(): boolean {
        return false;
    }
    public getBadgeClass(): string {
        return 'state-pending-inspection';
    }
}
