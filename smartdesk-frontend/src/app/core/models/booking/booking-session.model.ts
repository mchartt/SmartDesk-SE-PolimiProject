import { Desk } from '../desk/desk.model';
import { Worker } from '../user/worker.model';
export class BookingSession {
    public constructor(private _worker: Worker, private _desk: Desk) { }
    public get worker(): Worker {
        return this._worker;
    }
    public get desk(): Desk {
        return this._desk;
    }
}
