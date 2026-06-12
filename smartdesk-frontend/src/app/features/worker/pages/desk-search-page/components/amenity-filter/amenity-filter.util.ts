import { Desk } from '../../../../../../core/models';
import { compareAmenityTags } from '../../../desk-amenity-labels.util';

export function officeDesksForAmenityFilter(officeDesks: Desk[], includeMaintenance: boolean): Desk[] {
    return officeDesks.filter((desk) => {
        if (!includeMaintenance && desk.state.code === 'MAINTENANCE') {
            return false;
        }
        return true;
    });
}

export function collectAmenityTags(desks: Desk[]): string[] {
    const tags = new Set<string>();
    for (const desk of desks) {
        for (const tag of desk.amenities ?? []) {
            const normalized = (tag ?? '').trim();
            if (normalized) {
                tags.add(normalized);
            }
        }
    }
    return [...tags].sort((a, b) => compareAmenityTags(a, b));
}

export function computeAvailableAmenityTags(
    step2Done: boolean,
    hasCompleteSlotRange: boolean,
    officeDesks: Desk[],
    includeMaintenance: boolean
): string[] {
    if (!step2Done || !hasCompleteSlotRange || !officeDesks.length) {
        return [];
    }
    return collectAmenityTags(officeDesksForAmenityFilter(officeDesks, includeMaintenance));
}

export function pruneSelectedAmenityTags(selectedTags: string[], availableTags: string[]): string[] {
    const allowed = new Set(availableTags);
    return selectedTags.filter((tag) => allowed.has(tag));
}
