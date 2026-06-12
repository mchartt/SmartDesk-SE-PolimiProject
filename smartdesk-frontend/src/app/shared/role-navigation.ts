import { UserRole } from '../core/models';
import { SdIconName } from './icons/sd-icon/sd-icon.component';
export const ROLE_DASHBOARD_PATH: Record<UserRole, string> = {
    WORKER: '/worker/dashboard',
    HOST: '/host/dashboard',
    TECHNICIAN: '/technician/dashboard',
    SYS_ADMIN: '/admin/dashboard'
};
export const ROLE_LABEL: Record<UserRole, string> = {
    WORKER: 'Lavoratore',
    HOST: 'Host',
    TECHNICIAN: 'Tecnico',
    SYS_ADMIN: 'Amministratore'
};
export type NavQuickLink = {
    label: string;
    path: string;
    icon: SdIconName;
};
export const ROLE_QUICK_LINKS: Record<UserRole, NavQuickLink[]> = {
    WORKER: [
        { label: 'Prenota', path: '/worker/desks', icon: 'desktop' },
        { label: 'Prenotazioni', path: '/worker/bookings', icon: 'calendar-check' }
    ],
    HOST: [
        { label: 'Prenotazioni', path: '/host/bookings', icon: 'calendar-check' },
        { label: 'Segnalazioni', path: '/host/tickets', icon: 'ticket' }
    ],
    TECHNICIAN: [
        { label: 'In attesa', path: '/technician/tickets', icon: 'clipboard-list' },
        { label: 'Assegnate', path: '/technician/assigned', icon: 'tasks' }
    ],
    SYS_ADMIN: [
        { label: 'Utenti', path: '/admin/users', icon: 'users' },
        { label: 'Richieste', path: '/admin/requests', icon: 'inbox' }
    ]
};
export function roleDashboardPath(role: UserRole | undefined): string {
    return role ? ROLE_DASHBOARD_PATH[role] : '/';
}
export function roleProfilePath(role: UserRole | undefined): string | null {
    if (role === 'WORKER') {
        return '/worker/profile';
    }
    return null;
}
export function roleNotificationsPath(role: UserRole | undefined): string {
    switch (role) {
        case 'HOST':
            return '/host/notifications';
        case 'SYS_ADMIN':
            return '/admin/notifications';
        case 'TECHNICIAN':
            return '/technician/notifications';
        default:
            return '/worker/notifications';
    }
}
export type AccountMenuItem = {
    label: string;
    path: string;
    icon: SdIconName;
    showUnreadBadge?: boolean;
};
export function roleAccountMenuItems(role: UserRole): AccountMenuItem[] {
    const items: AccountMenuItem[] = [
        { label: 'Panoramica', path: ROLE_DASHBOARD_PATH[role], icon: 'home' }
    ];
    const profile = roleProfilePath(role);
    if (profile) {
        items.push({ label: 'Profilo e sicurezza', path: profile, icon: 'user-circle' });
    }
    items.push({
        label: 'Notifiche',
        path: roleNotificationsPath(role),
        icon: 'bell',
        showUnreadBadge: true
    });
    return items;
}
