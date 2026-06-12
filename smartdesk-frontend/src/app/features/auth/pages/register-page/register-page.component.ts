import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbValidationModule } from 'mdb-angular-ui-kit/validation';
import { AuthService } from '../../../../core/services/auth.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { SdBrandMarkComponent } from '../../../../shared/components/sd-brand-mark/sd-brand-mark.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
type RegistrationRole = 'WORKER' | 'HOST';
type WizardStep = 1 | 2 | 3;
const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const PASSWORD_COMPLEXITY_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/;
const VAT_IT_PATTERN = /^\d{11}$/;
const HOST_DESCRIPTION_MIN = 50;
const HOST_DESCRIPTION_MAX = 2000;
function passwordMatchValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
        const pwd = group.get('password')?.value as string | undefined;
        const confirm = group.get('confirmPassword')?.value as string | undefined;
        if (confirm == null || confirm === '') {
            return null;
        }
        return pwd === confirm ? null : { passwordMismatch: true };
    };
}
@Component({
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink, MdbFormsModule, MdbValidationModule, MdbRippleModule, SdIconComponent, SdBrandMarkComponent],
    templateUrl: './register-page.component.html',
    styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {
    private readonly auth = inject(AuthService);
    private readonly router = inject(Router);
    private readonly fb = inject(FormBuilder);
    private readonly confirmModal = inject(ConfirmModalService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly loading = signal(false);
    protected readonly errorMessage = signal<string | null>(null);
    protected readonly selectedRole = signal<RegistrationRole>('WORKER');
    protected readonly passwordVisible = signal(false);
    protected readonly confirmPasswordVisible = signal(false);
    protected readonly submittedAttempt = signal(false);
    protected readonly step = signal<WizardStep>(1);
    protected readonly roleExplicitlyChosen = signal(false);
    protected togglePasswordVisible(): void {
        this.passwordVisible.update((v) => !v);
    }
    protected toggleConfirmPasswordVisible(): void {
        this.confirmPasswordVisible.update((v) => !v);
    }
    protected readonly form = this.fb.nonNullable.group({
        email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
        password: [
            '',
            [
                Validators.required,
                Validators.minLength(8),
                Validators.maxLength(128),
                Validators.pattern(PASSWORD_COMPLEXITY_PATTERN)
            ]
        ],
        confirmPassword: ['', [Validators.required]],
        name: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/^\p{L}[\p{L}\p{N}\s'.-]{0,79}$/u)]],
        surname: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/^\p{L}[\p{L}\p{N}\s'.-]{0,79}$/u)]],
        description: [''],
        vatNumber: [''],
        nameStructure: [''],
        role: ['WORKER' as RegistrationRole]
    }, { validators: [passwordMatchValidator()] });
    protected selectRole(role: RegistrationRole): void {
        this.selectedRole.set(role);
        this.form.controls.role.setValue(role);
        this.errorMessage.set(null);
        this.submittedAttempt.set(false);
        this.roleExplicitlyChosen.set(true);
        if (role === 'WORKER') {
            this.form.controls.description.setValue('');
        }
        this.updateHostValidators(role === 'HOST');
    }
    protected roleClass(): string {
        return `role-${this.selectedRole().toLowerCase()}`;
    }
    protected stepMeta(): string {
        const s = this.step();
        return `Passo ${s} / 3`;
    }
    protected passwordStrengthLabel(): 'weak' | 'fair' | 'good' | 'strong' {
        const p = this.form.controls.password.value ?? '';
        if (p.length < 8) {
            return 'weak';
        }
        let score = 0;
        if (PASSWORD_COMPLEXITY_PATTERN.test(p)) {
            score += 2;
        }
        if (/[^A-Za-z0-9]/.test(p)) {
            score++;
        }
        if (p.length >= 12) {
            score++;
        }
        if (p.length >= 16) {
            score++;
        }
        if (score <= 1) {
            return 'weak';
        }
        if (score === 2) {
            return 'fair';
        }
        if (score === 3) {
            return 'good';
        }
        return 'strong';
    }
    protected passwordStrengthSegments(): boolean[] {
        const label = this.passwordStrengthLabel();
        const n = label === 'weak' ? 1 : label === 'fair' ? 2 : label === 'good' ? 3 : 4;
        return [0, 1, 2, 3].map((i) => i < n);
    }
    protected usernamePreview(): string {
        const n = this.form.controls.name.value.trim().toLowerCase().replace(/\s+/g, '');
        const s = this.form.controls.surname.value.trim().toLowerCase().replace(/\s+/g, '');
        if (!n || !s) {
            return '—';
        }
        const base = `${n}.${s}`.replace(/[^a-z0-9.]/g, '');
        return base.length > 28 ? `${base.slice(0, 28)}…` : base;
    }
    protected passwordStrengthHint(): string {
        const labels: Record<'weak' | 'fair' | 'good' | 'strong', string> = {
            weak: 'Debole',
            fair: 'Discreta',
            good: 'Buona',
            strong: 'Ottima'
        };
        return labels[this.passwordStrengthLabel()];
    }
    protected wizardProgressClass(index: number): string {
        const s = this.step();
        if (index < s) {
            return 'sd-wizard-progress__node sd-wizard-progress__node--done';
        }
        if (index === s) {
            return 'sd-wizard-progress__node sd-wizard-progress__node--active';
        }
        return 'sd-wizard-progress__node';
    }
    protected wizardLineClass(afterIndex: number): string {
        return this.step() > afterIndex ? 'sd-wizard-progress__line sd-wizard-progress__line--active' : 'sd-wizard-progress__line';
    }
    protected continueFromStep1(): void {
        if (!this.roleExplicitlyChosen()) {
            return;
        }
        this.step.set(2);
        this.errorMessage.set(null);
    }
    protected backToStep1(): void {
        this.step.set(1);
    }
    protected continueFromStep2(): void {
        this.patchTrimmedFields();
        this.submittedAttempt.set(true);
        const controlsToValidate = [
            this.form.controls.email,
            this.form.controls.name,
            this.form.controls.surname,
            this.form.controls.vatNumber,
            this.form.controls.nameStructure,
            this.form.controls.description
        ];
        controlsToValidate.forEach(c => c.markAsTouched());
        if (controlsToValidate.some(c => c.invalid)) {
            return;
        }
        this.submittedAttempt.set(false);
        this.step.set(3);
        this.errorMessage.set(null);
    }
    protected backToStep2(): void {
        this.step.set(2);
    }
    protected fieldShowError(controlName: 'email' | 'password' | 'confirmPassword' | 'name' | 'surname' | 'vatNumber' | 'nameStructure' | 'description'): boolean {
        const c = this.form.get(controlName);
        return !!c && c.invalid && this.submittedAttempt();
    }
    protected nameErrorMessage(): string {
        const e = this.form.controls.name.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci il nome.';
        }
        return 'Formato non valido: inizia con una lettera; fino a 80 caratteri (lettere, numeri, spazio, apostrofo, punto o trattino).';
    }
    protected surnameErrorMessage(): string {
        const e = this.form.controls.surname.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci il cognome.';
        }
        return 'Formato non valido: inizia con una lettera; fino a 80 caratteri (lettere, numeri, spazio, apostrofo, punto o trattino).';
    }
    protected emailErrorMessage(): string {
        const e = this.form.controls.email.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci un indirizzo email.';
        }
        return 'Email non valida: usa il formato nome@dominio.est (es. mario.rossi@mail.com).';
    }
    protected vatErrorMessage(): string {
        const e = this.form.controls.vatNumber.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci la Partita IVA.';
        }
        return 'Partita IVA non valida: servono esattamente 11 cifre, senza spazi né lettere.';
    }
    protected nameStructureErrorMessage(): string {
        const e = this.form.controls.nameStructure.errors;
        if (!e) {
            return '';
        }
        return 'Inserisci il nome della struttura.';
    }
    protected passwordErrorMessage(): string {
        const e = this.form.controls.password.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci una password (min. 8 caratteri, con minuscola, maiuscola e numero).';
        }
        if (e['minlength']) {
            return 'Password troppo corta: minimo 8 caratteri, con minuscola, maiuscola e numero.';
        }
        if (e['maxlength']) {
            return 'Password troppo lunga: massimo 128 caratteri.';
        }
        return 'Password non valida: servono almeno una minuscola, una maiuscola e un numero.';
    }
    protected confirmPasswordErrorMessage(): string {
        if (this.form.controls.confirmPassword.errors?.['required']) {
            return 'Conferma la password.';
        }
        if (this.form.hasError('passwordMismatch') && this.form.controls.confirmPassword.touched) {
            return 'Le password non coincidono.';
        }
        return '';
    }
    protected descriptionErrorMessage(): string {
        const e = this.form.controls.description.errors;
        if (!e) {
            return '';
        }
        if (e['required']) {
            return 'Inserisci la descrizione della struttura (minimo 50 caratteri).';
        }
        if (e['minlength']) {
            return 'Descrizione troppo breve: servono almeno 50 caratteri (massimo 2000).';
        }
        if (e['maxlength']) {
            return 'Descrizione troppo lunga: massimo 2000 caratteri.';
        }
        return '';
    }
    protected submit(): void {
        this.patchTrimmedFields();
        this.submittedAttempt.set(true);
        this.form.markAllAsTouched();
        if (this.form.invalid || this.loading()) {
            this.errorMessage.set(null);
            return;
        }
        this.loading.set(true);
        this.errorMessage.set(null);
        const role = this.form.controls.role.getRawValue();
        const payload = this.form.getRawValue();
        const request$ = role === 'HOST'
            ? this.auth.registerHost({
                email: payload.email,
                password: payload.password,
                name: payload.name,
                surname: payload.surname,
                description: payload.description,
                vatNumber: payload.vatNumber,
                nameStructure: payload.nameStructure
            })
            : this.auth.registerUser({
                email: payload.email,
                password: payload.password,
                name: payload.name,
                surname: payload.surname,
                description: '',
                role
            });
        request$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.loading.set(false);
                if (role === 'HOST') {
                    this.confirmModal
                        .alert({
                        title: 'Richiesta inviata',
                        message: "L'amministratore è stato informato della richiesta; verrai ricontattato il prima possibile."
                    })
                        .pipe(takeUntilDestroyed(this.destroyRef))
                        .subscribe(() => {
                        void this.router.navigate(['/login']);
                    });
                    return;
                }
                this.confirmModal
                    .alert({
                    title: 'Registrazione completata',
                    message: 'Registrazione completata. Accedi con le tue credenziali.'
                })
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe(() => {
                    void this.router.navigate(['/login']);
                });
            },
            error: (err) => {
                this.errorMessage.set(err.message);
                this.loading.set(false);
            }
        });
    }
    private patchTrimmedFields(): void {
        const v = this.form.getRawValue();
        this.form.patchValue({
            email: v.email.trim(),
            name: v.name.trim(),
            surname: v.surname.trim(),
            vatNumber: v.vatNumber.replace(/\s+/g, ''),
            nameStructure: v.nameStructure.trim(),
            description: v.description.trim()
        }, { emitEvent: false });
    }
    private updateHostValidators(isHost: boolean): void {
        const vatNumberControl = this.form.controls.vatNumber;
        const nameStructureControl = this.form.controls.nameStructure;
        const descriptionControl = this.form.controls.description;
        if (isHost) {
            vatNumberControl.setValidators([Validators.required, Validators.pattern(VAT_IT_PATTERN)]);
            nameStructureControl.setValidators([Validators.required]);
            descriptionControl.setValidators([
                Validators.required,
                Validators.minLength(HOST_DESCRIPTION_MIN),
                Validators.maxLength(HOST_DESCRIPTION_MAX)
            ]);
        }
        else {
            vatNumberControl.clearValidators();
            nameStructureControl.clearValidators();
            descriptionControl.clearValidators();
            vatNumberControl.setValue('');
            nameStructureControl.setValue('');
            descriptionControl.setValue('');
        }
        vatNumberControl.updateValueAndValidity({ emitEvent: false });
        nameStructureControl.updateValueAndValidity({ emitEvent: false });
        descriptionControl.updateValueAndValidity({ emitEvent: false });
    }
}
