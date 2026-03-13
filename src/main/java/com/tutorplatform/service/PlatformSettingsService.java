package com.tutorplatform.service;

import com.tutorplatform.model.PlatformSettings;
import com.tutorplatform.repository.PlatformSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLATFORM SETTINGS SERVICE
 * =====================================================
 * Business logic layer for reading and writing the platform's
 * global configuration (PlatformSettings).
 *
 * RESPONSIBILITIES:
 * 1. Load the single settings row from the database.
 * 2. Save / update that row when the admin submits the
 *    /admin/settings form.
 * 3. Provide getOrCreateDefault() so that the application
 *    always has a valid settings object — even on first boot
 *    before the admin has configured anything.
 *
 * SINGLE-ROW TABLE CONVENTION:
 * We always use id = 1 for the settings row.  If the row
 * doesn't exist yet, getOrCreateDefault() inserts it with
 * safe defaults (all switches off, provider = "TEXTLK").
 *
 * @Service   → marks this as a Spring-managed service bean
 * @Transactional → wraps every method in a DB transaction;
 *              rolls back automatically on RuntimeException
 * =====================================================
 */
@Service
@Transactional
public class PlatformSettingsService {

    /** The JPA repository used to talk to the platform_settings table */
    @Autowired
    private PlatformSettingsRepository platformSettingsRepository;

    // =====================================================
    // READ
    // =====================================================

    /**
     * Load the current platform settings.
     *
     * Uses id = 1 (our convention for the single settings row).
     * If the row does not exist yet, returns a default object
     * WITHOUT saving it — use getOrCreateDefault() when you also
     * need the row persisted.
     *
     * @return the current PlatformSettings; never null
     */
    @Transactional(readOnly = true)
    public PlatformSettings getSettings() {
        return platformSettingsRepository.findById(1L)
                .orElse(new PlatformSettings()); // return a fresh default if not yet seeded
    }

    // =====================================================
    // READ-OR-CREATE (used by DataInitializer on boot)
    // =====================================================

    /**
     * Load the settings row, or create and persist a default one if
     * it does not exist yet.
     *
     * Called by DataInitializer.run() to ensure the row is always
     * present in the database.  Also safe to call multiple times —
     * it only inserts if the row is missing.
     *
     * DEFAULT VALUES applied to the fresh row:
     *   smsProvider        = "TEXTLK"
     *   smsEnabled         = false  (off until admin configures credentials)
     *   emailEnabled       = false  (off until admin configures SMTP)
     *   requireVerification= false  (verification off by default)
     *   All credential fields = null (must be filled in via admin panel)
     *
     * @return the existing or newly created PlatformSettings
     */
    public PlatformSettings getOrCreateDefault() {
        return platformSettingsRepository.findById(1L)
                .orElseGet(() -> {
                    // No settings row yet — create one with safe defaults
                    PlatformSettings defaults = new PlatformSettings();
                    defaults.setSmsProvider("TEXTLK");
                    defaults.setSmsEnabled(false);
                    defaults.setEmailEnabled(false);
                    defaults.setRequireVerification(false);
                    // Credential fields are intentionally left null;
                    // admin must fill them in via /admin/settings
                    PlatformSettings saved = platformSettingsRepository.save(defaults);
                    System.out.println(">>> PlatformSettingsService: Created default settings row (id=1).");
                    return saved;
                });
    }

    // =====================================================
    // WRITE
    // =====================================================

    /**
     * Persist updated platform settings.
     *
     * Called from AdminController.saveSettings() when the admin
     * submits the /admin/settings form.  The passed object must
     * have id = 1 so JPA performs an UPDATE rather than an INSERT.
     *
     * @param settings the updated settings object to save
     * @return the saved (managed) PlatformSettings entity
     */
    public PlatformSettings saveSettings(PlatformSettings settings) {
        // Force id = 1 to guarantee we always update the single row,
        // never accidentally insert a second one.
        settings.setId(1L);
        PlatformSettings saved = platformSettingsRepository.save(settings);
        System.out.println(">>> PlatformSettingsService: Settings updated: " + saved);
        return saved;
    }
}
