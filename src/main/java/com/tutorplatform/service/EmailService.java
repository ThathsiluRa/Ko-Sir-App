package com.tutorplatform.service;

import com.tutorplatform.model.PlatformSettings;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * EMAIL SERVICE
 * =====================================================
 * Sends transactional emails (OTP verification, notifications)
 * using JavaMailSenderImpl configured dynamically from the
 * PlatformSettings stored in the database.
 *
 * WHY DYNAMIC CONFIGURATION?
 * Spring Boot's standard approach hard-codes SMTP credentials
 * in application.properties.  Here we build a fresh
 * JavaMailSenderImpl on every call, reading the live database
 * settings.  This means the admin can change SMTP credentials
 * via the admin panel and the change takes effect immediately —
 * no application restart required.
 *
 * MASTER SWITCH:
 *   PlatformSettings.emailEnabled == false → log and return early.
 *   Useful for disabling email in development.
 *
 * SMTP ASSUMPTIONS:
 * The service always enables STARTTLS (port 587 convention).
 * If you need plain SMTP or SSL on port 465 you can extend
 * the settings model with a "smtpTls" flag.
 *
 * ERROR HANDLING:
 * Errors are caught, logged, and NOT re-thrown.  A failed email
 * should not prevent registration from completing — the OTP is
 * also sent via SMS.
 *
 * @Service → Spring-managed singleton bean
 * =====================================================
 */
@Service
public class EmailService {

    /** Used to load current SMTP credentials on every send call */
    @Autowired
    private PlatformSettingsService platformSettingsService;

    // =====================================================
    // PUBLIC API
    // =====================================================

    /**
     * Send a plain-text (and optionally HTML-capable) email.
     *
     * Reads PlatformSettings fresh from the database on each call
     * so credential changes in the admin panel take effect without
     * restarting the application.
     *
     * @param to      recipient email address (e.g. "user@example.com")
     * @param subject email subject line
     * @param body    email body — treated as plain text (UTF-8)
     */
    public void sendEmail(String to, String subject, String body) {

        // Load current settings from DB
        PlatformSettings settings = platformSettingsService.getSettings();

        // Master switch — abort if email sending is disabled
        if (!settings.isEmailEnabled()) {
            System.out.println(">>> EmailService: Email is disabled in platform settings. "
                    + "Skipping email to " + to);
            return;
        }

        // Validate recipient
        if (to == null || to.isBlank()) {
            System.out.println(">>> EmailService: Cannot send email — recipient address is empty.");
            return;
        }

        // Validate SMTP host is configured
        if (settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()) {
            System.out.println(">>> EmailService: SMTP host is not configured. "
                    + "Please fill in the settings panel.");
            return;
        }

        try {
            // Build a JavaMailSenderImpl using the live settings
            JavaMailSenderImpl mailSender = buildMailSender(settings);

            // Create a MIME message (supports proper UTF-8 encoding)
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // MimeMessageHelper simplifies setting headers, from, to, etc.
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,       // false = not multipart (no attachments)
                    "UTF-8"      // character encoding
            );

            // Set the "From" header with optional display name
            String fromAddress = settings.getEmailFromAddress();
            String fromName    = settings.getEmailFromName();

            if (fromName != null && !fromName.isBlank()) {
                // Set "Ko-Sir Tutor Platform <noreply@example.com>" style header
                helper.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
            } else {
                helper.setFrom(fromAddress);
            }

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // false = plain text (not HTML)

            // Send — this is the actual SMTP connection
            mailSender.send(mimeMessage);

            System.out.println(">>> EmailService: Email sent successfully to " + to
                    + " | Subject: " + subject);

        } catch (Exception e) {
            // Log but don't rethrow — email failure must not block registration
            System.out.println(">>> EmailService: Exception sending email to " + to
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    /**
     * Build and configure a JavaMailSenderImpl from the current
     * PlatformSettings.
     *
     * A new instance is created on each call so that settings
     * changes take effect immediately without caching stale values.
     *
     * SMTP PROPERTIES SET:
     *   mail.smtp.auth            = true   (use username/password auth)
     *   mail.smtp.starttls.enable = true   (upgrade connection to TLS)
     *   mail.smtp.timeout         = 5000ms (5 second connection timeout)
     *   mail.smtp.writetimeout    = 5000ms (5 second write timeout)
     *
     * @param settings the platform settings containing SMTP credentials
     * @return a configured, ready-to-use JavaMailSenderImpl
     */
    private JavaMailSenderImpl buildMailSender(PlatformSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // SMTP server connection details
        mailSender.setHost(settings.getSmtpHost());

        // Parse the port from String to int; default to 587 if missing or invalid
        int port = 587;
        if (settings.getSmtpPort() != null && !settings.getSmtpPort().isBlank()) {
            try {
                port = Integer.parseInt(settings.getSmtpPort().trim());
            } catch (NumberFormatException e) {
                System.out.println(">>> EmailService: Invalid SMTP port '"
                        + settings.getSmtpPort() + "', defaulting to 587.");
            }
        }
        mailSender.setPort(port);

        // Authentication credentials
        mailSender.setUsername(settings.getSmtpUsername());
        mailSender.setPassword(settings.getSmtpPassword());

        // Always use UTF-8 for the default encoding
        mailSender.setDefaultEncoding("UTF-8");

        // Additional JavaMail properties
        Properties props = mailSender.getJavaMailProperties();

        // Enable SMTP AUTH — required by virtually all SMTP providers
        props.put("mail.smtp.auth", "true");

        // Enable STARTTLS — upgrades the connection to encrypted TLS
        // Most providers (Gmail, Outlook, SendGrid) require this on port 587
        props.put("mail.smtp.starttls.enable", "true");

        // Connection and write timeouts (milliseconds)
        // Prevent the thread from hanging indefinitely on a slow/unreachable server
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}
