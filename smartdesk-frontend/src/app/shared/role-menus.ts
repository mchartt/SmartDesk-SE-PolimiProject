import { UserRole } from '../core/models';
import { SdIconName } from './icons/sd-icon/sd-icon.component';
export type MenuItem = {
    label: string;
    path: string;
    icon: SdIconName;
};
export const ROLE_MENUS: Record<UserRole, MenuItem[]> = {
    WORKER: [
        { label: 'Panoramica', path: '/worker/dashboard', icon: 'home' },
        { label: 'Postazioni', path: '/worker/desks', icon: 'desktop' },
        { label: 'Prenotazioni', path: '/worker/bookings', icon: 'calendar-check' },
        { label: 'Segnalazioni', path: '/worker/tickets', icon: 'ticket' },
        { label: 'Notifiche', path: '/worker/notifications', icon: 'bell' },
        { label: 'Recensioni', path: '/worker/reviews', icon: 'star' },
        { label: 'Profilo', path: '/worker/profile', icon: 'user-circle' }
    ],
    HOST: [
        { label: 'Panoramica', path: '/host/dashboard', icon: 'chart-pie' },
        { label: 'Spazi', path: '/host/spaces', icon: 'building' },
        { label: 'Postazioni', path: '/host/desks', icon: 'chair' },
        { label: 'Tecnici', path: '/host/technicians', icon: 'wrench' },
        { label: 'Recensioni', path: '/host/reviews', icon: 'star' },
        { label: 'Segnalazioni', path: '/host/tickets', icon: 'ticket' },
        { label: 'Prenotazioni', path: '/host/bookings', icon: 'calendar-check' },
        { label: 'Notifiche', path: '/host/notifications', icon: 'bell' }
    ],
    TECHNICIAN: [
        { label: 'Panoramica', path: '/technician/dashboard', icon: 'home' },
        { label: 'In attesa', path: '/technician/tickets', icon: 'clipboard-list' },
        { label: 'Assegnate', path: '/technician/assigned', icon: 'tasks' },
        { label: 'Manutenzione', path: '/technician/maintenance', icon: 'wrench' },
        { label: 'Notifiche', path: '/technician/notifications', icon: 'bell' }
    ],
    SYS_ADMIN: [
        { label: 'Panoramica', path: '/admin/dashboard', icon: 'chart-pie' },
        { label: 'Utenti', path: '/admin/users', icon: 'users' },
        { label: 'Richieste', path: '/admin/requests', icon: 'inbox' },
        { label: 'Spazi', path: '/admin/spaces', icon: 'layers' },
        { label: 'Prenotazioni', path: '/admin/bookings', icon: 'calendar-check' },
        { label: 'Log', path: '/admin/logs', icon: 'server' },
        { label: 'Recensioni', path: '/admin/reviews', icon: 'star' },
        { label: 'Notifiche', path: '/admin/notifications', icon: 'bell' }
    ]
};
