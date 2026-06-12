import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    input,
    output,
    TemplateRef,
    ViewChild
} from '@angular/core';
import { MdbCollapseDirective, MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk } from '../../../../../../core/models';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { formatAmenityTag } from '../../../desk-amenity-labels.util';
import { type DeskViewModel } from '../../../desk-search-session.store';
import {
    computeAvailableAmenityTags,
    pruneSelectedAmenityTags as pruneSelectedAmenityTagsUtil
} from './amenity-filter.util';

@Component({
    selector: 'app-amenity-filter',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MdbCollapseModule, MdbModalModule, MdbRippleModule, SdModalHeaderComponent],
    templateUrl: './amenity-filter.component.html',
    styleUrl: './amenity-filter.component.scss'
})
export class AmenityFilterComponent {
    readonly officeDesks = input<Desk[]>([]);
    readonly step2Done = input(false);
    readonly hasCompleteSlotRange = input(false);
    readonly includeMaintenance = input(false);
    readonly selectedTags = input<string[]>([]);
    readonly isLoading = input(false);

    readonly tagToggle = output<string>();
    readonly clearFilters = output<void>();

    @ViewChild('filtersCollapse')
    private filtersCollapse?: MdbCollapseDirective;
    @ViewChild('amenitiesModal')
    private amenitiesModal!: TemplateRef<unknown>;

    private amenitiesModalRef: MdbModalRef<unknown> | null = null;
    protected amenitiesModalDesk: DeskViewModel | null = null;

    private readonly modalService = inject(MdbModalService);
    private readonly cdr = inject(ChangeDetectorRef);

    toggleCollapse(): void {
        this.filtersCollapse?.toggle();
    }

    pruneSelectedTags(current: string[]): string[] {
        return pruneSelectedAmenityTagsUtil(current, this.availableTags);
    }

    openAmenitiesModal(desk: DeskViewModel): void {
        this.amenitiesModalDesk = desk;
        this.amenitiesModalRef = this.modalService.open(this.amenitiesModal, {
            modalClass: 'modal-dialog-centered modal-lg'
        });
        this.cdr.markForCheck();
    }

    protected get availableTags(): string[] {
        return computeAvailableAmenityTags(
            this.step2Done(),
            this.hasCompleteSlotRange(),
            this.officeDesks(),
            this.includeMaintenance()
        );
    }

    protected isTagSelected(tag: string): boolean {
        return this.selectedTags().includes(tag);
    }

    protected toggleAmenityTag(tag: string): void {
        if (this.isLoading()) {
            return;
        }
        this.tagToggle.emit(tag);
    }

    protected onClearFilters(): void {
        this.clearFilters.emit();
    }

    protected closeAmenitiesModal(): void {
        this.amenitiesModalRef?.close();
        this.amenitiesModalRef = null;
        queueMicrotask(() => {
            this.amenitiesModalDesk = null;
            this.cdr.markForCheck();
        });
    }

    protected deskResultsHeading(desk: Desk): string {
        const c = desk.code?.trim();
        return c ? `Postazione ${c}` : `Postazione #${desk.id}`;
    }

    protected formatTag(tag: string): string {
        return formatAmenityTag(tag);
    }
}
