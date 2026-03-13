package com.tutorplatform.repository;

import com.tutorplatform.model.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * PLATFORM SETTINGS REPOSITORY
 * =====================================================
 * Spring Data JPA repository for the PlatformSettings entity.
 *
 * Because there is always exactly ONE settings row (id = 1),
 * we only need the standard JpaRepository methods:
 *
 *   findById(1L)   → load the settings row
 *   save(settings) → create or update the settings row
 *
 * PlatformSettingsService wraps these calls and adds the
 * "getOrCreateDefault()" convenience method so the rest of
 * the application never has to handle an empty Optional.
 *
 * Spring Data JPA auto-generates the SQL at runtime — no
 * @Query annotations are needed for this simple repository.
 *
 * @Repository is optional (JpaRepository already marks it
 * as a Spring bean), but included here for clarity.
 * =====================================================
 */
@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Long> {
    // All required operations are provided by JpaRepository:
    //   save(PlatformSettings)          → INSERT or UPDATE
    //   findById(Long)                  → SELECT by PK
    //   findAll()                       → SELECT all (only 1 row)
    //   deleteById(Long)                → DELETE (rarely needed)
    //
    // No custom query methods required for this entity.
}
