package com.tutorplatform.model;

import jakarta.persistence.*;

/**
 * PLATFORM SETTINGS ENTITY
 * =====================================================
 * Stores all configurable platform-wide settings in a single
 * database row.  There is always exactly ONE row in this table
 * (id = 1).  The DataInitializer calls
 * PlatformSettingsService.getOrCreateDefault() on startup to
 * guarantee that row exists.
 *
 * SETTINGS GROUPS:
 *
 * 1. SMS settings
 *    - smsProvider    : "TEXTLK" or "TWILIO" (which gateway to use)
 *    - textlkApiToken : Bearer token for the text.lk API
 *    - textlkSenderId : Sender ID shown on recipient's phone (text.lk)
 *    - twilioAccountSid, twilioAuthToken, twilioFromNumber : Twilio creds
 *    - smsEnabled     : master switch — if false, no SMS is sent at all
 *
 * 2. Email / SMTP settings
 *    - smtpHost, smtpPort, smtpUsername, smtpPassword : SMTP server creds
 *    - emailFromAddress, emailFromName : the "From" header on sent emails
 *    - emailEnabled   : master switch — if false, no email is sent
 *
 * 3. Verification policy
 *    - requireVerification : if true, new registrations must complete OTP
 *      verification (SMS + email) before their account is considered active
 *
 * DATABASE TABLE: "platform_settings"
 * =====================================================
 */
@Entity
@Table(name = "platform_settings")
public class PlatformSettings {

    // =====================================================
    // PRIMARY KEY
    // We always use id=1 for the single settings row.
    // GenerationType.IDENTITY still works; we just never
    // insert a second row.
    // =====================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // SMS PROVIDER SELECTION
    // Determines which SMS backend SmsService uses.
    // Accepted values: "TEXTLK" or "TWILIO"
    // =====================================================

    /**
     * Which SMS gateway to use.
     * "TEXTLK"  → text.lk REST API (local Sri Lanka provider)
     * "TWILIO"  → Twilio global SMS API
     */
    @Column(nullable = false)
    private String smsProvider = "TEXTLK";

    // =====================================================
    // TEXT.LK API CREDENTIALS
    // Only used when smsProvider = "TEXTLK"
    // Obtain these from your text.lk account dashboard.
    // =====================================================

    /** Bearer token for authenticating requests to text.lk API */
    @Column
    private String textlkApiToken;

    /**
     * Sender ID shown to the recipient on their phone screen.
     * Must be pre-registered in your text.lk account.
     * Example: "TUTORAPP"
     */
    @Column
    private String textlkSenderId;

    // =====================================================
    // TWILIO API CREDENTIALS
    // Only used when smsProvider = "TWILIO"
    // Obtain from https://console.twilio.com
    // =====================================================

    /** Twilio Account SID — uniquely identifies your Twilio account */
    @Column
    private String twilioAccountSid;

    /** Twilio Auth Token — secret used to authenticate API calls */
    @Column
    private String twilioAuthToken;

    /**
     * The Twilio phone number (or Messaging Service SID) to send FROM.
     * Must be in E.164 format, e.g. "+15017250604"
     */
    @Column
    private String twilioFromNumber;

    // =====================================================
    // SMTP / EMAIL SETTINGS
    // Used by EmailService to send transactional emails.
    // A JavaMailSenderImpl is constructed dynamically from these
    // values so the admin can change them without restarting.
    // =====================================================

    /** SMTP server hostname, e.g. "smtp.gmail.com" or "smtp.sendgrid.net" */
    @Column
    private String smtpHost;

    /**
     * SMTP server port as a String for easy form binding.
     * Common values: "587" (TLS/STARTTLS), "465" (SSL), "25" (unencrypted)
     */
    @Column
    private String smtpPort;

    /** SMTP authentication username (often the same as the from-address) */
    @Column
    private String smtpUsername;

    /**
     * SMTP authentication password / app password.
     * Stored as plain text here for simplicity; in production consider
     * encrypting this column or using environment variables.
     */
    @Column
    private String smtpPassword;

    /**
     * The email address that appears in the "From:" header.
     * Example: "noreply@tutorplatform.com"
     */
    @Column
    private String emailFromAddress;

    /**
     * The display name that appears alongside the from address.
     * Example: "Ko-Sir Tutor Platform"
     */
    @Column
    private String emailFromName;

    // =====================================================
    // MASTER SWITCHES
    // Quickly enable or disable entire channels without
    // deleting credentials.
    // =====================================================

    /**
     * Global SMS enable flag.
     * If false, SmsService.sendSms() is a no-op and logs a warning.
     * Useful for disabling SMS in development/testing environments.
     */
    @Column(nullable = false)
    private boolean smsEnabled = false;

    /**
     * Global email enable flag.
     * If false, EmailService.sendEmail() is a no-op.
     */
    @Column(nullable = false)
    private boolean emailEnabled = false;

