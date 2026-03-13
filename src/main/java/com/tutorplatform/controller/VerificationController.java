package com.tutorplatform.controller;

import com.tutorplatform.model.User;
import com.tutorplatform.service.EmailService;
import com.tutorplatform.service.OtpService;
import com.tutorplatform.service.SmsService;
import com.tutorplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * VERIFICATION CONTROLLER
 * =====================================================
 * Handles the OTP (One-Time Password) verification flow
 * that new users must complete after registration when
 * PlatformSettings.requireVerification is true.
 *
 * ROUTES:
 *   GET  /verify  → show the "enter your OTP code" form
 *   POST /verify  → validate the submitted code
 *
 * SECURITY:
 * Both routes are added to Spring Security's permitted list
 * (SecurityConfig.java) because an unverified user might not
 * yet have full access, but still needs to reach this page.
 *
 * The currently authenticated user is injected by Spring Security
 * via @AuthenticationPrincipal — no need to look them up manually.
 *
 * RESEND:
 * GET /verify?resend=true regenerates a new OTP and re-sends it.
 * =====================================================
 */
@Controller
@RequestMapping("/verify")
public class VerificationController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmailService emailService;

    // =====================================================
    // SHOW VERIFICATION FORM
    // =====================================================

    /**
     * Display the OTP entry form to the logged-in user.
     *
     * If the user is already fully verified (both phone and email),
     * redirect them to their appropriate dashboard instead of
     * showing a form they don't need.
     *
     * If the "resend" query parameter is present, a new OTP is
     * generated and sent before showing the form.
     *
     * @param currentUser  the logged-in User (injected by Spring Security)
     * @param resend       optional — if "true", regenerate and resend OTP
     * @param model        Thymeleaf model for template variables
     * @return the "verify" Thymeleaf template, or a redirect
     */
    @GetMapping
    public String showVerifyPage(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false, defaultValue = "false") boolean resend,
            Model model) {

        // Guard: if user is already verified, send them to their dashboard
        if (currentUser != null
                && currentUser.isPhoneVerified()
                && currentUser.isEmailVerified()) {
            return redirectToDashboard(currentUser);
        }

        // If the user requested a fresh OTP (clicked "Resend code")
        if (resend && currentUser != null) {
            sendOtpToUser(currentUser);
            model.addAttribute("infoMessage",
                    "A new verification code has been sent to your phone and email.");
        }

        // Pass the user's masked contact details so the template can say
        // "We sent a code to ***@gmail.com" without exposing full address.
        if (currentUser != null) {
            model.addAttribute("maskedEmail",  maskEmail(currentUser.getEmail()));
            model.addAttribute("maskedPhone",  maskPhone(currentUser.getPhone()));
            model.addAttribute("userEmail",    currentUser.getEmail());
        }

        return "verify"; // → templates/verify.html
    }

    // =====================================================
    // PROCESS SUBMITTED CODE
    // =====================================================

    /**
     * Handle the OTP code submitted by the user.
     *
     * Calls OtpService.validateOtp() which:
     *   - Checks the code matches
     *   - Checks the token hasn't expired
     *   - Marks the token as used
     *   - Sets user.phoneVerified = true, user.emailVerified = true
     *
     * On success: redirects to the user's role-based dashboard.
     * On failure: shows an error message and keeps the form open.
     *
     * @param currentUser the logged-in User (injected by Spring Security)
     * @param code        the 6-digit code typed by the user
     * @param redirectAttrs used to pass flash messages across the redirect
     * @return redirect on success, or back to /verify form on failure
     */
    @PostMapping
    public String submitCode(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String code,
            RedirectAttributes redirectAttrs) {

        // Safety check — should never happen if SecurityConfig is correct
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Validate the submitted code via OtpService
        boolean valid = otpService.validateOtp(currentUser, code);

        if (valid) {
            // Success — the user is now verified
            // Reload from DB to get the updated phoneVerified/emailVerified flags
            User refreshedUser = userService.findById(currentUser.getId());
            redirectAttrs.addFlashAttribute("successMessage",
                    "Your account has been verified successfully! Welcome to Ko-Sir Platform.");
            return redirectToDashboard(refreshedUser);
        } else {
            // Failure — wrong code or expired
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Invalid or expired verification code. "
                            + "Please check the code and try again, or click \"Resend code\".");
            return "redirect:/verify";
        }
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    /**
     * Generate a new OTP for the user and dispatch it via SMS and email.
     * Called when the user first arrives at /verify after registration,
     * or when they click "Resend code".
     *
     * @param user the user who needs a new OTP sent
     */
    private void sendOtpToUser(User user) {
        // Generate a fresh OTP (this also deletes any old token for this user)
        var token = otpService.generateOtp(user);
        String code = token.getCode();

        // Compose the verification message
        String smsBody = "Your Ko-Sir Tutor Platform verification code is: " + code
                + ". This code expires in 15 minutes. Do not share it.";

        String emailSubject = "Your Ko-Sir Platform Verification Code";
        String emailBody = "Hello " + user.getFullName() + ",\n\n"
                + "Your verification code for Ko-Sir Tutor Platform is:\n\n"
                + "    " + code + "\n\n"
                + "This code is valid for 15 minutes.\n"
                + "If you did not register on our platform, please ignore this email.\n\n"
                + "Thank you,\n"
                + "Ko-Sir Tutor Platform Team";

        // Send via SMS (if phone is present and SMS is enabled in settings)
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            smsService.sendSms(user.getPhone(), smsBody);
        }

        // Send via email
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendEmail(user.getEmail(), emailSubject, emailBody);
        }
    }

    /**
     * Redirect the user to the dashboard appropriate for their role.
     *
     * @param user the user to redirect
     * @return a "redirect:/..." Spring MVC redirect string
     */
    private String redirectToDashboard(User user) {
        return switch (user.getRole()) {
            case ADMIN   -> "redirect:/admin/dashboard";
            case TUTOR   -> "redirect:/tutor/dashboard";
            case STUDENT -> "redirect:/student/dashboard";
        };
    }

    /**
     * Mask an email address for display — shows first 2 characters
     * then asterisks up to the "@" sign to protect privacy.
     *
     * Example: "alice.wanjiku@gmail.com" → "al***@gmail.com"
     *
     * @param email full email address
     * @return masked email suitable for display
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local + "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }

    /**
     * Mask a phone number — shows first 3 digits and last 2 digits only.
     *
     * Example: "+254700000005" → "+25*******05"
     *
     * @param phone full phone number
     * @return masked phone suitable for display; "***" if null/blank
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "***";
        if (phone.length() <= 5) return "***";
        return phone.substring(0, 3) + "*******" + phone.substring(phone.length() - 2);
    }
}
