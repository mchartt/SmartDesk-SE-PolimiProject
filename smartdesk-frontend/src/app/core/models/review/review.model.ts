export class Review {
    private _reviewID: number;
    private _bookingID: number;
    private _hostID: number;
    private _workerID: number;
    private _rating: number;
    private _spaceID: number;
    private _comment: string;
    private _spaceName: string;
    private _city: string;
    private _spaceOfficeCode: string;
    private _createdAt: string | null;
    private _workerGivenName: string;
    private _workerFamilyName: string;
    private _workerEmail: string;
    private _seenByHost: boolean;
    private _bookingCode: string;
    public constructor(reviewID: number, bookingID: number, hostID: number, workerID: number, rating: number, spaceID: number, comment: string, spaceName = '', city = '', spaceOfficeCode = '', createdAt: string | null = null, workerGivenName = '', workerFamilyName = '', workerEmail = '', seenByHost = false, bookingCode = '') {
        this._reviewID = reviewID;
        this._bookingID = bookingID;
        this._hostID = hostID;
        this._workerID = workerID;
        this._rating = rating;
        this._spaceID = spaceID;
        this._comment = comment;
        this._spaceName = spaceName ?? '';
        this._city = city ?? '';
        this._spaceOfficeCode = (spaceOfficeCode ?? '').trim();
        this._createdAt = createdAt;
        this._workerGivenName = (workerGivenName ?? '').trim();
        this._workerFamilyName = (workerFamilyName ?? '').trim();
        this._workerEmail = (workerEmail ?? '').trim();
        this._seenByHost = !!seenByHost;
        this._bookingCode = (bookingCode ?? '').trim();
    }
    public get reviewID(): number {
        return this._reviewID;
    }
    public get bookingID(): number {
        return this._bookingID;
    }
    public get bookingCode(): string {
        return this._bookingCode;
    }
    public get hostID(): number {
        return this._hostID;
    }
    public get workerID(): number {
        return this._workerID;
    }
    public set hostID(value: number) {
        this._hostID = value;
    }
    public get rating(): number {
        return this._rating;
    }
    public set rating(value: number) {
        this._rating = Math.max(1, Math.min(5, value));
    }
    public get spaceID(): number {
        return this._spaceID;
    }
    public set spaceID(value: number) {
        this._spaceID = value;
    }
    public get comment(): string {
        return this._comment;
    }
    public set comment(value: string) {
        this._comment = value.trim();
    }
    public get spaceName(): string {
        return this._spaceName;
    }
    public get city(): string {
        return this._city;
    }
    public get spaceOfficeCode(): string {
        return this._spaceOfficeCode;
    }
    public get createdAt(): string | null {
        return this._createdAt;
    }
    public get seenByHost(): boolean {
        return this._seenByHost;
    }
    public set seenByHost(value: boolean) {
        this._seenByHost = !!value;
    }
    public get workerGivenName(): string {
        return this._workerGivenName;
    }
    public get workerFamilyName(): string {
        return this._workerFamilyName;
    }
    public get workerEmail(): string {
        return this._workerEmail;
    }
    public get reviewerFullName(): string {
        return [this._workerGivenName, this._workerFamilyName].filter((p) => p.length > 0).join(' ');
    }
}
