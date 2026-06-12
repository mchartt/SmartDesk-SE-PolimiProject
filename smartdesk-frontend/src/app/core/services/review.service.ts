import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Review } from '../models';
import { ModelFactory } from '../utils/model-factory';
@Injectable({ providedIn: 'root' })
export class ReviewService {
    private readonly http = inject(HttpClient);
    private readonly workersBase = `${environment.apiUrl}/workers`;
    private readonly adminBase = `${environment.apiUrl}/admin`;
    public leaveReview(payload: {
        bookingID: number;
        rating: number;
        comment: string;
    }): Observable<Review> {
        return this.http.post<Record<string, unknown>>(`${this.workersBase}/reviews`, payload).pipe(map((row) => ModelFactory.createReview(row as never)), catchError(() => throwError(() => new Error('Impossibile inviare la recensione.'))));
    }
    public updateReview(reviewId: number, payload: {
        rating: number;
        comment: string;
    }): Observable<Review> {
        return this.http.patch<Record<string, unknown>>(`${this.workersBase}/reviews/${reviewId}`, payload).pipe(map((row) => ModelFactory.createReview(row as never)), catchError(() => throwError(() => new Error('Impossibile aggiornare la recensione.'))));
    }
    public deleteReview(reviewId: number): Observable<void> {
        return this.http.delete<void>(`${this.workersBase}/reviews/${reviewId}`).pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la recensione.'))));
    }
    public getMyReviewHistory(): Observable<Review[]> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.workersBase}/reviews/history`).pipe(map((rows) => (Array.isArray(rows) ? rows : []).map((row) => ModelFactory.createReview(row as never))), catchError(() => throwError(() => new Error('Impossibile caricare lo storico recensioni.'))));
    }
    public getAdminSpaceReviews(spaceID: number): Observable<Review[]> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.adminBase}/spaces/${spaceID}/reviews`).pipe(map((rows) => rows.map((row) => ModelFactory.createReview(row as never))), catchError(() => throwError(() => new Error('Impossibile caricare le recensioni dello spazio.'))));
    }
    public deleteReviewAsAdmin(reviewID: number): Observable<void> {
        return this.http.delete<void>(`${this.adminBase}/reviews/${reviewID}`).pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la recensione.'))));
    }
}
