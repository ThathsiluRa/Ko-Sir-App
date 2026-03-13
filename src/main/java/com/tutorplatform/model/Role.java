package com.tutorplatform.model;

/**
 * ROLE ENUM
 * =====================================================
 * Defines the three types of users in the system.
 * Spring Security uses these to control access to pages.
 *
 * - STUDENT: Can search tutors, book sessions, leave reviews
 * - TUTOR:   Can view/accept/reject booking requests
 * - ADMIN:   Full control - manage all users, bookings, reviews
 *
 * HOW IT WORKS:
 * Each User entity has a "role" field of this type.
 * SecurityConfig maps roles to URL patterns:
 *   ADMIN  -> /admin/**
 *   TUTOR  -> /tutor/**
 *   STUDENT -> /student/**
 * =====================================================
 */
public enum Role {

    /** Regular student who books tutoring sessions */
    STUDENT,

    /** Tutor who offers and accepts sessions */
    TUTOR,

    /** Platform administrator - has access to everything */
    ADMIN
}
