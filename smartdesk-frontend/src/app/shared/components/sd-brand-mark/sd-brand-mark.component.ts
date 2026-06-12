import { ChangeDetectionStrategy, Component, input } from '@angular/core';
@Component({
    selector: 'app-sd-brand-mark',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrl: './sd-brand-mark.component.scss',
    template: `    <svg      class="sd-brand-mark-svg"      role="img"      aria-hidden="true"      xmlns="http://www.w3.org/2000/svg"      [attr.width]="size()"      [attr.height]="size()"      viewBox="0 0 32 32"    >      <path fill="#ffffff" fill-opacity="0.92" d="M6 7.5h20a1 1 0 0 1 1 1v2.5a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V8.5a1 1 0 0 1 1-1Z" />      <path        fill="#ffffff"        d="M5 13h22c.83 0 1.5.67 1.5 1.5V22c0 1.38-1.12 2.5-2.5 2.5H6A2.5 2.5 0 0 1 3.5 22v-7.5C3.5 13.67 4.17 13 5 13Z"      />      <path fill="#ffffff" fill-opacity="0.5" d="M9 25.2V28h2.2v-2.8zm11.8 0V28H23v-2.8z" />    </svg>  `,
    styles: [
        `      :host {        display: inline-flex;        line-height: 0;        vertical-align: middle;      }      .sd-brand-mark-svg {        display: block;      }    `
    ]
})
export class SdBrandMarkComponent {
    readonly size = input(22);
}
