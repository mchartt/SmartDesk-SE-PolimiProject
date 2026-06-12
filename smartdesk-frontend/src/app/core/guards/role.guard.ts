import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { UserRole } from '../models';
import { AuthStateService } from '../services/auth-state.service';
export const roleGuard: CanActivateFn = (route) => {
    const authState = inject(AuthStateService);
    const router = inject(Router);
    const expected = (route.data['role'] as UserRole | undefined) ?? 'WORKER';
    const user = authState.user();
    return user?.getRole() === expected ? true : router.createUrlTree(['/unauthorized']);
};
