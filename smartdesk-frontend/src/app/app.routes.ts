import { Routes } from '@angular/router';
export const routes: Routes = [
    { path: '', loadComponent: () => import('./features/public/pages/landing-page/landing-page.component').then((m) => m.LandingPageComponent) },
    { path: 'login', loadComponent: () => import('./features/auth/pages/login-page/login-page.component').then((m) => m.LoginPageComponent) },
    { path: 'register', loadComponent: () => import('./features/auth/pages/register-page/register-page.component').then((m) => m.RegisterPageComponent) },
    {
        path: 'register/host',
        redirectTo: 'register',
        pathMatch: 'full'
    },
    {
        path: 'unauthorized',
        loadComponent: () => import('./features/auth/pages/unauthorized-page/unauthorized-page.component').then((m) => m.UnauthorizedPageComponent)
    },
    { path: 'worker', loadChildren: () => import('./features/worker/worker.routes').then((m) => m.WORKER_ROUTES) },
    { path: 'host', loadChildren: () => import('./features/host/host.routes').then((m) => m.HOST_ROUTES) },
    { path: 'admin', loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES) },
    { path: 'technician', loadChildren: () => import('./features/technician/technician.routes').then((m) => m.TECHNICIAN_ROUTES) },
    { path: '**', redirectTo: '' }
];
