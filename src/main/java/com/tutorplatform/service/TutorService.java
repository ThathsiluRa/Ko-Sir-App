package com.tutorplatform.service;

import com.tutorplatform.model.TutorProfile;
import com.tutorplatform.model.User;
import com.tutorplatform.repository.TutorProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TUTOR SERVICE
 * =====================================================
 * Business logic for tutor profile management.
 *
 * Handles:
 * - Finding tutor profiles
 * - Admin: approve/reject tutors
 * - Tutor: update their own profile
 * - Student: search for available tutors
 * =====================================================
 */
@Service
@Transactional
public class TutorService {

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    // =====================================================
    // TUTOR SEARCH (used by students)
    // =====================================================

    /**
     * Get all approved tutors who are accepting bookings.
     * This is the default list students see on the search page.
     */
    @Transactional(readOnly = true)
    public List<TutorProfile> getAvailableTutors() {
        return tutorProfileRepository.findByApprovedTrueAndAcceptingBookingsTrue();
    }

    /**
     * Search tutors by subject or location keyword.
     * Students type something like "math" or "Nairobi" and get relevant tutors.
     * Returns all available tutors if keyword is empty.
     */
    @Transactional(readOnly = true)
    public List<TutorProfile> searchTutors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // No keyword? Return all available tutors
            return getAvailableTutors();
        }
        return tutorProfileRepository.searchBySubjectOrLocation(keyword.trim());
    }

    // =====================================================
    // TUTOR PROFILE MANAGEMENT
    // =====================================================

    /**
     * Get a tutor's profile by their User account.
     * Used when a logged-in tutor loads their dashboard.
     */
    @Transactional(readOnly = true)
    public TutorProfile getTutorProfileByUser(User user) {
        return tutorProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No tutor profile found for user: " + user.getEmail()
                ));
    }

    /**
     * Get a tutor's profile by their profile ID.
     * Used when a student views a tutor's profile page.
     */
    @Transactional(readOnly = true)
    public TutorProfile getTutorProfileById(Long id) {
        return tutorProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor profile not found with id: " + id
                ));
    }

    /**
     * Save/update a tutor profile.
     * Used when a tutor edits their profile details.
     */
    public TutorProfile saveTutorProfile(TutorProfile profile) {
        return tutorProfileRepository.save(profile);
    }

    /**
     * Toggle whether the tutor is currently accepting new bookings.
     * Tutor can go "offline" if they're busy.
     */
    public TutorProfile toggleAcceptingBookings(Long tutorProfileId) {
        TutorProfile profile = getTutorProfileById(tutorProfileId);
        profile.setAcceptingBookings(!profile.isAcceptingBookings());
        return tutorProfileRepository.save(profile);
    }

    // =====================================================
    // ADMIN OPERATIONS
    // =====================================================

    /**
     * Admin approves a new tutor.
     * After approval, the tutor appears in student search results.
     */
    public TutorProfile approveTutor(Long tutorProfileId) {
        TutorProfile profile = getTutorProfileById(tutorProfileId);
        profile.setApproved(true);
        return tutorProfileRepository.save(profile);
    }

    /**
     * Admin revokes a tutor's approval.
     * The tutor is hidden from search results but their data is kept.
     */
    public TutorProfile revokeTutorApproval(Long tutorProfileId) {
        TutorProfile profile = getTutorProfileById(tutorProfileId);
        profile.setApproved(false);
        return tutorProfileRepository.save(profile);
    }

    /**
     * Get all tutor profiles (admin use - includes unapproved ones).
     */
    @Transactional(readOnly = true)
    public List<TutorProfile> getAllTutorProfiles() {
        return tutorProfileRepository.findAll();
    }

    /**
     * Get tutors pending admin approval.
     */
    @Transactional(readOnly = true)
    public List<TutorProfile> getPendingApprovalTutors() {
        return tutorProfileRepository.findByApprovedFalse();
    }

    /**
     * Count approved tutors for the admin dashboard stats.
     */
    @Transactional(readOnly = true)
    public long countApprovedTutors() {
        return tutorProfileRepository.countByApprovedTrue();
    }

    /**
     * Delete a tutor profile (admin only).
     */
    public void deleteTutorProfile(Long id) {
        tutorProfileRepository.deleteById(id);
    }
}
