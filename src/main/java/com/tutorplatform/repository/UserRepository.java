package com.tutorplatform.repository;

import com.tutorplatform.model.Role;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * USER REPOSITORY
 * =====================================================
 * Handles all database operations for the User entity.
 *
 * By extending JpaRepository<User, Long> we get FREE methods:
 *   - save(user)           → INSERT or UPDATE
 *   - findById(id)         → SELECT by primary key
 *   - findAll()            → SELECT all users
 *   - delete(user)         → DELETE a record
 *   - count()              → COUNT total rows
 *   - existsById(id)       → Check if row exists
 *
 * We add CUSTOM methods using Spring Data's naming convention.
 * Spring auto-generates the SQL from the method name:
 *   findByEmail → SELECT * FROM users WHERE email = ?
 * =====================================================
 */
@Repository  // Marks this as a Spring-managed data access component
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used for login (Spring Security) and duplicate-check on registration.
     *
     * Returns Optional<User> because the user might not exist.
     * Pattern: userRepo.findByEmail("john@email.com").orElseThrow(...)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role.
     * Admin uses this to list all students or all tutors separately.
     * Example: findByRole(Role.TUTOR) → all tutor accounts
     */
    List<User> findByRole(Role role);

    /**
     * Find active/inactive users.
     * Admin uses this to see who is currently active on the platform.
     */
    List<User> findByActive(boolean active);

    /**
     * Find users by role and active status.
     * Example: all active tutors, all inactive students.
     */
    List<User> findByRoleAndActive(Role role, boolean active);

    /**
     * Count users by role.
     * Used on the admin dashboard for statistics.
     * Example: countByRole(Role.STUDENT) → total number of students
     */
    long countByRole(Role role);
}
