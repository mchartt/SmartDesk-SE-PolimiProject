import { Desk } from '../desk/desk.model';
export interface OpeningHoursDay {
    closed: boolean;
    open: string;
    close: string;
}
export class Space {
    private _spaceID: number;
    private _description: string;
    private _hostID: number;
    private _name: string;
    private _city: string;
    private _address: string;
    private _approved: boolean;
    private _desks: Desk[];
    private _officeCode: string;
    private _openingHours: Record<string, OpeningHoursDay>;
    public constructor(spaceID: number, description: string, hostID: number, name: string, city: string = '', address: string = '', approved: boolean = false, desks: Desk[] = [], officeCode: string = '', openingHours: Record<string, OpeningHoursDay> | null = null) {
        this._spaceID = spaceID;
        this._description = description;
        this._hostID = hostID;
        this._name = name;
        this._city = city;
        this._address = address;
        this._approved = approved;
        this._desks = [...desks];
        this._officeCode = officeCode.trim();
        this._openingHours = Space.cloneOpeningHoursMap(openingHours);
    }
    private static cloneOpeningHoursMap(src: Record<string, OpeningHoursDay> | null): Record<string, OpeningHoursDay> {
        if (!src || typeof src !== 'object')
            return {};
        const out: Record<string, OpeningHoursDay> = {};
        for (const [k, v] of Object.entries(src)) {
            if (!v || typeof v !== 'object')
                continue;
            out[k] = {
                closed: !!v.closed,
                open: (v.open ?? '').trim(),
                close: (v.close ?? '').trim()
            };
        }
        return out;
    }
    public get spaceID(): number {
        return this._spaceID;
    }
    public set spaceID(value: number) {
        this._spaceID = value;
    }
    public get description(): string {
        return this._description;
    }
    public set description(value: string) {
        this._description = value.trim();
    }
    public get hostID(): number {
        return this._hostID;
    }
    public set hostID(value: number) {
        this._hostID = value;
    }
    public get name(): string {
        return this._name;
    }
    public set name(value: string) {
        this._name = value.trim();
    }
    public get approved(): boolean {
        return this._approved;
    }
    public set approved(value: boolean) {
        this._approved = value;
    }
    public get city(): string {
        return this._city;
    }
    public set city(value: string) {
        this._city = value.trim();
    }
    public get address(): string {
        return this._address;
    }
    public set address(value: string) {
        this._address = value.trim();
    }
    public get desks(): Desk[] {
        return [...this._desks];
    }
    public get officeCode(): string {
        return this._officeCode;
    }
    public get openingHours(): Record<string, OpeningHoursDay> {
        const copy: Record<string, OpeningHoursDay> = {};
        for (const [k, v] of Object.entries(this._openingHours)) {
            copy[k] = { ...v };
        }
        return copy;
    }
    public addDesk(desk: Desk): void {
        this._desks.push(desk);
    }
}
