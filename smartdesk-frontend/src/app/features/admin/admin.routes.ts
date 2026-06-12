import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
const adminChild = (path: string, load: () => Promise<{
    new (...args: unknown[]): object;
}>, title: string, section: string) => ({
    path,
    loadComponent: load,
    data: { role: 'SYS_ADMIN', title, section }
});
export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        canActivate: [authGuard, roleGuard],
        loadComponent: () => import('../../shared/components/role-shell/role-shell.component').then((m) => m.RoleShellComponent),
        data: { role: 'SYS_ADMIN', shellLayout: true },
        children: [
            adminChild('dashboard', () => import('./pages/admin-dashboard-page/admin-dashboard-page.component').then((m) => m.AdminDashboardPageComponent), 'Panoramica amministratore', 'admin-dashboard'),
            adminChild('users', () => import('./pages/admin-users-page/admin-users-page.component').then((m) => m.AdminUsersPageComponent), 'Utenti', 'admin-users'),
            adminChild('requests', () => import('./pages/admin-requests-page/admin-requests-page.component').then((m) => m.AdminRequestsPageComponent), 'Richieste', 'admin-requests'),
            { path: 'hosts', pathMatch: 'full', redirectTo: 'requests' },
            adminChild('spaces', () => import('./pages/admin-spaces-page/admin-spaces-page.component').then((m) => m.AdminSpacesPageComponent), 'Spazi', 'admin-spaces'),
            adminChild('reviews', () => import('./pages/admin-reviews-page/admin-reviews-page.component').then((m) => m.AdminReviewsPageComponent), 'Recensioni uffici', 'admin-reviews'),
            adminChild('bookings', () => import('./pages/admin-bookings-page/admin-bookings-page.component').then((m) => m.AdminBookingsPageComponent), 'Prenotazioni', 'admin-bookings'),
            adminChild('logs', () => import('./pages/admin-logs-page/admin-logs-page.component').then((m) => m.AdminLogsPageComponent), 'Log di sistema', 'admin-logs'),
            adminChild('notifications', () => import('../worker/pages/worker-notifications-page/worker-notifications-page.component').then((m) => m.WorkerNotificationsPageComponent), 'Notifiche', 'admin-notifications'),
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
        ]
    }
];
