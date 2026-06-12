import { OverlayRef } from '@angular/cdk/overlay';
import { Subscription } from 'rxjs';
export class OverlayBackdropBinder {
    private sub: Subscription | null = null;
    public attach(overlayRef: OverlayRef, onBackdrop: () => void): void {
        this.detach();
        this.sub = overlayRef.backdropClick().subscribe(() => onBackdrop());
    }
    public detach(): void {
        this.sub?.unsubscribe();
        this.sub = null;
    }
}
