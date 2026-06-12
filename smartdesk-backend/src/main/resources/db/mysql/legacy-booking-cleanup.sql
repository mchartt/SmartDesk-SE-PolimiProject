-- DeskStateCode.BOOKED removed; availability from bookings.
-- Ensure MySQL state is consistent with the current enum.
UPDATE desk SET state_code = 'AVAILABLE' WHERE state_code = 'BOOKED';
