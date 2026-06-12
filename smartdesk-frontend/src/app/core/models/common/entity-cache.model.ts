import { Identifiable } from './identifiable.model';
export class EntityCache<T extends Identifiable> {
    private readonly data = new Map<number, T>();
    public setAll(items: T[]): void {
        this.data.clear();
        items.forEach((item) => this.data.set(item.id, item));
    }
    public values(): T[] {
        return Array.from(this.data.values());
    }
}
