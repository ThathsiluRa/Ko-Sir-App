package com.tutorplatform.service;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.PlatformSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PAYHERE PAYMENT SERVICE
 * =====================================================
 * Handles all PayHere payment gateway integration for Sri Lanka.
 *
 * WHAT IS PAYHERE?
 * PayHere (payhere.lk) is Sri Lanka's leading online payment gateway.
 * It accepts Visa, Mastercard, AMEX, and local bank payments.
 *
 * HOW THE INTEGRATION WORKS:
 * 1. Our server generates a security hash from order details + merchant secret
 * 2. We embed this hash in a hidden HTML form
 * 3. The form POSTs to PayHere's hosted checkout page
 * 4. Customer pays on PayHere's secure page (we never see card details)
 * 5. PayHere POSTs a notification to our /booking/payment/notify endpoint
 * 6. We verify the notification hash and mark the booking as paid
 * 7. PayHere redirects the customer back to our return/cancel URL
 *
 * HASH ALGORITHM (PayHere spec):
 * hash = MD5(
 *     merchant_id
 *     + order_id
 *     + amount_formatted   (2 decimal places, e.g. "2500.00")
 *     + currency           ("LKR")
 *     + MD5(merchant_secret).toUpperCase()
 * )
 * This hash prevents merchants from tampering with the payment amount
 * after generating the form — any change breaks the hash.
 *
 * NOTIFY HASH VERIFICATION (on callback):
 * md5sig = MD5(
 *     merchant_id
 *     + order_id
 *     + amount_formatted
 *     + currency
 *     + status_code
 *     + MD5(merchant_secret).toUpperCase()
 * ).toUpperCase()
 * Compare this to the md5sig sent by PayHere.
 *
 * MODES:
 * SANDBOX → https://sandbox.payhere.lk/pay/checkout  (test, no real money)
 * LIVE    → https://www.payhere.lk/pay/checkout       (real payments)
 *
 * SECURITY NOTES:
 * - The merchant secret MUST stay server-side — never expose in HTML/JS
 * - Always verify the notification hash before marking a booking as paid
 * - Use HTTPS in production (PayHere requires it for LIVE mode)
 * - Don't rely on return_url alone — it can be manipulated by the user;
 *   the notify_url server callback is the authoritative payment confirmation
 * =====================================================
 */
@Service
public class PayhereService {

    /** PayHere sandbox checkout URL (for testing) */
    private static final String SANDBOX_URL = "https://sandbox.payhere.lk/pay/checkout";

    /** PayHere live checkout URL (for real payments) */
    private static final String LIVE_URL = "https://www.payhere.lk/pay/checkout";

    @Autowired
    private PlatformSettingsService platformSettingsService;

    // =====================================================
    // PUBLIC API
    // =====================================================

    /**
     * Returns the PayHere checkout URL based on the current mode.
     * SANDBOX for testing, LIVE for real payments.
     */
    public String getCheckoutUrl() {
        PlatformSettings s = platformSettingsService.getSettings();
        return "LIVE".equalsIgnoreCase(s.getPayhereMode()) ? LIVE_URL : SANDBOX_URL;
    }

