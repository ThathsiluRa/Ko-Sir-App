package com.tutorplatform.controller;

import com.tutorplatform.model.*;
import com.tutorplatform.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * ADMIN CONTROLLER
 * =====================================================
 * Full administrative control panel for the platform.
 *
 * Admin can:
 * - View platform statistics dashboard
 * - Manage ALL users (view, create, edit, deactivate, delete)
 * - Approve/reject tutor registrations
 * - View and manage ALL bookings
 * - Update booking statuses
 * - View and delete reviews
 * - Add new tutors directly
 *
 * ALL routes here are secured: only ROLE_ADMIN can access /admin/**
 * (Configured in SecurityConfig.java)
 * =====================================================
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private TutorService tutorService;
    @Autowired private BookingService bookingService;
    @Autowired private ReviewService reviewService;

    /**
     * PlatformSettingsService handles loading and saving the single
     * platform_settings row.  Injected here so the admin can view
     * and update the settings via /admin/settings.
     */
    @Autowired private PlatformSettingsService platformSettingsService;

    // =====================================================
    // ADMIN DASHBOARD
    // The main stats overview page.
    // =====================================================

    /**
     * Admin dashboard: shows platform-wide statistics.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // ---- PLATFORM STATS ----
        // User counts
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalTutors", userService.countByRole(Role.TUTOR));
        model.addAttribute("approvedTutors", tutorService.countApprovedTutors());
        model.addAttribute("pendingTutors",
                tutorService.getPendingApprovalTutors().size());

        // Booking counts
        model.addAttribute("totalBookings", bookingService.countAllBookings());
        model.addAttribute("pendingBookings",
                bookingService.countByStatus(BookingStatus.PENDING));
        model.addAttribute("confirmedBookings",
                bookingService.countByStatus(BookingStatus.CONFIRMED));
        model.addAttribute("completedBookings",
                bookingService.countByStatus(BookingStatus.COMPLETED));
        model.addAttribute("todayBookings", bookingService.countTodayBookings());

        // Review count
        model.addAttribute("totalReviews", reviewService.countAllReviews());

        // Recent data lists (for activity feed on dashboard)
        model.addAttribute("pendingTutorsList",
                tutorService.getPendingApprovalTutors().stream().limit(5).toList());
        model.addAttribute("recentBookings",
                bookingService.getBookingsByStatus(BookingStatus.PENDING)
                        .stream().limit(5).toList());

        return "admin/dashboard"; // templates/admin/dashboard.html
    }

    // =====================================================
    // USER MANAGEMENT
    // =====================================================

    /**
     * List all users with optional role filter.
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String role, Model model) {

        List<User> users;

        if (role != null && !role.isEmpty()) {
            try {
                Role filterRole = Role.valueOf(role.toUpperCase());
                users = userService.findByRole(filterRole);
                model.addAttribute("filterRole", filterRole);
            } catch (IllegalArgumentException e) {
                users = userService.findAll();
            }
        } else {
            users = userService.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        return "admin/users"; // templates/admin/users.html
    }

    /**
     * Toggle a user's active status (activate/deactivate account).
     */
    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable Long userId,
                                    RedirectAttributes redirectAttrs) {
        User user = userService.toggleUserStatus(userId);
        String msg = user.isActive()
                ? user.getFullName() + "'s account has been activated."
                : user.getFullName() + "'s account has been deactivated.";
        redirectAttrs.addFlashAttribute("successMessage", msg);
        return "redirect:/admin/users";
    }

    /**
     * Show the create new user form.
     */
    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("roles", Role.values());
        return "admin/user-form"; // templates/admin/user-form.html
    }

    /**
     * Process the create new user form (admin can create any role).
     */
    @PostMapping("/users/new")
    public String createUser(@RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam String phone,
                              @RequestParam String role,
                              RedirectAttributes redirectAttrs) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            userService.adminCreateUser(fullName, email, password, phone, userRole);
            redirectAttrs.addFlashAttribute("successMessage",
                    "User '" + fullName + "' created successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to create user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Show the edit user form.
     */
    @GetMapping("/users/{userId}/edit")
    public String showEditUserForm(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId);
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/user-edit"; // templates/admin/user-edit.html
    }

    /**
     * Process the edit user form.
     */
    @PostMapping("/users/{userId}/edit")
    public String updateUser(@PathVariable Long userId,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String phone,
                              @RequestParam String role,
                              RedirectAttributes redirectAttrs) {
        try {
            User user = userService.findById(userId);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(Role.valueOf(role.toUpperCase()));
            userService.updateUser(user);
            redirectAttrs.addFlashAttribute("successMessage",
                    "User updated successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to update user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Delete a user permanently.
     */
    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttrs) {
        try {
            userService.deleteUser(userId);
            redirectAttrs.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // =====================================================
    // TUTOR MANAGEMENT
    // =====================================================

    /**
     * List all tutor profiles (including unapproved ones).
     */
    @GetMapping("/tutors")
    public String listTutors(Model model) {
        model.addAttribute("tutors", tutorService.getAllTutorProfiles());
        model.addAttribute("pendingCount", tutorService.getPendingApprovalTutors().size());
        return "admin/tutors"; // templates/admin/tutors.html
    }

    /**
     * Approve a tutor - they can now be found by students.
     */
    @PostMapping("/tutors/{tutorId}/approve")
    public String approveTutor(@PathVariable Long tutorId, RedirectAttributes redirectAttrs) {
        TutorProfile tutor = tutorService.approveTutor(tutorId);
        redirectAttrs.addFlashAttribute("successMessage",
                tutor.getUser().getFullName() + " has been approved as a tutor.");
        return "redirect:/admin/tutors";
    }

    /**
     * Revoke a tutor's approval - they are hidden from searches.
     */
    @PostMapping("/tutors/{tutorId}/revoke")
    public String revokeTutorApproval(@PathVariable Long tutorId,
                                       RedirectAttributes redirectAttrs) {
        TutorProfile tutor = tutorService.revokeTutorApproval(tutorId);
        redirectAttrs.addFlashAttribute("successMessage",
                tutor.getUser().getFullName() + "'s tutor approval has been revoked.");
        return "redirect:/admin/tutors";
    }

    /**
     * Show form to directly add a new tutor (admin creates tutor + profile together).
     */
    @GetMapping("/tutors/new")
    public String showAddTutorForm(Model model) {
        return "admin/tutor-form"; // templates/admin/tutor-form.html
    }

    /**
     * Process the add tutor form.
     * Admin adds tutors directly - they are auto-approved.
     */
    @PostMapping("/tutors/new")
    public String addTutor(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String phone,
                            @RequestParam String password,
                            @RequestParam String subjects,
                            @RequestParam String hourlyRate,
                            @RequestParam String location,
                            @RequestParam String qualification,
                            @RequestParam(required = false) Integer experienceYears,
                            @RequestParam(required = false) String bio,
                            @RequestParam(required = false) String availability,
                            RedirectAttributes redirectAttrs) {
        try {
            // Create the user account
            User tutorUser = userService.adminCreateUser(
                    fullName, email, password, phone, Role.TUTOR);

            // Create and auto-approve the tutor profile
            TutorProfile profile = new TutorProfile();
            profile.setUser(tutorUser);
            profile.setSubjects(subjects);
            profile.setHourlyRate(new BigDecimal(hourlyRate));
            profile.setLocation(location);
            profile.setQualification(qualification);
            profile.setExperienceYears(experienceYears != null ? experienceYears : 0);
            profile.setBio(bio);
            profile.setAvailability(availability);
            profile.setApproved(true);       // Admin-created tutors are pre-approved
            profile.setAcceptingBookings(true);

            tutorService.saveTutorProfile(profile);

            redirectAttrs.addFlashAttribute("successMessage",
                    "Tutor '" + fullName + "' added and approved successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to add tutor: " + e.getMessage());
        }
        return "redirect:/admin/tutors";
    }

    // =====================================================
    // BOOKING MANAGEMENT
    // =====================================================

    /**
     * View all bookings with optional status filter.
     */
    @GetMapping("/bookings")
    public String listBookings(@RequestParam(required = false) String status, Model model) {

        List<Booking> bookings;

        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus filterStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingService.getBookingsByStatus(filterStatus);
                model.addAttribute("filterStatus", filterStatus);
            } catch (IllegalArgumentException e) {
                bookings = bookingService.getAllBookings();
            }
        } else {
            bookings = bookingService.getAllBookings();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());

        // Stats for the filter bar
        model.addAttribute("pendingCount",
                bookingService.countByStatus(BookingStatus.PENDING));
        model.addAttribute("confirmedCount",
                bookingService.countByStatus(BookingStatus.CONFIRMED));
        model.addAttribute("completedCount",
                bookingService.countByStatus(BookingStatus.COMPLETED));

        return "admin/bookings"; // templates/admin/bookings.html
    }

    /**
     * Admin updates a booking's status.
     * Admin has full override - can set any status.
     */
    @PostMapping("/bookings/{bookingId}/status")
    public String updateBookingStatus(@PathVariable Long bookingId,
                                       @RequestParam String status,
                                       @RequestParam(required = false) String note,
                                       RedirectAttributes redirectAttrs) {
        try {
            BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
            bookingService.adminUpdateBookingStatus(bookingId, newStatus, note);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Booking #" + bookingId + " status updated to " + newStatus);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    /**
     * Admin deletes a booking.
     */
    @PostMapping("/bookings/{bookingId}/delete")
    public String deleteBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttrs) {
        try {
            // We cancel instead of hard-delete to preserve history
            bookingService.cancelBooking(bookingId, "Removed by administrator");
            redirectAttrs.addFlashAttribute("successMessage",
                    "Booking #" + bookingId + " has been cancelled.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // =====================================================
    // REVIEW MANAGEMENT
    // =====================================================

    /**
     * View all reviews on the platform.
     */
    @GetMapping("/reviews")
    public String listReviews(Model model) {
        model.addAttribute("reviews", reviewService.getAllReviews());
        model.addAttribute("totalReviews", reviewService.countAllReviews());
        return "admin/reviews"; // templates/admin/reviews.html
    }

    /**
     * Admin deletes a review.
     * This also recalculates the tutor's average rating.
     */
    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId, RedirectAttributes redirectAttrs) {
        try {
            reviewService.deleteReview(reviewId);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Review deleted and tutor rating recalculated.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to delete review: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    // =====================================================
    // PLATFORM SETTINGS MANAGEMENT
    // The admin can configure SMS (text.lk / Twilio) and
    // email (SMTP) credentials, plus the verification policy.
    // =====================================================

    /**
     * Show the platform settings form.
     *
     * Loads the current PlatformSettings row (or a fresh default if
     * none exists yet) and passes it to the Thymeleaf template as
     * a model attribute for form binding.
     *
     * URL: GET /admin/settings
     * Template: templates/admin/settings.html
     */
    @GetMapping("/settings")
    public String showSettings(Model model) {
        // getOrCreateDefault() ensures a row always exists in the DB.
        // The object is passed to the form so all current values are
        // pre-filled in the input fields.
        PlatformSettings settings = platformSettingsService.getOrCreateDefault();
        model.addAttribute("settings", settings);
        return "admin/settings"; // → templates/admin/settings.html
    }

    /**
     * Save updated platform settings submitted by the admin.
     *
     * Spring MVC binds the form fields automatically to the
     * PlatformSettings object via @ModelAttribute.
     * We then pass it to PlatformSettingsService.saveSettings()
     * which forces id=1 and calls repository.save().
     *
     * Checkboxes (smsEnabled, emailEnabled, requireVerification) are
     * handled correctly: unchecked checkboxes are NOT submitted by
     * browsers, so Spring MVC leaves the boolean field as false —
     * exactly the correct behaviour.
     *
     * URL: POST /admin/settings
     * Redirects to: GET /admin/settings (Post/Redirect/Get pattern)
     */
    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute PlatformSettings settings,
                                RedirectAttributes redirectAttrs) {
        try {
            platformSettingsService.saveSettings(settings);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Platform settings saved successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to save settings: " + e.getMessage());
        }
        // Post/Redirect/Get: redirect to GET so browser refresh doesn't resubmit
        return "redirect:/admin/settings";
    }
}
