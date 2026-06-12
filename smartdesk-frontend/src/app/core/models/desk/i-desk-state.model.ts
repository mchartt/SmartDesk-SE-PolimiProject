import { DeskStateCode } from './desk-state-code.type';
export interface IDeskState {
    getName(): string;
    canBeBooked(): boolean;
    getBadgeClass(): string;
    readonly code: DeskStateCode;
    readonly user?: string;
}
