-- Update any existing SCHEDULED bookings to IN_PROCESS
UPDATE bookings SET status = 'IN_PROCESS' WHERE status = 'SCHEDULED'; 