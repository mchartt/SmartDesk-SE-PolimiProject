import { Desk } from './desk.model';
import { SearchCriteria } from '../booking/search-criteria.model';
export class DeskCatalog {
    private readonly desks: Desk[];
    public constructor(desks: Desk[]) {
        this.desks = [...desks];
    }
    public searchDesks(): Desk[];
    public searchDesks(criteria: SearchCriteria): Desk[];
    public searchDesks(criteria?: SearchCriteria): Desk[] {
        if (!criteria) {
            return this.desks.filter((desk) => desk.isBookable());
        }
        return this.desks.filter((desk) => desk.isBookable() && desk.hasRequiredAmenities(criteria.requiredAmenities));
    }
}
