import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStateService } from '../services/auth-state.service';
import { shouldAttachJwtToRequest } from './jwt-request.util';
const RETRY_HEADER = 'X-Smartdesk-Auth-Retry';
type AuthRefreshDto = {
    accessToken: string;
    refreshToken?: string;
};
let refreshInFlight: Promise<AuthRefreshDto> | null = null;
function sharedRefresh(refreshToken: string): Promise<AuthRefreshDto> {
    if (!refreshInFlight) {
        refreshInFlight = (async () => {
            const res = await fetch(`${environment.apiUrl}/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken }),
            });
            const body = (await res.json().catch(() => null)) as AuthRefreshDto | {
                message?: string;
            } | null;
            if (!res.ok) {
                const msg = body && typeof body === 'object' && 'message' in body && body.message != null
                    ? String(body.message)
                    : 'refresh failed';
                throw new Error(msg);
            }
            return body as AuthRefreshDto;
        })().finally(() => {
            refreshInFlight = null;
        });
    }
    return refreshInFlight;
}
export const authRefreshInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthStateService);
    const http = inject(HttpClient);
    return next(req).pipe(catchError((err: unknown) => {
        if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
            return throwError(() => err);
        }
        const url = req.url;
        if (!shouldAttachJwtToRequest(url, environment.apiUrl)) {
            return throwError(() => err);
        }
        if (url.includes('/auth/')) {
            return throwError(() => err);
        }
        if (req.headers.has(RETRY_HEADER)) {
            return throwError(() => err);
        }
        const rt = auth.refreshToken();
        const access = auth.token();
        if (!rt || !access) {
            return throwError(() => err);
        }
        return from(sharedRefresh(rt)).pipe(switchMap((dto) => {
            auth.updateTokens(dto.accessToken, dto.refreshToken);
            return http.request(req.clone({
                setHeaders: {
                    Authorization: `Bearer ${dto.accessToken}`,
                    [RETRY_HEADER]: '1',
                },
            }));
        }), catchError(() => throwError(() => err)));
    }));
};
