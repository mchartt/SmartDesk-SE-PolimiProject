import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
const techChild = (path: string, load: () => Promise<{
    new (...args: unknown[]): object;
}>, title: string, section: string) => ({
    path,
    loadComponent: load,
    data: { role: 'TECHNICIAN', title, section }
});
export const TECHNICIAN_ROUTES: Routes = [
    {
        path: '',
        canActivate: [authGuard, roleGuard],
        loadComponent: () => import('../../shared/components/role-shell/role-shell.component').then((m) => m.RoleShellComponent),
        data: { role: 'TECHNICIAN', shellLayout: true },
        children: [
            techChild('dashboard', () => import('./pages/technician-dashboard-page/technician-dashboard-page.component').then((m) => m.TechnicianDashboardPageComponent), 'Panoramica tecnico', 'technician-dashboard'),
            techChild('tickets', () => import('./pages/technician-pending-tickets-page/technician-pending-tickets-page.component').then((m) => m.TechnicianPendingTicketsPageComponent), 'Segnalazioni in attesa', 'technician-tickets'),
            techChild('assigned', () => import('./pages/technician-assigned-tickets-page/technician-assigned-tickets-page.component').then((m) => m.TechnicianAssignedTicketsPageComponent), 'Segnalazioni assegnate', 'technician-assigned'),
            techChild('maintenance', () => import('./pages/technician-maintenance-page/technician-maintenance-page.component').then((m) => m.TechnicianMaintenancePageComponent), 'Manutenzione', 'technician-maintenance'),
            techChild('notifications', () => import('../worker/pages/worker-notifications-page/worker-notifications-page.component').then((m) => m.WorkerNotificationsPageComponent), 'Notifiche', 'technician-notifications'),
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
        ]
    }
];
