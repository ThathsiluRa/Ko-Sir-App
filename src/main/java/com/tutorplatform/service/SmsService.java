package com.tutorplatform.service;

import com.tutorplatform.model.PlatformSettings;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * SMS SERVICE
 * =====================================================
 * Sends SMS messages using either the text.lk REST API or
 * the Twilio API, depending on the platform's current settings.
 *
 * PROVIDER SELECTION:
 *   PlatformSettings.smsProvider == "TEXTLK"  → text.lk API
 *   PlatformSettings.smsProvider == "TWILIO"  → Twilio SDK
 *
 * MASTER SWITCH:
 *   PlatformSettings.smsEnabled == false → log a warning and return
 *   immediately without hitting any external API.
 *
 * TEXT.LK API DETAILS:
 *   Endpoint: POST https://app.text.lk/api/v3/sms/send
 *   Auth    : Bearer token in Authorization header
 *   Body    : JSON with recipient, sender_id, type, message
 *
 * TWILIO API DETAILS:
 *   Uses the official Twilio Java SDK.
 *   Credentials (accountSid + authToken) must be stored in
 *   PlatformSettings.  The SDK is initialised fresh on every
 *   call so settings changes take effect immediately without
 *   restarting the application.
 *
 * ERROR HANDLING:
 *   Errors are caught and logged but NOT re-thrown, so a failed
 *   SMS does not prevent the user from completing registration.
 *   This is intentional — SMS delivery is best-effort.
 *
 * @Service → Spring-managed singleton bean
 * =====================================================
 */
@Service
public class SmsService {

    /** Used to load current provider credentials on every send call */
    @Autowired
    private PlatformSettingsService platformSettingsService;

    /**
     * Java 11+ built-in HTTP client for the text.lk REST call.
     * Shared instance — HttpClient is thread-safe.
     */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // =====================================================
    // PUBLIC API
    // =====================================================

    /**
     * Send an SMS message to the given phone number.
     *
     * This method reads live settings on every invocation so that
     * changes made by the admin in the settings panel take effect
     * without requiring an application restart.
     *
     * @param phoneNumber destination phone number in E.164 format
     *                    (e.g. "+94771234567" or "+254700000001")
     * @param message     the text body of the SMS (keep under 160 chars
     *                    to avoid multi-part billing)
     */
    public void sendSms(String phoneNumber, String message) {

        // Load the current platform settings from the database
        PlatformSettings settings = platformSettingsService.getSettings();

        // Master switch — abort silently if SMS is disabled
        if (!settings.isSmsEnabled()) {
            System.out.println(">>> SmsService: SMS is disabled in platform settings. "
                    + "Skipping SMS to " + phoneNumber);
            return;
        }

        // Validate that a phone number was provided
        if (phoneNumber == null || phoneNumber.isBlank()) {
            System.out.println(">>> SmsService: Cannot send SMS — phone number is empty.");
            return;
        }

        // Route to the configured provider
        String provider = settings.getSmsProvider();
        if ("TWILIO".equalsIgnoreCase(provider)) {
            sendViaTwilio(phoneNumber, message, settings);
        } else {
            // Default to text.lk for any other value (including "TEXTLK")
            sendViaTextLk(phoneNumber, message, settings);
        }
    }

    // =====================================================
    // TEXT.LK IMPLEMENTATION
    // =====================================================

