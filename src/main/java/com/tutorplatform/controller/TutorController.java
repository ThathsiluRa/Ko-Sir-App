package com.tutorplatform.controller;

import com.tutorplatform.model.*;
import com.tutorplatform.service.BookingService;
import com.tutorplatform.service.ReviewService;
import com.tutorplatform.service.TutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * TUTOR CONTROLLER
 * =====================================================
 * Handles all pages and actions for TUTOR users.
 *
 * Key tutor actions:
 * - View their dashboard (pending bookings, stats)
 * - Accept or reject booking requests
 * - Mark sessions as completed
 * - View all their bookings
 * - Edit their profile
 * - Toggle availability (accepting bookings on/off)
 * =====================================================
 */
@Controller
@RequestMapping("/tutor")
public class TutorController {

    @Autowired private TutorService tutorService;
    @Autowired private BookingService bookingService;
    @Autowired private ReviewService reviewService;

    // =====================================================
    // TUTOR DASHBOARD
    // =====================================================

    /**
     * Tutor's main dashboard.
     * Shows pending bookings that need attention + recent activity.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {

        TutorProfile tutorProfile = tutorService.getTutorProfileByUser(currentUser);

        // Get all bookings for this tutor
        List<Booking> allBookings = bookingService.getBookingsByTutor(tutorProfile);

        // Get PENDING bookings - these need tutor response
        List<Booking> pendingBookings = bookingService.getPendingBookingsForTutor(tutorProfile);

        // Count stats for the dashboard cards
        long confirmedCount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completedCount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        // Get recent reviews
        List<Review> recentReviews = reviewService.getReviewsForTutor(tutorProfile)
                .stream().limit(3).toList();

        model.addAttribute("tutorProfile", tutorProfile);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalBookings", allBookings.size());
        model.addAttribute("recentReviews", recentReviews);
        model.addAttribute("currentUser", currentUser);

        return "tutor/dashboard"; // templates/tutor/dashboard.html
    }

    // =====================================================
    // BOOKING MANAGEMENT
    // =====================================================

    /**
     * View all bookings for this tutor with optional status filter.
     */
    @GetMapping("/bookings")
    public String myBookings(@AuthenticationPrincipal User currentUser,
                              @RequestParam(required = false) String status,
                              Model model) {

        TutorProfile tutorProfile = tutorService.getTutorProfileByUser(currentUser);
        List<Booking> bookings = bookingService.getBookingsByTutor(tutorProfile);

        // Apply status filter if provided
        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus filterStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() == filterStatus)
                        .toList();
                model.addAttribute("filterStatus", filterStatus);
            } catch (IllegalArgumentException e) {
                // Invalid status - show all
            }
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        return "tutor/bookings"; // templates/tutor/bookings.html
    }

    /**
     * Tutor confirms a booking request.
     * POST /tutor/bookings/{id}/confirm
     */
    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirmBooking(@PathVariable Long bookingId,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttrs) {
        try {
            bookingService.confirmBooking(bookingId, currentUser);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Booking confirmed! The student has been notified.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tutor/bookings";
    }

    /**
     * Tutor rejects a booking request.
     * POST /tutor/bookings/{id}/reject
     */
    @PostMapping("/bookings/{bookingId}/reject")
    public String rejectBooking(@PathVariable Long bookingId,
                                 @AuthenticationPrincipal User currentUser,
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes redirectAttrs) {
        try {
            bookingService.rejectBooking(bookingId, currentUser, reason);
            redirectAttrs.addFlashAttribute("successMessage", "Booking request declined.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tutor/bookings";
    }

    /**
     * Tutor marks a session as completed.
     * This enables the student to leave a review.
     */
    @PostMapping("/bookings/{bookingId}/complete")
    public String completeBooking(@PathVariable Long bookingId,
                                   RedirectAttributes redirectAttrs) {
        try {
            bookingService.completeBooking(bookingId);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Session marked as completed. The student can now leave a review.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tutor/bookings";
    }

    // =====================================================
    // TUTOR PROFILE MANAGEMENT
    // =====================================================

    /**
     * Show the tutor's edit profile page.
     */
    @GetMapping("/profile")
    public String editProfile(@AuthenticationPrincipal User currentUser, Model model) {
        TutorProfile tutorProfile = tutorService.getTutorProfileByUser(currentUser);
        model.addAttribute("tutorProfile", tutorProfile);
        return "tutor/profile"; // templates/tutor/profile.html
    }

    /**
     * Save profile changes.
     */
    @PostMapping("/profile")
    public String saveProfile(@AuthenticationPrincipal User currentUser,
                               @RequestParam String subjects,
                               @RequestParam String bio,
                               @RequestParam String qualification,
                               @RequestParam Integer experienceYears,
                               @RequestParam String hourlyRate,
                               @RequestParam String location,
                               @RequestParam String availability,
                               @RequestParam(defaultValue = "HOME_VISIT") String teachingMode,
                               @RequestParam(required = false) String teachingAddress,
                               RedirectAttributes redirectAttrs) {

        TutorProfile profile = tutorService.getTutorProfileByUser(currentUser);

        // Update all profile fields
        profile.setSubjects(subjects);
        profile.setBio(bio);
        profile.setQualification(qualification);
        profile.setExperienceYears(experienceYears);
        profile.setLocation(location);
        profile.setAvailability(availability);
        profile.setTeachingMode(teachingMode);
        // Only store teachingAddress when relevant — clear it for pure home-visit tutors
        profile.setTeachingAddress("HOME_VISIT".equals(teachingMode) ? null : teachingAddress);

        try {
            profile.setHourlyRate(new BigDecimal(hourlyRate));
        } catch (NumberFormatException e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Invalid hourly rate format.");
            return "redirect:/tutor/profile";
        }

        tutorService.saveTutorProfile(profile);
        redirectAttrs.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/tutor/profile";
    }

    /**
     * Toggle accepting bookings on/off.
     * Tutor can "go offline" to stop receiving new requests.
     */
    @PostMapping("/toggle-availability")
    public String toggleAvailability(@AuthenticationPrincipal User currentUser,
                                      RedirectAttributes redirectAttrs) {
        TutorProfile profile = tutorService.getTutorProfileByUser(currentUser);
        tutorService.toggleAcceptingBookings(profile.getId());

        String message = profile.isAcceptingBookings()
                ? "You are now offline. No new bookings will be accepted."
                : "You are now online. Students can book sessions with you.";

        redirectAttrs.addFlashAttribute("successMessage", message);
        return "redirect:/tutor/dashboard";
    }
}
