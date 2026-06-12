import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Booking, BookingRequest, Desk, SearchCriteria } from '../models';
import { ModelFactory } from '../utils/model-factory';
import { normalizeSlotTimeHm } from '../utils/time.util';
import { NotificationService } from './notification.service';
type CatalogDeskDto = {
    id?: number;
    deskID?: number;
    code?: string;
    building?: string;
    roomID?: number;
    roomName?: string;
    roomCode?: string;
    amenities?: string[];
    currentState?: string;
};
type DeskDto = {
    id?: number;
    deskID?: number;
    code?: string;
    building: string;
    amenities: string[];
    currentState?: string;
    bookable?: boolean;
    spaceID?: number;
    spaceAverageRating?: number | null;
};
type BookingDto = {
    bookingID: number;
    bookingCode?: string;
    version?: number;
    deskID: number;
    deskCode?: string;
    spaceName?: string;
    city?: string;
    buildingName?: string;
    bookedDay?: string | null;
    startTime: string;
    endTime: string;
    status?: string;
};
export type SlotStatus = {
    time: string;
    status: 'free' | 'busy';
};
export type OpeningHoursDayPayload = {
    closed?: boolean;
    open?: string;
    close?: string;
};
export type WorkerSpace = {
    spaceID: number;
    name: string;
    city: string;
    deskCount: number;
    officeCode?: string;
    openingHours?: Record<string, OpeningHoursDayPayload>;
    averageReviewRating?: number;
};
export type WorkerSpaceClosure = {
    id: number;
    spaceID: number;
    closedDate: string;
    reason: string;
};
export type WaitlistStatusDto = {
    deskID: number;
    date: string;
    active: boolean;
    position?: number;
};
@Injectable({
    providedIn: 'root'
})
export class BookingService {
    private readonly http = inject(HttpClient);
    private readonly notifications = inject(NotificationService);
    private readonly baseUrl = `${environment.apiUrl}/workers`;
    private static mapHttpError(err: unknown, fallback: string): Error {
        if (err instanceof HttpErrorResponse) {
            const body = err.error;
            if (typeof body === 'string' && body.trim()) {
                return new Error(body);
            }
            if (body && typeof body === 'object' && 'message' in body) {
                const m = (body as {
                    message?: unknown;
                }).message;
                if (typeof m === 'string' && m.trim()) {
                    return new Error(m);
                }
            }
            if (err.status === 403) {
                return new Error('Accesso negato: account non autorizzato o bloccato.');
            }
        }
        return new Error(fallback);
    }
    public searchDesks(): Observable<Desk[]>;
    public searchDesks(criteria: SearchCriteria): Observable<Desk[]>;
    public searchDesks(criteria?: SearchCriteria): Observable<Desk[]> {
        const body: Record<string, unknown> = criteria
            ? {
                targetDate: criteria.targetDate,
                requiredAmenities: criteria.requiredAmenities,
                includeMaintenance: criteria.includeMaintenance
            }
            : {
                targetDate: new Date().toISOString().slice(0, 10),
                requiredAmenities: [],
                includeMaintenance: false
            };
        if (criteria?.slotStart && criteria.slotEnd) {
            body['startTime'] = `${criteria.targetDate}T${criteria.slotStart}:00`;
            body['endTime'] = `${criteria.targetDate}T${criteria.slotEnd}:00`;
        }
        if (criteria?.spaceId) {
            body['spaceId'] = criteria.spaceId;
        }
        return this.http.post<DeskDto[]>(`${this.baseUrl}/bookings/search`, body).pipe(map((rawDesks) => rawDesks.map((rawDesk) => ModelFactory.createDesk(rawDesk))), catchError((error) => {
            console.error('Ricerca postazioni non riuscita', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile caricare le postazioni.'));
        }));
    }
    public subscribeWaitlist(deskId: number, dateIso: string, desiredStart?: string, desiredEnd?: string): Observable<void> {
        let params = new HttpParams().set('date', dateIso);
        if (desiredStart) {
            params = params.set('desiredStart', desiredStart);
        }
        if (desiredEnd) {
            params = params.set('desiredEnd', desiredEnd);
        }
        return this.http.post<void>(`${this.baseUrl}/desks/${deskId}/waitlist`, null, { params }).pipe(tap(() => this.notifications.requestRefresh()), catchError((error) => throwError(() => BookingService.mapHttpError(error, 'Impossibile iscriversi alla lista d’attesa.'))));
    }
    public getSlotAvailability(deskId: number, date: string): Observable<SlotStatus[]> {
        return this.http.get<unknown[]>(`${this.baseUrl}/desks/${deskId}/slots`, { params: { date } }).pipe(map((rows) => (Array.isArray(rows) ? rows : []).map((row) => BookingService.normalizeSlotRow(row))), catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare la disponibilità.'))));
    }
    private static normalizeSlotRow(row: unknown): SlotStatus {
        const r = row as Record<string, unknown>;
        const timeRaw = String(r?.['time'] ?? r?.['Time'] ?? '');
        const st = String(r?.['status'] ?? r?.['Status'] ?? 'free').trim().toLowerCase();
        const status: 'free' | 'busy' = st === 'busy' || st === 'occupied' ? 'busy' : 'free';
        try {
            return { time: normalizeSlotTimeHm(timeRaw), status };
        }
        catch {
            return { time: '', status };
        }
    }
    public listDesksInSpace(spaceId: number): Observable<Desk[]> {
        const params = new HttpParams().set('spaceId', String(spaceId));
        return this.http.get<CatalogDeskDto[]>(`${environment.apiUrl}/desks`, { params }).pipe(map((rows) => rows.map((raw) => ModelFactory.createDesk({
            ...raw,
            building: raw.building ?? '',
            amenities: raw.amenities ?? []
        } as never))), catchError((error) => throwError(() => BookingService.mapHttpError(error, 'Impossibile caricare le postazioni della sede.'))));
    }
    public getWorkerSpaces(): Observable<WorkerSpace[]> {
        return this.http.get<WorkerSpace[]>(`${environment.apiUrl}/spaces`).pipe(catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare gli spazi disponibili.'))));
    }
    public getSpaceClosure(spaceId: number, dateIso: string): Observable<WorkerSpaceClosure | null> {
        return this.http
            .get<WorkerSpaceClosure | null>(`${this.baseUrl}/spaces/${spaceId}/closures`, { params: { date: dateIso } })
            .pipe(map((closure) => closure ?? null), catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile verificare le chiusure della sede.'))));
    }
    public bookDesk(req: BookingRequest): Observable<Booking> {
        return this.http
            .post<BookingDto>(`${this.baseUrl}/bookings`, {
            deskID: req.deskID,
            startTime: req.startTime,
            end: req.end
        })
            .pipe(map((rawBooking) => ModelFactory.createBooking(rawBooking)), tap(() => this.notifications.requestRefresh()), catchError((error) => {
            console.error('Prenotazione non riuscita', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile completare la prenotazione.'));
        }));
    }
    public cancelBooking(bookingId: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/bookings/${bookingId}`).pipe(tap(() => this.notifications.requestRefresh()), catchError((error) => {
            console.error('Annullamento prenotazione non riuscito', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile annullare la prenotazione.'));
        }));
    }
    public leaveDesk(bookingId: number): Observable<Booking> {
        return this.http.post<BookingDto>(`${this.baseUrl}/bookings/${bookingId}/leave`, {}).pipe(map((raw) => ModelFactory.createBooking(raw)), tap(() => this.notifications.requestRefresh()), catchError((error) => {
            console.error('Uscita dalla postazione non riuscita', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile lasciare la postazione.'));
        }));
    }
    public getMyBookings(): Observable<Booking[]> {
        return this.http.get<BookingDto[]>(`${this.baseUrl}/bookings`).pipe(map((rows) => rows.map((row) => ModelFactory.createBooking(row))), catchError((error) => {
            console.error('Caricamento prenotazioni non riuscito', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile caricare le prenotazioni.'));
        }));
    }
    public clearPastBookingHistory(): Observable<number> {
        return this.http.delete<{
            deleted: number;
        }>(`${this.baseUrl}/bookings/history`).pipe(map((body) => body.deleted ?? 0), catchError((error) => {
            console.error('Svuotamento storico prenotazioni non riuscito', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile svuotare lo storico prenotazioni.'));
        }));
    }
    public getMyReviewEligibleBookings(): Observable<Booking[]> {
        return this.http.get<BookingDto[]>(`${this.baseUrl}/bookings/review-eligible`).pipe(map((rows) => rows.map((row) => ModelFactory.createBooking(row))), catchError((error) => {
            console.error('Caricamento prenotazioni recensibili non riuscito', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile caricare le prenotazioni recensibili.'));
        }));
    }
    public reschedule(bookingId: number, version: number, newStart: string, newEnd: string): Observable<Booking> {
        return this.http
            .patch<BookingDto>(`${this.baseUrl}/bookings/${bookingId}`, { bookingId, version, newStart, newEnd })
            .pipe(map((row) => ModelFactory.createBooking(row)), tap(() => this.notifications.requestRefresh()), catchError((error) => {
            console.error('Riprogrammazione prenotazione non riuscita', error);
            return throwError(() => BookingService.mapHttpError(error, 'Impossibile riprogrammare la prenotazione.'));
        }));
    }
    public getWaitlistStatus(deskId: number, date: string): Observable<WaitlistStatusDto> {
        return this.http
            .get<WaitlistStatusDto>(`${this.baseUrl}/desks/${deskId}/waitlist`, { params: { date } })
            .pipe(catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile verificare lo stato della lista d’attesa.'))));
    }
    public getAvailableDesks(date: string): Observable<Desk[]> {
        return this.http
            .get<DeskDto[]>(`${environment.apiUrl}/desks/available`, { params: { date } })
            .pipe(map((rows) => rows.map((row) => ModelFactory.createDesk(row))), catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare le postazioni disponibili.'))));
    }
    public getDesk(id: number): Observable<Desk> {
        return this.http
            .get<DeskDto>(`${environment.apiUrl}/desks/${id}`)
            .pipe(map((row) => ModelFactory.createDesk(row)), catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare il dettaglio della postazione.'))));
    }
    public getSpace(id: number): Observable<WorkerSpace> {
        return this.http
            .get<WorkerSpace>(`${environment.apiUrl}/spaces/${id}`)
            .pipe(catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare il dettaglio dell’ufficio.'))));
    }
    public getBookingById(bookingId: number): Observable<Booking> {
        return this.http
            .get<BookingDto>(`${environment.apiUrl}/bookings/${bookingId}`)
            .pipe(map((row) => ModelFactory.createBooking(row)), catchError((err) => throwError(() => BookingService.mapHttpError(err, 'Impossibile caricare il dettaglio della prenotazione.'))));
    }
}
