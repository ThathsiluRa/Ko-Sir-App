package com.tutorplatform.model;

/**
 * BOOKING STATUS ENUM
 * =====================================================
 * Tracks the lifecycle of a booking from creation to completion.
 *
 * LIFECYCLE FLOW:
 *
 *   Student requests → PENDING
 *          |
 *          ├── Tutor accepts  → CONFIRMED
 *          |       |
 *          |       ├── Session happens → COMPLETED
 *          |       |       |
 *          |       |       └── Student can leave a REVIEW
 *          |       |
 *          |       └── Tutor or Admin cancels → CANCELLED
 *          |
 *          └── Tutor rejects  → REJECTED
 *          └── Admin cancels  → CANCELLED
 *
 * The Admin can change any booking to any status at any time.
 * =====================================================
 */
public enum BookingStatus {

    /**
     * Booking created but payment not yet completed.
     * The booking is held/reserved until the student pays.
     * After successful payment, status moves to PENDING.
     */
    PENDING_PAYMENT,

    /** Payment received. Booking is waiting for tutor to accept/reject. */
    PENDING,

    /** Tutor accepted the booking request */
    CONFIRMED,

    /** Session has been completed - student can now leave a review */
    COMPLETED,

    /** Booking was cancelled (by student, tutor, or admin) */
    CANCELLED,

    /** Tutor declined the booking request */
    REJECTED
}
