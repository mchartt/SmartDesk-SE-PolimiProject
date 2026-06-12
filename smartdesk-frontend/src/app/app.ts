import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { AuthStateService } from './core/services/auth-state.service';
import { ToastContainerComponent } from './shared/components/toast-container/toast-container.component';
@Component({
    selector: 'app-root',
    imports: [RouterOutlet, ToastContainerComponent],
    templateUrl: './app.html'
})
export class App implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly authState = inject(AuthStateService);
    private readonly destroyRef = inject(DestroyRef);
    public ngOnInit(): void {
        if (this.authState.token() && this.authState.currentUserSnapshot()) {
            this.authService
                .fetchCurrentProfile()
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe();
        }
    }
}
