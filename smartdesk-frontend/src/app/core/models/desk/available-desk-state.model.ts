import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
export class AvailableDeskState implements IDeskState {
    public readonly code: DeskStateCode = 'AVAILABLE';
    public readonly user?: string = undefined;
    public getName(): string {
        return 'Disponibile';
    }
    public canBeBooked(): boolean {
        return true;
    }
    public getBadgeClass(): string {
        return 'state-available';
    }
}