    /**
     * Send SMS using the text.lk REST API (Sri Lanka SMS gateway).
     *
     * API specification:
     *   Method : POST
     *   URL    : https://app.text.lk/api/v3/sms/send
     *   Headers: Authorization: Bearer <token>
     *            Content-Type: application/json
     *            Accept: application/json
     *   Body   : {
     *               "recipient": "<phone>",
     *               "sender_id": "<senderId>",
     *               "type": "plain",
     *               "message": "<message>"
     *            }
     *
     * A 200 response indicates the request was accepted.
     * Non-200 responses are logged as errors.
     *
     * @param phoneNumber E.164 phone number of the recipient
     * @param message     SMS text body
     * @param settings    current platform settings (contains credentials)
     */
    private void sendViaTextLk(String phoneNumber, String message, PlatformSettings settings) {
        try {
            // Validate credentials are configured
            String apiToken = settings.getTextlkApiToken();
            String senderId = settings.getTextlkSenderId();

            if (apiToken == null || apiToken.isBlank()) {
                System.out.println(">>> SmsService [text.lk]: API token is not configured. "
                        + "Please fill in the settings panel.");
                return;
            }
            if (senderId == null || senderId.isBlank()) {
                System.out.println(">>> SmsService [text.lk]: Sender ID is not configured.");
                return;
            }

            // Build the JSON request body manually (no Jackson dependency required here)
            // We sanitise the message to escape double quotes and prevent JSON injection.
            String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
            String safePhone   = phoneNumber.replace("\"", "");
            String safeSender  = senderId.replace("\"", "");

            String jsonBody = "{"
                    + "\"recipient\":\"" + safePhone + "\","
                    + "\"sender_id\":\"" + safeSender + "\","
                    + "\"type\":\"plain\","
                    + "\"message\":\"" + safeMessage + "\""
                    + "}";

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://app.text.lk/api/v3/sms/send"))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Send the request and read the response
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println(">>> SmsService [text.lk]: SMS sent successfully to "
                        + phoneNumber + ". Response: " + response.body());
            } else {
                System.out.println(">>> SmsService [text.lk]: SMS send failed. HTTP "
                        + response.statusCode() + " — " + response.body());
            }

        } catch (Exception e) {
            // Log but don't rethrow — a failed SMS should not block registration
            System.out.println(">>> SmsService [text.lk]: Exception sending SMS to "
                    + phoneNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================================
    // TWILIO IMPLEMENTATION
    // =====================================================

    /**
     * Send SMS using the Twilio Java SDK.
     *
     * The Twilio client is initialised with the credentials stored
     * in PlatformSettings so that admin changes take effect without
     * a restart.
     *
     * Twilio requires:
     *   accountSid   → uniquely identifies the Twilio account
     *   authToken    → secret for authentication
     *   fromNumber   → the Twilio phone number or Messaging Service SID
     *
     * @param phoneNumber E.164 destination number (e.g. "+94771234567")
     * @param message     SMS body text
     * @param settings    platform settings containing Twilio credentials
     */
    private void sendViaTwilio(String phoneNumber, String message, PlatformSettings settings) {
        try {
            // Validate Twilio credentials are present
            String accountSid = settings.getTwilioAccountSid();
            String authToken  = settings.getTwilioAuthToken();
            String fromNumber = settings.getTwilioFromNumber();

            if (accountSid == null || accountSid.isBlank()) {
                System.out.println(">>> SmsService [Twilio]: Account SID is not configured.");
                return;
            }
            if (authToken == null || authToken.isBlank()) {
                System.out.println(">>> SmsService [Twilio]: Auth Token is not configured.");
                return;
            }
            if (fromNumber == null || fromNumber.isBlank()) {
                System.out.println(">>> SmsService [Twilio]: From Number is not configured.");
                return;
            }

            // Initialise (or re-initialise) the Twilio SDK with current credentials.
            // Twilio.init() is idempotent — safe to call on every send.
            Twilio.init(accountSid, authToken);

            // Create and send the message using Twilio's fluent builder API
            Message twilioMessage = Message.creator(
                            new PhoneNumber(phoneNumber),  // "To" phone number
                            new PhoneNumber(fromNumber),   // "From" Twilio number
                            message                        // SMS body
                    )
                    .create(); // Executes the API call

            System.out.println(">>> SmsService [Twilio]: SMS sent. SID = "
                    + twilioMessage.getSid() + ", Status = " + twilioMessage.getStatus());

        } catch (Exception e) {
            // Log but don't rethrow — SMS failure must not block user registration
            System.out.println(">>> SmsService [Twilio]: Exception sending SMS to "
                    + phoneNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
