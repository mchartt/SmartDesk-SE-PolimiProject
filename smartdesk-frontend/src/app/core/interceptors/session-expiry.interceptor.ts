import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStateService } from '../services/auth-state.service';
import { NotificationService } from '../services/notification.service';
import { isPublicAuthRequestUrl, shouldAttachJwtToRequest } from './jwt-request.util';
export const sessionExpiryInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthStateService);
    const notifications = inject(NotificationService);
    const router = inject(Router);
    return next(req).pipe(catchError((err: unknown) => {
        if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
            return throwError(() => err);
        }
        const url = req.url;
        if (!shouldAttachJwtToRequest(url, environment.apiUrl) || isPublicAuthRequestUrl(url)) {
            return throwError(() => err);
        }
        notifications.disconnectRealtimeStream();
        auth.clearSession();
        if (!router.url.startsWith('/login')) {
            void router.navigate(['/login'], {
                queryParams: { session: 'expired' },
                replaceUrl: true
            });
        }
        return throwError(() => err);
    }));
};
