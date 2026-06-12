export class SearchCriteria {
    private _targetDate: string;
    private _requiredAmenities: string[];
    private _includeMaintenance: boolean;
    private _slotStart: string | null;
    private _slotEnd: string | null;
    private _spaceId: number | null;
    public constructor(targetDate: string, requiredAmenities: string[] = [], includeMaintenance = false, slotStart: string | null = null, slotEnd: string | null = null, spaceId: number | null = null) {
        this._targetDate = targetDate;
        this._requiredAmenities = [...requiredAmenities];
        this._includeMaintenance = includeMaintenance;
        this._slotStart = slotStart;
        this._slotEnd = slotEnd;
        this._spaceId = spaceId;
    }
    public get targetDate(): string {
        return this._targetDate;
    }
    public set targetDate(value: string) {
        this._targetDate = value;
    }
    public get requiredAmenities(): string[] {
        return [...this._requiredAmenities];
    }
    public set requiredAmenities(value: string[]) {
        this._requiredAmenities = [...value];
    }
    public get includeMaintenance(): boolean {
        return this._includeMaintenance;
    }
    public set includeMaintenance(value: boolean) {
        this._includeMaintenance = value;
    }
    public get slotStart(): string | null {
        return this._slotStart;
    }
    public set slotStart(value: string | null) {
        this._slotStart = value;
    }
    public get slotEnd(): string | null {
        return this._slotEnd;
    }
    public set slotEnd(value: string | null) {
        this._slotEnd = value;
    }
    public get spaceId(): number | null {
        return this._spaceId;
    }
    public set spaceId(value: number | null) {
        this._spaceId = value;
    }
}
