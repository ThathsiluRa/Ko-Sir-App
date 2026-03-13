package com.tutorplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * USER ENTITY
 * =====================================================
 * The core user model for the entire platform.
 * This single table stores ALL user types (student, tutor, admin).
 * The "role" field determines what they can do.
 *
 * KEY DESIGN DECISION:
 * We implement Spring Security's UserDetails interface directly
 * on this entity. This means Spring Security can load users
 * directly from the database without needing a separate class.
 *
 * DATABASE TABLE: "users" (not "user" — "user" is reserved in H2/SQL)
 *
 * RELATIONSHIPS:
 * - One User → One TutorProfile (if role=TUTOR)
 * - One User → One StudentProfile (if role=STUDENT)
 * =====================================================
 */
@Entity
@Table(name = "users") // Using "users" because "user" is a SQL reserved word
public class User implements UserDetails {

    // =====================================================
    // PRIMARY KEY
    // Auto-incremented ID (1, 2, 3...) for each user.
    // =====================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // BASIC USER INFORMATION
    // =====================================================

    /** Full name displayed throughout the platform */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String fullName;

    /** Email used as the login username - must be unique */
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password (never store plain text!) */
    @Column(nullable = false)
    private String password;

    /** Phone number for contact purposes */
    @Column
    private String phone;

    // =====================================================
    // ROLE - determines what the user can do
    // Stored as a String in the DB (e.g., "STUDENT")
    // =====================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // =====================================================
    // ACCOUNT STATUS FLAGS
    // Used by Spring Security and Admin management
    // =====================================================

    /** Whether the account is active (admin can deactivate accounts) */
    @Column(nullable = false)
    private boolean active = true;

    // =====================================================
    // VERIFICATION FLAGS
    // Track whether the user has verified their contact details.
    // Set to false on registration when requireVerification is true.
    // OtpService marks these true after a successful OTP check.
    // =====================================================

    /**
     * Whether the user's phone number has been verified via OTP SMS.
     * Defaults to true (verified) so that existing / admin-created
     * accounts are not blocked. Set to false only when the platform's
     * requireVerification setting is enabled at registration time.
     */
    @Column(nullable = false)
    private boolean phoneVerified = true;

    /**
     * Whether the user's email address has been verified via OTP email.
     * Same default behaviour as phoneVerified above.
     */
    @Column(nullable = false)
    private boolean emailVerified = true;

    /** When this account was created (auto-set, never updated) */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =====================================================
    // JPA LIFECYCLE HOOKS
    // These methods run automatically before DB operations.
    // =====================================================

    /**
     * @PrePersist runs just before saving a NEW record to the DB.
     * We use it to auto-set the creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =====================================================
    // SPRING SECURITY - UserDetails INTERFACE METHODS
    // Spring Security calls these to check auth status.
    // =====================================================

    /**
     * Returns the user's authorities (roles).
     * Spring Security uses "ROLE_" prefix convention.
     * So our ADMIN role becomes "ROLE_ADMIN" internally.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefix "ROLE_" is required by Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security's "username" = our email address.
     * This is what users type in the login form's username field.
     */
    @Override
    public String getUsername() {
        return this.email; // We use email as the login identifier
    }

    /** Whether the account has expired (we always return true = not expired) */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Whether the account is locked (we use our own "active" flag below) */
    @Override
    public boolean isAccountNonLocked() {
        return this.active; // If admin deactivates account, this returns false
    }

    /** Whether credentials (password) have expired */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Whether the account is enabled */
    @Override
    public boolean isEnabled() {
        return this.active; // Only active accounts can log in
    }

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    /** Default constructor required by JPA */
    public User() {}

    /** Convenience constructor for creating users quickly */
    public User(String fullName, String email, String password, String phone, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.active = true;
    }

    // =====================================================
    // GETTERS AND SETTERS
    // Standard Java bean properties - Spring/JPA need these.
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // getPassword() is required by Spring Security's UserDetails interface
    // It must return the stored (hashed) password for Spring to verify logins
    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Returns true if the user's phone number has been OTP-verified */
    public boolean isPhoneVerified() { return phoneVerified; }
    /** Set to true once the user completes phone OTP verification */
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    /** Returns true if the user's email address has been OTP-verified */
    public boolean isEmailVerified() { return emailVerified; }
    /** Set to true once the user completes email OTP verification */
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // toString for debugging (never include password in logs!)
    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
