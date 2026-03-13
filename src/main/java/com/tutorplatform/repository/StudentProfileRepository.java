package com.tutorplatform.repository;

import com.tutorplatform.model.StudentProfile;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * STUDENT PROFILE REPOSITORY
 * =====================================================
 * Handles database operations for StudentProfile.
 *
 * Simpler than TutorProfileRepository because students
 * don't need complex search queries.
 * =====================================================
 */
@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    /**
     * Find a student's profile by their User account.
     * Used when a logged-in student accesses their dashboard.
     */
    Optional<StudentProfile> findByUser(User user);

    /**
     * Check if a user already has a student profile.
     * Prevents duplicate profiles.
     */
    boolean existsByUser(User user);
}
