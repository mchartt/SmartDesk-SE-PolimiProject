import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
const hostChild = (path: string, load: () => Promise<{
    new (...args: unknown[]): object;
}>, title: string, section: string) => ({
    path,
    loadComponent: load,
    data: { role: 'HOST', title, section }
});
export const HOST_ROUTES: Routes = [
    {
        path: '',
        canActivate: [authGuard, roleGuard],
        loadComponent: () => import('../../shared/components/role-shell/role-shell.component').then((m) => m.RoleShellComponent),
        data: { role: 'HOST', shellLayout: true },
        children: [
            hostChild('dashboard', () => import('./pages/host-dashboard-page/host-dashboard-page.component').then((m) => m.HostDashboardPageComponent), 'Panoramica host', 'host-dashboard'),
            hostChild('spaces', () => import('./pages/host-spaces-page/host-spaces-page.component').then((m) => m.HostSpacesPageComponent), 'Spazi', 'host-spaces'),
            hostChild('desks', () => import('./pages/host-desks-page/host-desks-page.component').then((m) => m.HostDesksPageComponent), 'Postazioni', 'host-desks'),
            hostChild('technicians', () => import('./pages/host-technicians-page/host-technicians-page.component').then((m) => m.HostTechniciansPageComponent), 'Tecnici', 'host-technicians'),
            hostChild('reviews', () => import('./pages/host-reviews-page/host-reviews-page.component').then((m) => m.HostReviewsPageComponent), 'Recensioni', 'host-reviews'),
            hostChild('tickets', () => import('./pages/host-tickets-page/host-tickets-page.component').then((m) => m.HostTicketsPageComponent), 'Segnalazioni', 'host-tickets'),
            hostChild('bookings', () => import('./pages/host-bookings-page/host-bookings-page.component').then((m) => m.HostBookingsPageComponent), 'Prenotazioni', 'host-bookings'),
            hostChild('notifications', () => import('../worker/pages/worker-notifications-page/worker-notifications-page.component').then((m) => m.WorkerNotificationsPageComponent), 'Notifiche', 'host-notifications'),
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
        ]
    }
];
