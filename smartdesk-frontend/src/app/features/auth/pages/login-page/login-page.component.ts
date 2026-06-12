import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbValidationModule } from 'mdb-angular-ui-kit/validation';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
@Component({
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MdbFormsModule,
        MdbValidationModule,
        MdbRippleModule,
        SdIconComponent
    ],
    templateUrl: './login-page.component.html',
    styleUrl: './login-page.component.scss'
})
export class LoginPageComponent implements OnInit {
    private readonly auth = inject(AuthService);
    private readonly toast = inject(ToastService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly fb = inject(FormBuilder);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly loading = signal(false);
    protected readonly errorMessage = signal<string | null>(null);
    protected readonly passwordVisible = signal(false);
    protected readonly sessionExpiredBanner = signal(false);
    public ngOnInit(): void {
        if (this.route.snapshot.queryParamMap.get('session') === 'expired') {
            this.sessionExpiredBanner.set(true);
        }
    }
    protected togglePasswordVisible(): void {
        this.passwordVisible.update((v) => !v);
    }
    protected fieldShowError(controlName: 'email' | 'password'): boolean {
        const c = this.form.get(controlName);
        return !!c && c.invalid && c.touched;
    }
    protected emailAriaDescribedBy(): string {
        return this.fieldShowError('email') ? 'email-login-error' : 'email-login-hint';
    }
    protected passwordAriaDescribedBy(): string | null {
        return this.fieldShowError('password') ? 'password-login-error' : null;
    }
    protected readonly form = this.fb.nonNullable.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required]]
    });
    protected submit(): void {
        if (this.loading()) {
            return;
        }
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        this.loading.set(true);
        this.errorMessage.set(null);
        const { email, password } = this.form.getRawValue();
        this.auth
            .login(email, password)
            .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (user) => {
                const role = user.getRole();
                const destinationMap: Record<string, string> = {
                    SYS_ADMIN: '/admin/dashboard',
                    HOST: '/host/dashboard',
                    WORKER: '/worker/dashboard',
                    TECHNICIAN: '/technician/dashboard'
                };
                const destination = destinationMap[role] ?? '/worker/dashboard';
                void this.router.navigate([destination]).then((ok) => {
                    if (ok) {
                        this.toast.success('Accesso effettuato.');
                    }
                });
            },
            error: (err) => {
                this.errorMessage.set(err.message);
                this.toast.error(err.message || 'Accesso non riuscito.');
            }
        });
    }
}
