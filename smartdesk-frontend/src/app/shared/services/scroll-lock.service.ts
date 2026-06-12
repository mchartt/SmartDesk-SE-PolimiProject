import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class ScrollLockService {
    private readonly document = inject(DOCUMENT);
    private depth = 0;
    acquire(): void {
        this.depth++;
        if (this.depth !== 1) {
            return;
        }
        const docEl = this.document.documentElement;
        const body = this.document.body;
        const gap = typeof window !== 'undefined' ? window.innerWidth - docEl.clientWidth : 0;
        if (gap > 0) {
            body.style.paddingRight = `${gap}px`;
        }
        docEl.classList.add('sd-scroll-locked');
        body.classList.add('sd-scroll-locked');
    }
    release(): void {
        if (this.depth <= 0) {
            return;
        }
        this.depth--;
        if (this.depth !== 0) {
            return;
        }
        const docEl = this.document.documentElement;
        const body = this.document.body;
        docEl.classList.remove('sd-scroll-locked');
        body.classList.remove('sd-scroll-locked');
        body.style.paddingRight = '';
    }
}
