package com.tutorplatform.controller;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.BookingStatus;
import com.tutorplatform.model.PlatformSettings;
import com.tutorplatform.model.TutorProfile;
import com.tutorplatform.model.User;
import com.tutorplatform.service.BookingService;
import com.tutorplatform.service.PayhereService;
import com.tutorplatform.service.PlatformSettingsService;
import com.tutorplatform.service.TutorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * BOOKING CONTROLLER
 * =====================================================
 * Handles creating new bookings (from the student's side).
 *
 * Flow:
 * 1. Student finds a tutor on /student/search
 * 2. Clicks "Book Session" → GET /booking/new?tutorId=X
 * 3. Fills out the booking form
 * 4. Submits → POST /booking/new → booking created with PENDING status
 * =====================================================
 */
@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired private BookingService bookingService;
    @Autowired private TutorService tutorService;
    @Autowired private PayhereService payhereService;
    @Autowired private PlatformSettingsService platformSettingsService;

    // =====================================================
    // SHOW BOOKING FORM
    // =====================================================

    /**
     * Show the booking form for a specific tutor.
     *
     * @param tutorId the tutor to book (from ?tutorId=X URL parameter)
     */
    @GetMapping("/new")
    public String showBookingForm(@RequestParam Long tutorId, Model model) {

        // Load the tutor's profile to display in the form
        TutorProfile tutor = tutorService.getTutorProfileById(tutorId);

        // Make sure the tutor is still available and approved
        if (!tutor.isApproved() || !tutor.isAcceptingBookings()) {
            return "redirect:/student/search?error=tutor-unavailable";
        }

        model.addAttribute("tutor", tutor);

        // Duration options: 1, 2, or 3 hours
        model.addAttribute("durationOptions", new int[]{1, 2, 3});

        // Set minimum date to today (can't book in the past)
        model.addAttribute("minDate", LocalDate.now().toString());

        // Teaching mode drives which address fields the booking form shows
        // HOME_VISIT      → student fills in their home address
        // FIXED_LOCATION  → show tutor's address (read-only), student just confirms
        // BOTH            → student picks option, then fills/sees the right address
        model.addAttribute("teachingMode", tutor.getTeachingMode() != null ? tutor.getTeachingMode() : "HOME_VISIT");

        return "booking/book"; // templates/booking/book.html
    }

    // =====================================================
    // PROCESS BOOKING FORM
    // =====================================================

    /**
     * Process the booking form submission.
     * Creates a new booking with PENDING status.
     *
     * @param tutorId  the tutor being booked
     * @param subject  subject for the session
     * @param date     session date (from date input)
     * @param time     session start time (from time input)
     * @param duration session length in hours
     * @param notes    optional notes from student
     */
    @PostMapping("/new")
    public String processBooking(
            @AuthenticationPrincipal User currentUser,
            @RequestParam Long tutorId,
            @RequestParam String subject,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam int duration,
            @RequestParam(required = false) String sessionLocation,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttrs) {

        try {
            TutorProfile tutor = tutorService.getTutorProfileById(tutorId);

            // Parse the date and time strings from the HTML form
            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime startTime = LocalTime.parse(time);

            // Validate booking date is not in the past
            if (bookingDate.isBefore(LocalDate.now())) {
                redirectAttrs.addFlashAttribute("errorMessage",
                        "Booking date cannot be in the past.");
                return "redirect:/booking/new?tutorId=" + tutorId;
            }

            // Resolve the session location:
            // For FIXED_LOCATION tutors the student doesn't enter an address —
            // we use the tutor's own teaching address.
            // For HOME_VISIT and BOTH, the student submits their address.
            String resolvedLocation = sessionLocation;
            if ("FIXED_LOCATION".equals(tutor.getTeachingMode())) {
                resolvedLocation = tutor.getTeachingAddress();
            }

            // Create the booking (status = PENDING_PAYMENT until student pays)
            var booking = bookingService.createBooking(
                    currentUser, tutor, subject, bookingDate, startTime, duration,
                    resolvedLocation, notes
            );

            // Redirect to payment page — student must pay before tutor is notified
            return "redirect:/booking/payment/" + booking.getId();

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Failed to create booking: " + e.getMessage());
            return "redirect:/booking/new?tutorId=" + tutorId;
        }
    }

    // =====================================================
    // PAYMENT PAGE — show payment summary + card form
    // =====================================================

    /**
     * Show the payment page for a booking that is awaiting payment.
     * After the student fills in card details and submits, the booking
     * moves to PENDING status (visible to the tutor for confirmation).
     *
     * @param bookingId the booking to pay for
     */
    @GetMapping("/payment/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId,
                                   @AuthenticationPrincipal User currentUser,
                                   HttpServletRequest request,
                                   Model model) {
        Booking booking = bookingService.findById(bookingId);

        // Security: ensure the booking belongs to this student
        if (!booking.getStudentProfile().getUser().getId().equals(currentUser.getId())) {
            return "redirect:/student/bookings";
        }

        // Only show payment page for bookings that still need payment
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return "redirect:/student/bookings";
        }

        PlatformSettings settings = platformSettingsService.getSettings();
        model.addAttribute("booking", booking);
        model.addAttribute("payhereEnabled", settings.isPayhereEnabled());

        // If PayHere is enabled, generate the checkout parameters (hash computed server-side)
        if (settings.isPayhereEnabled()) {
            // Build base URL from the incoming request (handles localhost, ngrok, production domains)
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                       ? ":" + request.getServerPort() : "");
            model.addAttribute("payhereCheckoutUrl", payhereService.getCheckoutUrl());
            model.addAttribute("payhereParams", payhereService.buildCheckoutParams(booking, baseUrl));
        }

        return "booking/payment"; // templates/booking/payment.html
    }

    /**
     * Process the payment form submission.
     * In a real system this would call PayHere / Stripe API.
     * Here we simulate a successful payment by moving status to PENDING.
     *
     * After payment, the tutor can see and confirm/reject the booking.
     *
     * @param bookingId   the booking being paid for
     * @param cardNumber  card number (not stored — demo only)
     * @param cardExpiry  card expiry (not stored — demo only)
     * @param cardCvc     CVC (not stored — demo only)
     */
    @PostMapping("/payment/{bookingId}")
    public String processPayment(@PathVariable Long bookingId,
                                  @AuthenticationPrincipal User currentUser,
                                  @RequestParam(required = false) String cardNumber,
                                  @RequestParam(required = false) String cardExpiry,
                                  @RequestParam(required = false) String cardCvc,
                                  RedirectAttributes redirectAttrs) {
        try {
            var booking = bookingService.findById(bookingId);

            // Security check
            if (!booking.getStudentProfile().getUser().getId().equals(currentUser.getId())) {
                redirectAttrs.addFlashAttribute("errorMessage", "Unauthorized.");
                return "redirect:/student/bookings";
            }

            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                redirectAttrs.addFlashAttribute("errorMessage", "This booking has already been paid.");
                return "redirect:/student/bookings";
            }

            // Basic card validation (demo only — not real payment processing)
            if (cardNumber == null || cardNumber.replaceAll("\\s", "").length() < 16) {
                redirectAttrs.addFlashAttribute("errorMessage", "Please enter a valid card number.");
                return "redirect:/booking/payment/" + bookingId;
            }

            // Simulate payment success → move booking to PENDING (awaiting tutor confirmation)
            bookingService.adminUpdateBookingStatus(bookingId,
                    BookingStatus.PENDING,
                    "Payment received. Awaiting tutor confirmation.");

            redirectAttrs.addFlashAttribute("successMessage",
                    "Payment successful! LKR " + booking.getTotalPrice() +
                    " paid. Waiting for " + booking.getTutorProfile().getUser().getFullName() +
                    " to confirm your session.");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Payment failed: " + e.getMessage());
            return "redirect:/booking/payment/" + bookingId;
        }

        return "redirect:/student/bookings";
    }

    // =====================================================
    // PAYHERE CALLBACKS
    // =====================================================

    /**
     * PayHere Notify URL — server-to-server callback.
     *
     * PayHere POSTs to this endpoint after every payment attempt.
     * This is the AUTHORITATIVE payment confirmation — NOT the return_url,
     * which can be manipulated by the user.
     *
     * SECURITY:
     * 1. We verify the md5sig hash before touching the booking.
     * 2. We only mark the booking PAID when status_code = "2" (success).
     * 3. This endpoint must be publicly reachable (use ngrok for localhost).
     *
     * PayHere status codes:
     *   2  = success
     *   0  = pending
     *  -1  = cancelled
     *  -2  = failed
     *  -3  = chargeback
     *
     * Note: Spring Security CSRF is disabled for this endpoint because
     * PayHere is an external server that cannot include a CSRF token.
     * Add the path to SecurityConfig's csrf().ignoringRequestMatchers().
     */
    @PostMapping("/payment/notify")
    @ResponseBody
    public ResponseEntity<String> handlePayhereNotify(
            @RequestParam("merchant_id")      String merchantId,
            @RequestParam("order_id")         String orderId,
            @RequestParam("payhere_amount")   String amount,
            @RequestParam("payhere_currency") String currency,
            @RequestParam("status_code")      String statusCode,
            @RequestParam("md5sig")           String md5sig) {

        PlatformSettings settings = platformSettingsService.getSettings();

        // 1. Verify the hash — reject fraudulent callbacks
        boolean valid = payhereService.verifyNotification(
                merchantId, orderId, amount, currency, statusCode, md5sig,
                settings.getPayhereMerchantSecret()
        );

        if (!valid) {
            // Hash mismatch — likely a forged notification; log and ignore
            System.err.println("[PayHere] INVALID notification hash for order: " + orderId);
            return ResponseEntity.ok("INVALID");
        }

        // 2. Extract booking ID from order_id ("BOOKING-123" → 123)
        if (!"2".equals(statusCode)) {
            // Not a successful payment (pending/cancelled/failed) — no action needed
            return ResponseEntity.ok("OK");
        }

        try {
            String bookingIdStr = orderId.replace("BOOKING-", "");
            Long bookingId = Long.parseLong(bookingIdStr);
            Booking booking = bookingService.findById(bookingId);

            // 3. Only process if still PENDING_PAYMENT (idempotency — PayHere may retry)
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                bookingService.adminUpdateBookingStatus(bookingId,
                        BookingStatus.PENDING,
                        "PayHere payment received. Amount: " + amount + " " + currency);
            }
        } catch (Exception e) {
            System.err.println("[PayHere] Error processing notify for " + orderId + ": " + e.getMessage());
            return ResponseEntity.ok("ERROR");
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * PayHere Return URL — user is redirected here after successful payment.
     *
     * IMPORTANT: Do NOT mark the booking as paid here — the user can manipulate
     * this URL. The notify_url callback is the authoritative confirmation.
     * This endpoint just shows a "thank you" message; the booking status
     * will already be updated by the time PayHere redirects (usually).
     */
    @GetMapping("/payment/success")
    public String handlePaymentSuccess(@RequestParam Long bookingId,
                                        RedirectAttributes redirectAttrs) {
        try {
            Booking booking = bookingService.findById(bookingId);
            if (booking.getStatus() == BookingStatus.PENDING
                    || booking.getStatus() == BookingStatus.CONFIRMED) {
                redirectAttrs.addFlashAttribute("successMessage",
                        "Payment successful! LKR " + booking.getTotalPrice()
                        + " paid via PayHere. Waiting for "
                        + booking.getTutorProfile().getUser().getFullName()
                        + " to confirm your session.");
            } else {
                // Notify may not have arrived yet — show a softer message
                redirectAttrs.addFlashAttribute("successMessage",
                        "Thank you! Your payment is being confirmed. "
                        + "Your booking will be updated shortly.");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("successMessage",
                    "Payment received. Your booking will be confirmed shortly.");
        }
        return "redirect:/student/bookings";
    }

    /**
     * PayHere Cancel URL — user is redirected here if they cancel on PayHere's page.
     * The booking remains PENDING_PAYMENT so they can try again.
     */
    @GetMapping("/payment/cancel")
    public String handlePaymentCancel(@RequestParam Long bookingId,
                                       RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("errorMessage",
                "Payment was cancelled. Your booking is still saved — "
                + "you can complete payment from My Bookings.");
        return "redirect:/student/bookings";
    }

    // =====================================================
    // CANCEL BOOKING (by student)
    // =====================================================

    /**
     * Student cancels their own booking.
     * Only PENDING bookings can be cancelled by the student.
     */
    @PostMapping("/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId,
                                 @AuthenticationPrincipal User currentUser,
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes redirectAttrs) {
        try {
            // Verify the booking belongs to this student before cancelling
            var booking = bookingService.findById(bookingId);
            if (!booking.getStudentProfile().getUser().getId().equals(currentUser.getId())) {
                redirectAttrs.addFlashAttribute("errorMessage", "Unauthorized action.");
                return "redirect:/student/bookings";
            }

            bookingService.cancelBooking(bookingId, reason != null ? reason : "Cancelled by student");
            redirectAttrs.addFlashAttribute("successMessage", "Booking cancelled successfully.");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/bookings";
    }
}
