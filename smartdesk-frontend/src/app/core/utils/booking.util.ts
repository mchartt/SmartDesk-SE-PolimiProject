import { Booking } from '../models';
export function bookingPublicRef(b: Pick<Booking, 'bookingCode' | 'bookingID'>): string {
    const c = b.bookingCode?.trim() ?? '';
    return c.length > 0 ? c : String(b.bookingID);
}
