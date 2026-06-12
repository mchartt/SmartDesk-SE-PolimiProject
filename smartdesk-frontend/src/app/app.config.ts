import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withRouterConfig } from '@angular/router';
import { MdbModalModule } from 'mdb-angular-ui-kit/modal';
import { routes } from './app.routes';
import { authRefreshInterceptor } from './core/interceptors/auth-refresh.interceptor';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { sessionExpiryInterceptor } from './core/interceptors/session-expiry.interceptor';
export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideAnimations(),
        provideHttpClient(withInterceptors([jwtInterceptor, sessionExpiryInterceptor, authRefreshInterceptor])),
        provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),
        importProvidersFrom(MdbModalModule)
    ]
};
