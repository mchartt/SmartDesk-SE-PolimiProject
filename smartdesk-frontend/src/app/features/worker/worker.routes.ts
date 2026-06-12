import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
const workerChild = (path: string, load: () => Promise<{
    new (...args: unknown[]): object;
}>, title: string, section: string) => ({
    path,
    loadComponent: load,
    data: { role: 'WORKER', title, section }
});
export const WORKER_ROUTES: Routes = [
    {
        path: '',
        canActivate: [authGuard, roleGuard],
        loadComponent: () => import('../../shared/components/role-shell/role-shell.component').then((m) => m.RoleShellComponent),
        data: { role: 'WORKER', shellLayout: true },
        children: [
            workerChild('dashboard', () => import('./pages/worker-dashboard-page/worker-dashboard-page.component').then((m) => m.WorkerDashboardPageComponent), 'Panoramica lavoratore', 'worker-dashboard'),
            workerChild('desks', () => import('./pages/desk-search-page/desk-search-page.component').then((m) => m.DeskSearchPageComponent), 'Ricerca postazioni', 'worker-desks'),
            workerChild('bookings', () => import('./pages/worker-bookings-page/worker-bookings-page.component').then((m) => m.WorkerBookingsPageComponent), 'Le mie prenotazioni', 'worker-bookings'),
            workerChild('tickets', () => import('./pages/worker-tickets-page/worker-tickets-page.component').then((m) => m.WorkerTicketsPageComponent), 'Le mie segnalazioni', 'worker-tickets'),
            workerChild('notifications', () => import('./pages/worker-notifications-page/worker-notifications-page.component').then((m) => m.WorkerNotificationsPageComponent), 'Notifiche', 'worker-notifications'),
            workerChild('reviews', () => import('./pages/worker-reviews-page/worker-reviews-page.component').then((m) => m.WorkerReviewsPageComponent), 'Recensioni', 'worker-reviews'),
            workerChild('profile', () => import('./pages/worker-profile-page/worker-profile-page.component').then((m) => m.WorkerProfilePageComponent), 'Profilo', 'worker-profile'),
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
        ]
    }
];