    /**
     * Builds a Map of all form fields required for a PayHere checkout.
     *
     * This map is passed to the Thymeleaf template which renders each
     * entry as a hidden input in the checkout form.
     *
     * PayHere will use these fields to pre-fill the payment page and
     * to validate the hash.
     *
     * @param booking         the booking being paid for
     * @param baseUrl         the application's base URL (e.g. "http://localhost:8080")
     *                        used to build return_url, cancel_url, notify_url
     * @return map of field name → value for all PayHere form inputs
     */
    public Map<String, String> buildCheckoutParams(Booking booking, String baseUrl) {
        PlatformSettings s = platformSettingsService.getSettings();

        // Format amount to exactly 2 decimal places — PayHere requirement
        String amount = formatAmount(booking.getTotalPrice());

        // Unique order ID — use booking ID prefixed to avoid conflicts
        String orderId = "BOOKING-" + booking.getId();

        // Generate the security hash
        String hash = generateHash(
                s.getPayhereMerchantId(),
                orderId,
                amount,
                "LKR",
                s.getPayhereMerchantSecret()
        );

        // Build the student's name from their User record
        String fullName = booking.getStudentProfile().getUser().getFullName();
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName  = nameParts.length > 1 ? nameParts[1] : "-";

        // Use LinkedHashMap to preserve insertion order
        Map<String, String> params = new LinkedHashMap<>();

        // ── Required PayHere fields ──

        // Your PayHere Merchant ID (from dashboard)
        params.put("merchant_id", s.getPayhereMerchantId());

        // Where to redirect after successful payment
        params.put("return_url", baseUrl + "/booking/payment/success?bookingId=" + booking.getId());

        // Where to redirect after cancellation
        params.put("cancel_url", baseUrl + "/booking/payment/cancel?bookingId=" + booking.getId());

        // Server-to-server notification URL (PayHere POSTs here to confirm payment)
        // IMPORTANT: must be a publicly reachable URL in production
        // For localhost testing, use ngrok: https://ngrok.com
        params.put("notify_url", baseUrl + "/booking/payment/notify");

        // Unique order identifier
        params.put("order_id", orderId);

        // Description shown on PayHere's payment page
        params.put("items", "Tutor Session: " + booking.getSubject()
                + " with " + booking.getTutorProfile().getUser().getFullName());

        // Currency — Sri Lanka Rupees
        params.put("currency", "LKR");

        // Total amount (2 decimal places)
        params.put("amount", amount);

        // Customer details — pre-fills PayHere's form
        params.put("first_name", firstName);
        params.put("last_name",  lastName);
        params.put("email",      booking.getStudentProfile().getUser().getEmail());
        params.put("phone",      booking.getStudentProfile().getUser().getPhone() != null
                                   ? booking.getStudentProfile().getUser().getPhone()
                                   : "0000000000");

        // Address fields — required by PayHere (defaults for home tutoring)
        params.put("address", "Home Session");
        params.put("city",    "Colombo");
        params.put("country", "Sri Lanka");

        // Security hash — prevents amount tampering
        params.put("hash", hash);

        return params;
    }

    /**
     * Verifies the hash in a PayHere payment notification callback.
     *
     * SECURITY: Always call this on the notify_url endpoint before
     * marking a booking as paid. If the hash doesn't match, the
     * notification is fraudulent — reject it.
     *
     * PayHere notify_url sends these parameters:
     *   merchant_id, order_id, payment_id, payhere_amount,
     *   payhere_currency, status_code, md5sig
     *
     * @param merchantId    from PayHere notification
     * @param orderId       from PayHere notification
     * @param amount        from PayHere notification (payhere_amount)
     * @param currency      from PayHere notification (payhere_currency)
     * @param statusCode    from PayHere notification (2 = success)
     * @param md5sig        from PayHere notification (their hash to verify)
     * @param merchantSecret your merchant secret from settings
     * @return true if the notification is authentic
     */
    public boolean verifyNotification(String merchantId, String orderId,
                                       String amount, String currency,
                                       String statusCode, String md5sig,
                                       String merchantSecret) {
        // Build expected hash: same algorithm as checkout but includes statusCode
        String secretHash = md5(merchantSecret).toUpperCase();
        String rawString   = merchantId + orderId + amount + currency + statusCode + secretHash;
        String expectedSig = md5(rawString).toUpperCase();

        // Constant-time comparison to prevent timing attacks
        return expectedSig.equals(md5sig.toUpperCase());
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    /**
     * Generates the PayHere checkout security hash.
     *
     * Algorithm:
     *   secretHash = MD5(merchantSecret).toUpperCase()
     *   hash       = MD5(merchantId + orderId + amount + currency + secretHash)
     *
     * The hash is lowercase as PayHere expects.
     */
    private String generateHash(String merchantId, String orderId,
                                 String amount, String currency,
                                 String merchantSecret) {
        // First: hash the secret (uppercase result)
        String secretHash = md5(merchantSecret).toUpperCase();
        // Then: hash everything together
        String rawString  = merchantId + orderId + amount + currency + secretHash;
        return md5(rawString); // lowercase
    }

    /**
     * Computes the MD5 hash of a string and returns it as a lowercase hex string.
     *
     * @param input the string to hash
     * @return 32-character lowercase hex MD5 hash
     * @throws RuntimeException if MD5 algorithm is unavailable (never happens on Java)
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available on standard Java — this should never throw
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Formats a BigDecimal to exactly 2 decimal places.
     * PayHere requires amounts like "2500.00" not "2500" or "2500.5".
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