    // =====================================================
    // PAYHERE PAYMENT GATEWAY (Sri Lanka)
    // https://www.payhere.lk/
    //
    // HOW PAYHERE WORKS:
    // 1. Your page submits a hidden form to PayHere's server
    // 2. PayHere shows their hosted payment page (card/bank/etc.)
    // 3. After payment, PayHere POSTs a notification to your notify_url
    // 4. And redirects the user to return_url (success) or cancel_url
    //
    // SECURITY — Hash Generation:
    // hash = MD5(merchant_id + order_id + amount + currency + MD5(secret).toUpperCase())
    // This prevents tampering with the amount or order details.
    // The notify_url callback must re-verify this hash before marking paid.
    //
    // Get credentials from: https://www.payhere.lk/merchant/settings
    // =====================================================

    /**
     * PayHere Merchant ID.
     * Found in your PayHere merchant account dashboard.
     * Example: "1234567"
     */
    @Column
    private String payhereMerchantId;

    /**
     * PayHere Merchant Secret.
     * Used to generate the security hash — KEEP THIS CONFIDENTIAL.
     * Never expose this in client-side HTML or JavaScript.
     * Found in: PayHere Dashboard → Integrations → Merchant Secret
     */
    @Column
    private String payhereMerchantSecret;

    /**
     * PayHere environment mode.
     * "SANDBOX" → uses sandbox.payhere.lk for testing (no real charges)
     * "LIVE"    → uses www.payhere.lk for production payments
     */
    @Column(nullable = false)
    private String payhereMode = "SANDBOX";

    /**
     * Master switch for PayHere payments.
     * If false, the demo card form is shown instead.
     * Enable after you have valid merchant credentials.
     */
    @Column(nullable = false)
    private boolean payhereEnabled = false;

    // =====================================================
    // VERIFICATION POLICY
    // =====================================================

    /**
     * If true, newly registered users must verify their phone and email
     * via OTP before they can use the platform.
     * If false, accounts are considered immediately verified.
     */
    @Column(nullable = false)
    private boolean requireVerification = false;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    /** Default constructor required by JPA */
    public PlatformSettings() {}

    // =====================================================
    // GETTERS AND SETTERS
    // Standard Java bean accessors used by JPA, Thymeleaf
    // form binding, and Spring MVC model binding.
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSmsProvider() { return smsProvider; }
    public void setSmsProvider(String smsProvider) { this.smsProvider = smsProvider; }

    public String getTextlkApiToken() { return textlkApiToken; }
    public void setTextlkApiToken(String textlkApiToken) { this.textlkApiToken = textlkApiToken; }

    public String getTextlkSenderId() { return textlkSenderId; }
    public void setTextlkSenderId(String textlkSenderId) { this.textlkSenderId = textlkSenderId; }

    public String getTwilioAccountSid() { return twilioAccountSid; }
    public void setTwilioAccountSid(String twilioAccountSid) { this.twilioAccountSid = twilioAccountSid; }

    public String getTwilioAuthToken() { return twilioAuthToken; }
    public void setTwilioAuthToken(String twilioAuthToken) { this.twilioAuthToken = twilioAuthToken; }

    public String getTwilioFromNumber() { return twilioFromNumber; }
    public void setTwilioFromNumber(String twilioFromNumber) { this.twilioFromNumber = twilioFromNumber; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public String getSmtpPort() { return smtpPort; }
    public void setSmtpPort(String smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public String getEmailFromAddress() { return emailFromAddress; }
    public void setEmailFromAddress(String emailFromAddress) { this.emailFromAddress = emailFromAddress; }

    public String getEmailFromName() { return emailFromName; }
    public void setEmailFromName(String emailFromName) { this.emailFromName = emailFromName; }

    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public boolean isRequireVerification() { return requireVerification; }
    public void setRequireVerification(boolean requireVerification) {
        this.requireVerification = requireVerification;
    }

    public String getPayhereMerchantId() { return payhereMerchantId; }
    public void setPayhereMerchantId(String payhereMerchantId) { this.payhereMerchantId = payhereMerchantId; }

    public String getPayhereMerchantSecret() { return payhereMerchantSecret; }
    public void setPayhereMerchantSecret(String payhereMerchantSecret) { this.payhereMerchantSecret = payhereMerchantSecret; }

    public String getPayhereMode() { return payhereMode; }
    public void setPayhereMode(String payhereMode) { this.payhereMode = payhereMode; }

    public boolean isPayhereEnabled() { return payhereEnabled; }
    public void setPayhereEnabled(boolean payhereEnabled) { this.payhereEnabled = payhereEnabled; }

    @Override
    public String toString() {
        return "PlatformSettings{id=" + id
                + ", smsProvider='" + smsProvider + "'"
                + ", smsEnabled=" + smsEnabled
                + ", emailEnabled=" + emailEnabled
                + ", requireVerification=" + requireVerification + "}";
    }
}
