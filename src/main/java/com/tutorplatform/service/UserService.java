package com.tutorplatform.service;

import com.tutorplatform.dto.RegisterDto;
import com.tutorplatform.model.*;
import com.tutorplatform.repository.StudentProfileRepository;
import com.tutorplatform.repository.TutorProfileRepository;
import com.tutorplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
// OtpToken is in com.tutorplatform.model.* (wildcard above covers it)
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * USER SERVICE
 * =====================================================
 * Business logic layer for all user-related operations.
 *
 * This service does two important things:
 * 1. Implements UserDetailsService → Spring Security calls
 *    loadUserByUsername() when someone logs in
 * 2. Handles registration → creates User + StudentProfile or TutorProfile
 *
 * SPRING SERVICE PATTERN:
 * Controller receives HTTP request → calls Service method
 * Service contains all the business logic → calls Repository
 * Repository talks to the database → returns data back up the chain
 *
 * @Service marks this as a Spring-managed service bean.
 * @Transactional on the class means all methods run in a DB transaction.
 * If any exception occurs, the transaction rolls back automatically.
 * =====================================================
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    // =====================================================
    // DEPENDENCIES - Spring injects these automatically
    // @Autowired tells Spring to inject the matching bean
    // =====================================================

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    /**
     * PasswordEncoder is defined in SecurityConfig.
     * It uses BCrypt to hash passwords securely.
     * We NEVER store plain-text passwords!
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    // =====================================================
    // VERIFICATION DEPENDENCIES
    // These are injected @Lazy to break any potential
    // circular dependency that could arise because
    // OtpService → UserRepository → UserService.
    // @Lazy tells Spring to inject a proxy and only
    // resolve the real bean when first actually used.
    // =====================================================

    /** Reads requireVerification flag from the settings table */
    @Autowired
    @Lazy
    private PlatformSettingsService platformSettingsService;

    /**
     * Generates a 6-digit OTP token and saves it to the DB.
     * Called here after registration when verification is required.
     */
    @Autowired
    @Lazy
    private OtpService otpService;

    /** Sends the OTP code to the user's phone number via SMS gateway */
    @Autowired
    @Lazy
    private SmsService smsService;

    /** Sends the OTP code to the user's email address via SMTP */
    @Autowired
    @Lazy
    private EmailService emailService;

    // =====================================================
    // SPRING SECURITY INTEGRATION
    // Spring Security calls this method when a user tries to log in.
    // It looks up the user by their "username" (which is email for us).
    // =====================================================

    /**
     * Load user by email (Spring Security uses "username" term but
     * we configured it to use email as the username).
     *
     * This is called automatically by Spring Security during login.
     * If the user is found, Spring Security checks the password.
     * If not found, login fails with "Bad credentials".
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with email: " + email
                ));
    }

    // =====================================================
    // REGISTRATION
    // Creates a new User + their profile (Student or Tutor)
    // =====================================================

    /**
     * Register a new user from the registration form data.
     *
     * PROCESS:
     * 1. Validate the email isn't already taken
     * 2. Validate passwords match
     * 3. Create and save the User entity
     * 4. Create the role-specific profile (Student or Tutor)
     * 5. Save the profile
     *
     * @param dto Registration form data
     * @throws IllegalArgumentException if validation fails
     */
    public User registerUser(RegisterDto dto) {
        // Check if email is already registered
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        // Check passwords match
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create the User entity
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        // HASH the password before saving! Never store plain text.
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setActive(true);

        // =====================================================
        // VERIFICATION CHECK
        // If the admin has enabled requireVerification in the
        // platform settings, mark the new user as unverified.
        // They must enter an OTP before they can use the platform.
        // If requireVerification is false, the defaults on User
        // (phoneVerified=true, emailVerified=true) mean the user
        // is automatically considered verified on registration.
        // =====================================================
        boolean requireVerification = platformSettingsService
                .getSettings()
                .isRequireVerification();

        if (requireVerification) {
            // Mark both flags as false — user must complete OTP flow
            user.setPhoneVerified(false);
            user.setEmailVerified(false);
        }
        // else: User defaults are true (already "verified") — nothing to do

        // Save the user first (we need the user.id for the profile FK)
        User savedUser = userRepository.save(user);

        // Create the appropriate profile based on role
        if (dto.getRole() == Role.STUDENT) {
            createStudentProfile(savedUser, dto);
        } else if (dto.getRole() == Role.TUTOR) {
            createTutorProfile(savedUser, dto);
        }

        // =====================================================
        // SEND OTP IF VERIFICATION IS REQUIRED
        // Generate a 6-digit OTP, save it to the DB, then
        // dispatch it to the user's phone (SMS) and email.
        // This is done AFTER the profile is created so the user
        // row is fully committed before we reference it from OTP.
        // =====================================================
        if (requireVerification) {
            try {
                // Generate and persist the OTP token
                OtpToken token = otpService.generateOtp(savedUser);
                String code = token.getCode();

                // Build the SMS message (keep under 160 chars)
                String smsBody = "Your Ko-Sir Tutor Platform code: " + code
                        + ". Valid 15 min. Do not share.";

                // Build the email message
                String emailSubject = "Your Ko-Sir Platform Verification Code";
                String emailBody = "Hello " + savedUser.getFullName() + ",\n\n"
                        + "Thank you for registering on Ko-Sir Tutor Platform!\n\n"
                        + "Your verification code is:\n\n"
                        + "    " + code + "\n\n"
                        + "This code is valid for 15 minutes.\n"
                        + "Enter it at: http://localhost:8080/verify\n\n"
                        + "If you did not register, please ignore this email.\n\n"
                        + "Ko-Sir Tutor Platform Team";

                // Send SMS (swallows errors internally)
                if (savedUser.getPhone() != null && !savedUser.getPhone().isBlank()) {
                    smsService.sendSms(savedUser.getPhone(), smsBody);
                }

                // Send email (swallows errors internally)
                emailService.sendEmail(savedUser.getEmail(), emailSubject, emailBody);

                System.out.println(">>> UserService: OTP sent to new user "
                        + savedUser.getEmail());

            } catch (Exception e) {
                // OTP sending failure must NOT prevent registration.
                // The user can request a new code via /verify?resend=true
                System.out.println(">>> UserService: Failed to send OTP for "
                        + savedUser.getEmail() + ": " + e.getMessage());
            }
        }

        return savedUser;
    }

    /**
     * Creates a StudentProfile for the newly registered user.
     * Called internally by registerUser() when role=STUDENT.
     */
    private void createStudentProfile(User user, RegisterDto dto) {
        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setGrade(dto.getGrade());
        profile.setSchool(dto.getSchool());
        profile.setParentName(dto.getParentName());
        profile.setParentPhone(dto.getParentPhone());
        studentProfileRepository.save(profile);
    }

    /**
     * Creates a TutorProfile for the newly registered tutor.
     * New tutors are NOT approved by default - admin must approve them.
     */
    private void createTutorProfile(User user, RegisterDto dto) {
        TutorProfile profile = new TutorProfile();
        profile.setUser(user);
        profile.setSubjects(dto.getSubjects());
        profile.setQualification(dto.getQualification());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setLocation(dto.getLocation());

        // Parse hourly rate from string to BigDecimal
        try {
            if (dto.getHourlyRate() != null && !dto.getHourlyRate().isEmpty()) {
                profile.setHourlyRate(new BigDecimal(dto.getHourlyRate()));
            } else {
                profile.setHourlyRate(BigDecimal.ZERO);
            }
        } catch (NumberFormatException e) {
            profile.setHourlyRate(BigDecimal.ZERO);
        }

        // New tutors need admin approval before appearing in search results
        profile.setApproved(false);
        profile.setAcceptingBookings(true);

        tutorProfileRepository.save(profile);
    }

    // =====================================================
    // ADMIN OPERATIONS
    // These methods are called from AdminController
    // =====================================================

    /**
     * Admin creates a new user directly (bypasses the public registration form).
     * The admin can set any role including ADMIN.
     */
    public User adminCreateUser(String fullName, String email, String rawPassword,
                                 String phone, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User(fullName, email, passwordEncoder.encode(rawPassword), phone, role);
        return userRepository.save(user);
    }

    /**
     * Toggle a user's active status.
     * Admin can deactivate users (they can no longer log in).
     */
    public User toggleUserStatus(Long userId) {
        User user = findById(userId);
        user.setActive(!user.isActive()); // Flip the boolean
        return userRepository.save(user);
    }

    /**
     * Update user details (admin or user themselves editing their profile).
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Delete a user by ID.
     * Admin only. This cascades and deletes their profile too.
     */
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Change a user's password.
     * @param userId  the user to update
     * @param newPass new plain-text password (will be hashed)
     */
    public void changePassword(Long userId, String newPass) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
    }

    // =====================================================
    // QUERY METHODS (READ ONLY)
    // @Transactional(readOnly = true) is an optimization:
    // tells Hibernate it doesn't need to track changes for these calls.
    // =====================================================

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
