
/**
 * Life-cycle of an allocation request.
 *
 * PENDING     -> just read from file, not processed yet
 * ASSIGNED    -> student currently holds a bed
 * WAITLISTED  -> preferred room type full, student is queued
 * REJECTED    -> failed eligibility checks
 * VACATED     -> student was assigned earlier and has since checked out
 */
public enum RequestStatus {
    PENDING,
    ASSIGNED,
    WAITLISTED,
    REJECTED,
    VACATED
}
