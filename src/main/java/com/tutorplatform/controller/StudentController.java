package com.tutorplatform.controller;

import com.tutorplatform.model.*;
import com.tutorplatform.repository.StudentProfileRepository;
import com.tutorplatform.service.BookingService;
import com.tutorplatform.service.ReviewService;
import com.tutorplatform.service.TutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * STUDENT CONTROLLER
 * =====================================================
 * Handles all pages and actions for STUDENT users.
 *
 * Only accessible to users with ROLE_STUDENT.
 * (Enforced in SecurityConfig: /student/** requires STUDENT role)
 *
 * KEY PATTERN: @AuthenticationPrincipal User currentUser
 * This injects the currently logged-in User object directly
 * from the Spring Security context. We use this to:
 * - Load the student's specific profile
 * - Ensure students can only see their own data
 * =====================================================
 */
@Controller
@RequestMapping("/student") // All URLs in this controller start with /student
public class StudentController {

    @Autowired private TutorService tutorService;
    @Autowired private BookingService bookingService;
    @Autowired private ReviewService reviewService;
    @Autowired private StudentProfileRepository studentProfileRepository;

    // =====================================================
    // STUDENT DASHBOARD
    // =====================================================

    /**
     * Student's main dashboard page.
     * Shows:
     * - Welcome message
     * - Recent bookings summary
     * - Quick stats (total bookings, pending, completed)
     *
     * @AuthenticationPrincipal injects the currently logged-in user
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {

        // Get the student's profile (needed to query their bookings)
        StudentProfile studentProfile = studentProfileRepository
                .findByUser(currentUser)
                .orElse(null);

        if (studentProfile != null) {
            // Get recent bookings (last 5)
            List<Booking> allBookings = bookingService.getBookingsByStudent(studentProfile);
            List<Booking> recentBookings = allBookings.stream().limit(5).toList();

            // Count bookings by status for stats cards
            long pendingCount = allBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PENDING).count();
            long confirmedCount = allBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
            long completedCount = allBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

            model.addAttribute("recentBookings", recentBookings);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("totalBookings", allBookings.size());
            model.addAttribute("studentProfile", studentProfile);
        }

        model.addAttribute("currentUser", currentUser);
        return "student/dashboard"; // templates/student/dashboard.html
    }

    // =====================================================
    // TUTOR SEARCH
    // =====================================================

    /**
     * Show the tutor search page with optional keyword filter.
     *
     * @param keyword optional search term (?keyword=mathematics)
     */
    @GetMapping("/search")
    public String searchTutors(@RequestParam(required = false) String keyword,
                                Model model) {

        List<TutorProfile> tutors;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Search by keyword
            tutors = tutorService.searchTutors(keyword);
            model.addAttribute("keyword", keyword);
            model.addAttribute("searchPerformed", true);
        } else {
            // Show all available tutors
            tutors = tutorService.getAvailableTutors();
            model.addAttribute("searchPerformed", false);
        }

        model.addAttribute("tutors", tutors);
        model.addAttribute("resultCount", tutors.size());
        return "student/search"; // templates/student/search.html
    }

    /**
     * View a specific tutor's full profile page.
     * Shows bio, qualifications, availability, and reviews.
     *
     * @param tutorId the tutor profile ID from the URL path
     */
    @GetMapping("/tutor/{tutorId}")
    public String viewTutorProfile(@PathVariable Long tutorId, Model model) {
        TutorProfile tutor = tutorService.getTutorProfileById(tutorId);

        // Get reviews for this tutor
        List<Review> reviews = reviewService.getReviewsForTutor(tutor);

        model.addAttribute("tutor", tutor);
        model.addAttribute("reviews", reviews);
        return "student/tutor-profile"; // templates/student/tutor-profile.html
    }

    // =====================================================
    // STUDENT BOOKINGS LIST
    // =====================================================

    /**
     * Show all of the student's bookings.
     * Optional filter by status (?status=PENDING)
     */
    @GetMapping("/bookings")
    public String myBookings(@AuthenticationPrincipal User currentUser,
                              @RequestParam(required = false) String status,
                              Model model) {

        StudentProfile studentProfile = studentProfileRepository
                .findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        List<Booking> bookings = bookingService.getBookingsByStudent(studentProfile);

        // Filter by status if requested
        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus filterStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() == filterStatus)
                        .toList();
                model.addAttribute("filterStatus", filterStatus);
            } catch (IllegalArgumentException e) {
                // Invalid status param - ignore and show all
            }
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        return "student/bookings"; // templates/student/bookings.html
    }

    // =====================================================
    // REVIEW SUBMISSION
    // =====================================================

    /**
     * Show the review form for a completed booking.
     * Only accessible if the booking is COMPLETED and has no review yet.
     */
    @GetMapping("/bookings/{bookingId}/review")
    public String showReviewForm(@PathVariable Long bookingId,
                                  @AuthenticationPrincipal User currentUser,
                                  Model model) {

        Booking booking = bookingService.findById(bookingId);

        // Security: ensure the student owns this booking
        if (!booking.getStudentProfile().getUser().getId().equals(currentUser.getId())) {
            return "redirect:/student/bookings";
        }

        // Can only review completed bookings without an existing review
        if (!booking.canLeaveReview()) {
            return "redirect:/student/bookings";
        }

        model.addAttribute("booking", booking);
        return "student/review-form"; // templates/student/review-form.html
    }

    /**
     * Process the review form submission.
     */
    @PostMapping("/bookings/{bookingId}/review")
    public String submitReview(@PathVariable Long bookingId,
                                @AuthenticationPrincipal User currentUser,
                                @RequestParam int rating,
                                @RequestParam(required = false) String comment,
                                RedirectAttributes redirectAttrs) {

        Booking booking = bookingService.findById(bookingId);

        try {
            reviewService.submitReview(booking, currentUser, rating, comment);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Thank you! Your review has been submitted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/bookings";
    }
}
