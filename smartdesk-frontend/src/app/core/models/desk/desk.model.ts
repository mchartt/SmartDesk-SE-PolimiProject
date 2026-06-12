import { AvailableDeskState } from './available-desk-state.model';
import { BookedDeskState } from './booked-desk-state.model';
import { DecommissionedDeskState } from './decommissioned-desk-state.model';
import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
import { MaintenanceDeskState } from './maintenance-desk-state.model';
import { PendingInspectionDeskState } from './pending-inspection-desk-state.model';
export interface DeskRoomMeta {
    id: number;
    code: string;
    name: string;
}
export class Desk {
    private _id: number;
    private _code: string;
    private _building: string;
    private _amenities: string[];
    private _state: IDeskState;
    private _room: DeskRoomMeta | null;
    private readonly _spaceID: number | null;
    private readonly _spaceAverageRating: number | null;
    public constructor(id: number, code: string, building: string, amenities: string[], stateCode: DeskStateCode = 'AVAILABLE', room: DeskRoomMeta | null = null, spaceID: number | null = null, spaceAverageRating: number | null = null) {
        this._id = id;
        this._code = code;
        this._building = building;
        this._amenities = [...amenities];
        this._state = Desk.resolveState(stateCode);
        this._room = room;
        this._spaceID = spaceID;
        this._spaceAverageRating = spaceAverageRating;
    }
    public get room(): DeskRoomMeta | null {
        return this._room;
    }
    public set room(value: DeskRoomMeta | null) {
        this._room = value;
    }
    public get roomID(): number | undefined {
        return this._room?.id;
    }
    public get roomCode(): string | undefined {
        return this._room?.code;
    }
    public get roomName(): string | undefined {
        return this._room?.name;
    }
    public get spaceID(): number | null {
        return this._spaceID;
    }
    public get spaceAverageRating(): number | null {
        return this._spaceAverageRating;
    }
    public get id(): number {
        return this._id;
    }
    public set id(value: number) {
        this._id = value;
    }
    public get code(): string {
        return this._code;
    }
    public set code(value: string) {
        this._code = value;
    }
    public get building(): string {
        return this._building;
    }
    public set building(value: string) {
        this._building = value.trim();
    }
    public get amenities(): string[] {
        return [...this._amenities];
    }
    public set amenities(value: string[]) {
        this._amenities = [...value];
    }
    public get state(): IDeskState {
        return this._state;
    }
    public changeState(nextState: IDeskState): void {
        this._state = nextState;
    }
    public addAmenity(amenity: string): void {
        if (!this._amenities.includes(amenity)) {
            this._amenities.push(amenity);
        }
    }
    public hasRequiredAmenities(requiredAmenities: string[]): boolean {
        return requiredAmenities.every((requiredAmenity) => this._amenities.includes(requiredAmenity));
    }
    public isBookable(): boolean {
        return this._state.canBeBooked();
    }
    public getStateLabel(): string {
        return this._state.getName();
    }
    public get currentState(): DeskStateCode {
        return this._state.code;
    }
    public static resolveState(code: DeskStateCode | string): IDeskState {
        const normalized = String(code ?? 'AVAILABLE').trim().toUpperCase() as DeskStateCode;
        switch (normalized) {
            case 'BOOKED':
            case 'RESERVED':
                return new BookedDeskState();
            case 'MAINTENANCE':
                return new MaintenanceDeskState();
            case 'PENDING_INSPECTION':
                return new PendingInspectionDeskState();
            case 'DECOMMISSIONED':
                return new DecommissionedDeskState();
            default:
                return new AvailableDeskState();
        }
    }
}
