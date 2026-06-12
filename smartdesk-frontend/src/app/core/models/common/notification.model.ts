import { INotifiable } from './i-notifiable.model';
import { Identifiable } from './identifiable.model';
export class NotificationModel implements INotifiable, Identifiable {
    public constructor(public readonly id: number, private readonly _title: string, private _read = false, public readonly createdAt: string | null = null, public readonly kind: string | null = null, public readonly actorName: string | null = null, public readonly actorSurname: string | null = null, public readonly actorEmail: string | null = null, public readonly actorRating: number | null = null) { }
    public get title(): string {
        return this._title;
    }
    public get read(): boolean {
        return this._read;
    }
    public markAsRead(): void {
        this._read = true;
    }
    public withRead(read: boolean): NotificationModel {
        return new NotificationModel(this.id, this._title, read, this.createdAt, this.kind, this.actorName, this.actorSurname, this.actorEmail, this.actorRating);
    }
    public getNotificationText(): string {
        return this._title;
    }
}
