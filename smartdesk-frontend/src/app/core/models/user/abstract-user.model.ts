import { UserRole } from './user-role.type';
function titleCaseItalianToken(token: string): string {
    const t = token.trim();
    if (!t) {
        return '';
    }
    if (t.includes('-')) {
        return t
            .split('-')
            .map((p) => p ? p.charAt(0).toLocaleUpperCase('it-IT') + p.slice(1).toLocaleLowerCase('it-IT') : '')
            .join('-');
    }
    return t.charAt(0).toLocaleUpperCase('it-IT') + t.slice(1).toLocaleLowerCase('it-IT');
}
function expandSlugLikeSingleField(singleField: string): string {
    const t = singleField.trim();
    if (!t) {
        return '';
    }
    if (/\s/.test(t)) {
        return t.split(/\s+/).map(titleCaseItalianToken).filter(Boolean).join(' ');
    }
    if (/[._]/.test(t)) {
        return t.split(/[._]+/).map(titleCaseItalianToken).filter(Boolean).join(' ');
    }
    return titleCaseItalianToken(t);
}
function buildDisplayName(name: string, surname: string, email: string): string {
    const n = (name ?? '').trim();
    const s = (surname ?? '').trim();
    if (n && s) {
        return `${titleCaseItalianToken(n)} ${titleCaseItalianToken(s)}`.trim();
    }
    if (n) {
        return expandSlugLikeSingleField(n);
    }
    if (s) {
        return expandSlugLikeSingleField(s);
    }
    const local = (email ?? '').split('@')[0]?.trim() ?? '';
    return expandSlugLikeSingleField(local);
}
export abstract class AbstractUser {
    private _id: number;
    private _email: string;
    private _surname: string;
    private _name: string;
    private _active: boolean;
    protected constructor(id: number, email: string, surname: string, name: string, active = true) {
        this._id = id;
        this._email = email;
        this._surname = surname;
        this._name = name;
        this._active = active;
    }
    public get id(): number {
        return this._id;
    }
    public set id(value: number) {
        this._id = value;
    }
    public get email(): string {
        return this._email;
    }
    public set email(value: string) {
        this._email = value.trim().toLowerCase();
    }
    public get surname(): string {
        return this._surname;
    }
    public set surname(value: string) {
        this._surname = value.trim();
    }
    public get name(): string {
        return this._name;
    }
    public set name(value: string) {
        this._name = value.trim();
    }
    public get active(): boolean {
        return this._active;
    }
    public set active(value: boolean) {
        this._active = value;
    }
    public get fullName(): string {
        return `${this._name} ${this._surname}`.trim();
    }
    public get displayName(): string {
        return buildDisplayName(this._name, this._surname, this._email);
    }
    public deactivate(): void {
        this._active = false;
    }
    public reactivate(): void {
        this._active = true;
    }
    public abstract getRole(): UserRole;
    public abstract getDashboardTitle(): string;
    public abstract canOpenTicket(): boolean;
}
