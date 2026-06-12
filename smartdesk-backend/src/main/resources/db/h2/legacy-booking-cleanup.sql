-- DB H2 locali creati prima degli slot orari: il vecchio UNIQUE(deskID, booked_day)
-- blocca piu prenotazioni non sovrapposte nello stesso giorno.
ALTER TABLE IF EXISTS booking DROP CONSTRAINT IF EXISTS UK_BOOKING_DESK_DAY;
DROP INDEX IF EXISTS UK_BOOKING_DESK_DAY;

-- DeskStateCode.BOOKED removed; availability from bookings.
UPDATE desk SET state_code = 'AVAILABLE' WHERE state_code = 'BOOKED';
