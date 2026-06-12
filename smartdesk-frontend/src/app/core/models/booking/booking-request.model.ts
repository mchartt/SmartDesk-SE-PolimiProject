export class BookingRequest {
    private _deskID: number;
    private _startTime?: string;
    private _end: string;
    public constructor(deskID: number, end: string, startTime?: string) {
        this._deskID = deskID;
        this._end = end;
        this._startTime = startTime;
    }
    public get deskID(): number {
        return this._deskID;
    }
    public set deskID(value: number) {
        this._deskID = value;
    }
    public get startTime(): string | undefined {
        return this._startTime;
    }
    public set startTime(value: string | undefined) {
        this._startTime = value;
    }
    public get end(): string {
        return this._end;
    }
    public set end(value: string) {
        this._end = value;
    }
}
