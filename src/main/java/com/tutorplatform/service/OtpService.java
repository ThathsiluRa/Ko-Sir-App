package com.tutorplatform.service;

import com.tutorplatform.model.OtpToken;
import com.tutorplatform.model.User;
import com.tutorplatform.repository.OtpTokenRepository;
import com.tutorplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OTP SERVICE
 * =====================================================
 * Handles the full lifecycle of One-Time Password tokens
 * used for phone/email verification.
 *
 * FLOW:
 *   Registration (UserService)
 *     → generateOtp(user)          ← creates & saves a 6-digit OTP token
 *     → SmsService.sendSms(...)    ← sends the code via SMS
 *     → EmailService.sendEmail(...)← sends the code via email
 *
 *   Verification page (/verify)
 *     → validateOtp(user, code)    ← checks code, marks user verified
 *
 * OTP RULES:
 *   - 6 digits, zero-padded (e.g. "048302")
 *   - Expires 15 minutes after creation
 *   - Single-use: once validated it is marked used = true
 *   - Generating a new OTP deletes any old one for the same user
 *
 * @Service   → Spring-managed service bean
 * @Transactional → DB transactions; rolls back on exception
 * =====================================================
 */
@Service
@Transactional
public class OtpService {

    /**
     * Cryptographically secure random number generator.
     * Using SecureRandom (not java.util.Random) because we are
     * generating authentication tokens — weak RNGs are predictable.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * OTP validity window in minutes.
     * After this many minutes the token is considered expired and
     * the user must request a new one.
     */
    private static final int OTP_VALID_MINUTES = 15;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // =====================================================
    // GENERATE
    // =====================================================

    /**
     * Generate a fresh OTP token for the given user.
     *
     * STEPS:
     * 1. Delete any existing (unused) token for this user so only
     *    one token is active at a time.
     * 2. Generate a cryptographically random 6-digit code.
     * 3. Set createdAt = now, expiresAt = now + 15 minutes.
     * 4. Persist and return the new OtpToken.
     *
     * The caller (UserService) is responsible for sending the code
     * to the user via SmsService and EmailService.
     *
     * @param user the user who needs to verify their account
     * @return the newly created OtpToken (contains the code to send)
     */
    public OtpToken generateOtp(User user) {
        // Step 1 — remove any previous token so there is at most one active
        otpTokenRepository.deleteByUser(user);

        // Step 2 — generate a 6-digit code using SecureRandom
        // nextInt(1_000_000) gives 0–999999; String.format pads to 6 digits
        int rawCode = SECURE_RANDOM.nextInt(1_000_000);
        String code = String.format("%06d", rawCode);

        // Step 3 — set timestamps
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(OTP_VALID_MINUTES);

        // Step 4 — persist and return
        OtpToken token = new OtpToken(user, code, now, expiry);
        OtpToken saved = otpTokenRepository.save(token);

        System.out.println(">>> OtpService: Generated OTP for user "
                + user.getEmail() + " (expires " + expiry + ")");

        return saved;
    }

    // =====================================================
    // VALIDATE
    // =====================================================

    /**
     * Validate a user-supplied OTP code against the stored token.
     *
     * VALIDATION CHECKS (all must pass):
     * 1. A token exists for this user and is not yet used
     * 2. The token has not expired (current time < expiresAt)
     * 3. The supplied code matches the stored code (case-insensitive
     *    trim to tolerate minor whitespace / copy-paste issues)
     *
     * ON SUCCESS:
     * - Marks the OtpToken as used (prevents replay)
     * - Sets user.phoneVerified = true
     * - Sets user.emailVerified = true
     * - Persists the updated User entity
     *
     * ON FAILURE:
     * - Returns false; token remains unchanged so the user can retry
     *   (until the token expires)
     *
     * @param user            the user attempting verification
     * @param submittedCode   the code the user typed on the /verify page
     * @return true if verification succeeded, false otherwise
     */
    public boolean validateOtp(User user, String submittedCode) {
        // Look up the active (unused) token for this user
        Optional<OtpToken> optionalToken = otpTokenRepository.findByUserAndUsedFalse(user);

        if (optionalToken.isEmpty()) {
            // No active token found (never generated, already used, or deleted)
            System.out.println(">>> OtpService: No active token found for " + user.getEmail());
            return false;
        }

        OtpToken token = optionalToken.get();

        // Check expiry
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            System.out.println(">>> OtpService: Token expired for " + user.getEmail());
            return false;
        }

        // Check code match (trim whitespace to be lenient about copy-paste)
        if (!token.getCode().equals(submittedCode.trim())) {
            System.out.println(">>> OtpService: Incorrect code supplied for " + user.getEmail());
            return false;
        }

        // All checks passed — mark token as used
        token.setUsed(true);
        otpTokenRepository.save(token);

        // Mark user as fully verified
        user.setPhoneVerified(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        System.out.println(">>> OtpService: User " + user.getEmail()
                + " successfully verified.");
        return true;
    }

    // =====================================================
    // HELPER — check if a user still has a pending OTP
    // =====================================================

    /**
     * Returns true if the user has a non-expired, unused OTP token.
     * Used by VerificationController to decide whether to show the
     * "enter code" form or a "code expired / request new one" message.
     *
     * @param user the user to check
     * @return true if an active OTP exists for this user
     */
    @Transactional(readOnly = true)
    public boolean hasActiveOtp(User user) {
        Optional<OtpToken> token = otpTokenRepository.findByUserAndUsedFalse(user);
        return token.isPresent() && token.get().isValid();
    }
}
