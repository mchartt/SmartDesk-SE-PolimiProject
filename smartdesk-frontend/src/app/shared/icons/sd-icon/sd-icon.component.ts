import { Component, computed, input } from '@angular/core';
export type SdIconName = 'home' | 'bell' | 'menu' | 'moon' | 'user-circle' | 'users' | 'user-tie' | 'chart-pie' | 'layers' | 'server' | 'clipboard-list' | 'tasks' | 'wrench' | 'logout' | 'star' | 'star-outline' | 'ticket' | 'desktop' | 'calendar-check' | 'calendar' | 'life-ring' | 'rocket' | 'building' | 'location-pin' | 'wifi' | 'monitor' | 'chair' | 'outlet' | 'standing-desk' | 'sun' | 'tag' | 'filter' | 'search' | 'exclamation' | 'workspace' | 'host-venue' | 'door-open' | 'arrow-right' | 'envelope' | 'envelope-open' | 'check' | 'circle-check' | 'circle-exclamation' | 'circle-xmark' | 'circle-info' | 'inbox' | 'shield' | 'user' | 'user-plus' | 'rotate-left' | 'x' | 'chevron-up' | 'chevron-down' | 'chevron-left' | 'chevron-right' | 'minus' | 'plus' | 'reply' | 'send' | 'trash' | 'edit' | 'history' | 'clock' | 'eye' | 'eye-off' | 'rotate-right' | 'gauge' | 'clipboard-check' | 'user-gear' | 'calendar-day';
@Component({
    selector: 'app-sd-icon',
    standalone: true,
    styleUrl: './sd-icon.component.scss',
    template: `    <i [class]="faClass()" [style.fontSize.px]="size()" aria-hidden="true"></i>  `,
    styles: [
        `      :host {        display: inline-flex;        vertical-align: middle;        align-items: center;        justify-content: center;      }    `,
    ],
})
export class SdIconComponent {
    readonly name = input.required<SdIconName>();
    readonly size = input(24);
    private readonly iconMap: Record<SdIconName, string> = {
        'home': 'fa-solid fa-house',
        'bell': 'fa-solid fa-bell',
        'menu': 'fa-solid fa-bars',
        'moon': 'fa-solid fa-moon',
        'user-circle': 'fa-solid fa-circle-user',
        'users': 'fa-solid fa-users',
        'user-tie': 'fa-solid fa-user-tie',
        'chart-pie': 'fa-solid fa-chart-pie',
        'layers': 'fa-solid fa-layer-group',
        'server': 'fa-solid fa-server',
        'clipboard-list': 'fa-solid fa-clipboard-list',
        'tasks': 'fa-solid fa-list-check',
        'wrench': 'fa-solid fa-wrench',
        'logout': 'fa-solid fa-right-from-bracket',
        'star': 'fa-solid fa-star',
        'star-outline': 'fa-regular fa-star',
        'ticket': 'fa-solid fa-ticket',
        'desktop': 'fa-solid fa-desktop',
        'calendar-check': 'fa-solid fa-calendar-check',
        'calendar': 'fa-solid fa-calendar-days',
        'clock': 'fa-solid fa-clock',
        'eye': 'fa-solid fa-eye',
        'eye-off': 'fa-solid fa-eye-slash',
        'life-ring': 'fa-solid fa-life-ring',
        'rocket': 'fa-solid fa-rocket',
        'building': 'fa-solid fa-building',
        'location-pin': 'fa-solid fa-location-dot',
        'wifi': 'fa-solid fa-wifi',
        'monitor': 'fa-solid fa-display',
        'chair': 'fa-solid fa-chair',
        'outlet': 'fa-solid fa-plug',
        'standing-desk': 'fa-solid fa-desk-pc',
        'sun': 'fa-solid fa-sun',
        'tag': 'fa-solid fa-tag',
        'filter': 'fa-solid fa-filter',
        'search': 'fa-solid fa-magnifying-glass',
        'exclamation': 'fa-solid fa-circle-exclamation',
        'workspace': 'fa-solid fa-briefcase',
        'host-venue': 'fa-solid fa-hotel',
        'door-open': 'fa-solid fa-door-open',
        'arrow-right': 'fa-solid fa-arrow-right',
        'envelope': 'fa-solid fa-envelope',
        'envelope-open': 'fa-solid fa-envelope-open',
        'check': 'fa-solid fa-check',
        'circle-check': 'fa-solid fa-circle-check',
        'circle-exclamation': 'fa-solid fa-circle-exclamation',
        'circle-xmark': 'fa-solid fa-circle-xmark',
        'circle-info': 'fa-solid fa-circle-info',
        'inbox': 'fa-solid fa-inbox',
        'shield': 'fa-solid fa-shield-halved',
        'user': 'fa-solid fa-user',
        'user-plus': 'fa-solid fa-user-plus',
        'rotate-left': 'fa-solid fa-rotate-left',
        'x': 'fa-solid fa-xmark',
        'chevron-up': 'fa-solid fa-chevron-up',
        'chevron-down': 'fa-solid fa-chevron-down',
        'chevron-left': 'fa-solid fa-chevron-left',
        'chevron-right': 'fa-solid fa-chevron-right',
        'minus': 'fa-solid fa-minus',
        'plus': 'fa-solid fa-plus',
        'reply': 'fa-solid fa-reply',
        'send': 'fa-solid fa-paper-plane',
        'trash': 'fa-solid fa-trash-can',
        'edit': 'fa-solid fa-pen-to-square',
        'history': 'fa-solid fa-clock-rotate-left',
        'rotate-right': 'fa-solid fa-rotate-right',
        'gauge': 'fa-solid fa-gauge',
        'clipboard-check': 'fa-solid fa-clipboard-check',
        'user-gear': 'fa-solid fa-user-gear',
        'calendar-day': 'fa-solid fa-calendar-day'
    };
    protected readonly faClass = computed(() => {
        const icon = this.iconMap[this.name()] || 'fa-solid fa-question';
        return icon;
    });
}
