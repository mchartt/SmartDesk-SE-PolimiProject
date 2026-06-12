import { UserRole } from '../models';
export function readAccessTokenRole(token: string | null): UserRole | null {
    if (!token)
        return null;
    try {
        const parts = token.split('.');
        if (parts.length !== 3)
            return null;
        const payload = JSON.parse(atob(parts[1]));
        const role = payload.role;
        if (typeof role === 'string') {
            const normalized = role.toUpperCase();
            if (['WORKER', 'HOST', 'TECHNICIAN', 'SYS_ADMIN'].includes(normalized)) {
                return normalized as UserRole;
            }
            if (normalized === 'ADMIN') {
                return 'SYS_ADMIN';
            }
        }
        return null;
    }
    catch {
        return null;
    }
}
