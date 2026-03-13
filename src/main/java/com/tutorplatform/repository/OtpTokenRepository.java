package com.tutorplatform.repository;

import com.tutorplatform.model.OtpToken;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OTP TOKEN REPOSITORY
 * =====================================================
 * Spring Data JPA repository for the OtpToken entity.
 *
 * Spring Data JPA automatically generates the SQL query
 * for findByUserAndUsedFalse() by parsing the method name:
 *   "findBy"  → SELECT * FROM otp_tokens WHERE ...
 *   "User"    → user_id = :user
 *   "And"     → AND
 *   "Used"    → used = ...
 *   "False"   → false
 *
 * So the generated SQL is roughly:
 *   SELECT * FROM otp_tokens WHERE user_id = ? AND used = false
 *
 * This returns an Optional because a user might not have any
 * pending (unused) token — e.g. they never registered with
 * verification enabled, or they already consumed their token.
 * =====================================================
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Find the most-recently-created, still-unused OTP token for a user.
     *
     * Called by OtpService.validateOtp() to retrieve the active token
     * and compare the user-supplied code against the stored code.
     *
     * Returns Optional.empty() if:
     *   - the user has no token at all
     *   - the user's token has already been marked used = true
     *
     * @param user the user whose pending token we want
     * @return an Optional containing the unused OtpToken, or empty
     */
    Optional<OtpToken> findByUserAndUsedFalse(User user);

    /**
     * Delete any existing (possibly unused) token for a user.
     *
     * Called by OtpService.generateOtp() BEFORE creating a new token,
     * so that a user who requests a fresh OTP invalidates their old one
     * and there is never more than one active token per user.
     *
     * Spring Data generates:
     *   DELETE FROM otp_tokens WHERE user_id = ?
     *
     * @param user the user whose old tokens should be removed
     */
    void deleteByUser(User user);
}
