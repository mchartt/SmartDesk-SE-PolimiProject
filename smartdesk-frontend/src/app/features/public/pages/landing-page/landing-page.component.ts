import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdBrandMarkComponent } from '../../../../shared/components/sd-brand-mark/sd-brand-mark.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
@Component({
    standalone: true,
    selector: 'app-landing-page',
    imports: [CommonModule, RouterLink, MdbRippleModule, SdIconComponent, SdBrandMarkComponent],
    templateUrl: './landing-page.component.html',
    styleUrl: './landing-page.component.scss'
})
export class LandingPageComponent {
    protected readonly currentYear = new Date().getFullYear();
    transformStyle = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
    isHovered = false;
    onMouseMove(event: MouseEvent) {
        if (!this.isHovered)
            return;
        const container = event.currentTarget as HTMLElement;
        const rect = container.getBoundingClientRect();
        const x = event.clientX - rect.left - rect.width / 2;
        const y = event.clientY - rect.top - rect.height / 2;
        const rotateX = -(y / rect.height) * 15;
        const rotateY = (x / rect.width) * 15;
        this.transformStyle = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
    }
    onMouseEnter() {
        this.isHovered = true;
    }
    onMouseLeave() {
        this.isHovered = false;
        this.transformStyle = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
    }
}
