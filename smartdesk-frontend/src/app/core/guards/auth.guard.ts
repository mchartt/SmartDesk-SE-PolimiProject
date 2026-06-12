import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStateService } from '../services/auth-state.service';
export const authGuard: CanActivateFn = () => {
    const authState = inject(AuthStateService);
    const router = inject(Router);
    const token = authState.token();
    const user = authState.currentUserSnapshot();
    if (token && user?.id != null) {
        return true;
    }
    if (token && !user) {
        authState.clearSession();
    }
    return router.createUrlTree(['/login']);
};
