package com.tutorplatform.controller;

import com.tutorplatform.dto.RegisterDto;
import com.tutorplatform.model.Role;
import com.tutorplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AUTHENTICATION CONTROLLER
 * =====================================================
 * Handles login and registration pages/form submissions.
 *
 * Note: The actual LOGIN processing is done by Spring Security
 * automatically (we configured it in SecurityConfig).
 * We only need to show the login page (GET /login).
 *
 * Registration requires both GET (show form) and POST (process form).
 * =====================================================
 */
@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // =====================================================
    // LOGIN PAGE
    // =====================================================

    /**
     * Show the login page.
     *
     * Spring Security processes the form POST to /login automatically.
     * We just need to provide the GET handler to show the form.
     *
     * URL params:
     *   ?error=true   → show "Invalid email or password" message
     *   ?logout=true  → show "You have been logged out" message
     */
    @GetMapping("/login")
    public String loginPage(Model model,
                            String error,
                            String logout) {
        // If login failed, show error message
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }

        // If user just logged out, show logout message
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully logged out.");
        }

        return "auth/login"; // Renders templates/auth/login.html
    }

    // =====================================================
    // REGISTRATION - GET (show empty form)
    // =====================================================

    /**
     * Show the registration form.
     *
     * We add an empty RegisterDto to the model.
     * Thymeleaf binds this to the form fields using th:object="${registerDto}".
     * This is also where validation errors get displayed on the form.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        // Add empty DTO for the form to bind to
        model.addAttribute("registerDto", new RegisterDto());

        // Add roles list so the form can show STUDENT/TUTOR radio buttons
        model.addAttribute("roles", Role.values());

        return "auth/register"; // Renders templates/auth/register.html
    }

    // =====================================================
    // REGISTRATION - POST (process form submission)
    // =====================================================

    /**
     * Process the registration form.
     *
     * HOW VALIDATION WORKS:
     * @Valid triggers the validation annotations on RegisterDto
     * BindingResult captures any validation errors
     * If errors exist, we return the form page again with error messages
     *
     * HOW REDIRECTS WORK:
     * RedirectAttributes lets us pass messages across a redirect.
     * "redirect:/login" sends HTTP 302, browser goes to /login.
     *
     * @param dto             form data (bound from HTML form fields)
     * @param bindingResult   contains any validation errors
     * @param redirectAttrs   for passing flash messages after redirect
     */
    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registerDto") RegisterDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttrs) {

        // If there are validation errors, show the form again with errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "auth/register"; // Stay on registration page with errors shown
        }

        try {
            // Attempt to register the user
            userService.registerUser(dto);

            // Success! Redirect to login with a success message.
            // We use "redirect:" prefix to trigger HTTP redirect.
            if (dto.getRole() == Role.TUTOR) {
                // Tutors need admin approval before they can log in fully
                redirectAttrs.addFlashAttribute("successMessage",
                    "Registration successful! Your tutor account is pending admin approval. " +
                    "You will be notified once approved.");
            } else {
                redirectAttrs.addFlashAttribute("successMessage",
                    "Registration successful! Please log in with your new account.");
            }

            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            // Registration failed (e.g., email already exists, passwords don't match)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "auth/register"; // Stay on registration page with error
        }
    }
}
