export class Booking {
    private _bookingID: number;
    private _deskID: number;
    private _deskCode: string;
    private _spaceName: string;
    private _city: string;
    private _buildingName: string;
    private _startTime: string;
    private _endTime: string;
    private _status: string;
    private _bookedDay: string;
    private _bookingCode: string;
    private _workerID: number | null;
    private _workerEmail: string;
    private _workerName: string;
    private _version: number;
    public constructor(bookingID: number, deskID: number, startTime: string, endTime: string, status: string = 'PENDING', bookedDay: string | null | undefined = undefined, deskCode: string = '', spaceName: string = '', buildingName: string = '', city: string = '', bookingCode: string = '', workerID: number | null = null, workerEmail: string = '', workerName: string = '', version: number = 0) {
        this._bookingID = bookingID;
        this._deskID = deskID;
        this._deskCode = deskCode;
        this._spaceName = spaceName;
        this._city = (city ?? '').trim();
        this._buildingName = buildingName;
        this._startTime = startTime;
        this._endTime = endTime;
        this._status = status;
        this._bookedDay = bookedDay?.trim() ?? '';
        this._bookingCode = (bookingCode ?? '').trim();
        this._workerID = workerID ?? null;
        this._workerEmail = (workerEmail ?? '').trim();
        this._workerName = (workerName ?? '').trim();
        this._version = Number.isFinite(version) ? version : 0;
    }
    public get bookingID(): number {
        return this._bookingID;
    }
    public set bookingID(value: number) {
        this._bookingID = value;
    }
    public get deskID(): number {
        return this._deskID;
    }
    public set deskID(value: number) {
        this._deskID = value;
    }
    public get deskCode(): string {
        return this._deskCode;
    }
    public set deskCode(value: string) {
        this._deskCode = value;
    }
    public get spaceName(): string {
        return this._spaceName;
    }
    public set spaceName(value: string) {
        this._spaceName = value;
    }
    public get city(): string {
        return this._city;
    }
    public set city(value: string) {
        this._city = (value ?? '').trim();
    }
    public get buildingName(): string {
        return this._buildingName;
    }
    public set buildingName(value: string) {
        this._buildingName = value;
    }
    public get startTime(): string {
        return this._startTime;
    }
    public set startTime(value: string) {
        this._startTime = value;
    }
    public get endTime(): string {
        return this._endTime;
    }
    public set endTime(value: string) {
        this._endTime = value;
    }
    public get status(): string {
        return this._status;
    }
    public set status(value: string) {
        this._status = value;
    }
    public get bookedDay(): string {
        return this._bookedDay;
    }
    public set bookedDay(value: string) {
        this._bookedDay = value?.trim() ?? '';
    }
    public get bookingCode(): string {
        return this._bookingCode;
    }
    public set bookingCode(value: string) {
        this._bookingCode = (value ?? '').trim();
    }
    public get workerID(): number | null {
        return this._workerID;
    }
    public get workerEmail(): string {
        return this._workerEmail;
    }
    public get workerName(): string {
        return this._workerName;
    }
    public get version(): number {
        return this._version;
    }
    public set version(value: number) {
        this._version = Number.isFinite(value) ? value : 0;
    }
}
