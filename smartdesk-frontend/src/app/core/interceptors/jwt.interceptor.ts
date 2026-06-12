import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthStateService } from '../services/auth-state.service';
import { isPublicAuthRequestUrl, shouldAttachJwtToRequest } from './jwt-request.util';
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
    const token = inject(AuthStateService).token();
    const isApiUrl = shouldAttachJwtToRequest(req.url, environment.apiUrl);
    const publicAuth = isPublicAuthRequestUrl(req.url);
    if (token && isApiUrl && !publicAuth) {
        return next(req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`,
            },
        }));
    }
    return next(req);
};
