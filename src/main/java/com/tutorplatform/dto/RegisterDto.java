package com.tutorplatform.dto;

import com.tutorplatform.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * REGISTRATION DATA TRANSFER OBJECT (DTO)
 * =====================================================
 * A DTO is a simple container for transferring data between
 * the HTML form and the controller/service layers.
 *
 * WHY USE A DTO INSTEAD OF THE USER ENTITY DIRECTLY?
 * 1. The registration form has fields the User entity doesn't
 *    (like confirmPassword, role selection, parent info)
 * 2. We don't want to expose the User entity directly to forms
 *    (security concern - binding attacks)
 * 3. We can have registration-specific validation rules
 *
 * HOW IT FLOWS:
 *   HTML Form → RegisterDto → AuthController → UserService → User entity saved
 * =====================================================
 */
public class RegisterDto {

    // =====================================================
    // BASIC ACCOUNT INFORMATION
    // =====================================================

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /** Confirm password field - validated in the service layer */
    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    /** Phone number (optional but recommended) */
    private String phone;

    // =====================================================
    // ROLE SELECTION
    // The registration form shows two options: STUDENT or TUTOR.
    // ADMIN accounts are created by other admins only.
    // =====================================================

    @NotNull(message = "Please select whether you are a student or tutor")
    private Role role;

    // =====================================================
    // STUDENT-SPECIFIC FIELDS (only filled if role=STUDENT)
    // =====================================================

    /** Student's current grade/year (e.g., "Form 3", "Grade 7") */
    private String grade;

    /** Name of the student's school */
    private String school;

    /** Parent or guardian name */
    private String parentName;

    /** Parent or guardian phone number */
    private String parentPhone;

    // =====================================================
    // TUTOR-SPECIFIC FIELDS (only filled if role=TUTOR)
    // =====================================================

    /** Subjects the tutor teaches (e.g., "Mathematics, Physics") */
    private String subjects;

    /** Tutor's hourly rate */
    private String hourlyRate;

    /** Where the tutor teaches */
    private String location;

    /** Tutor's educational qualification */
    private String qualification;

    /** Years of experience */
    private Integer experienceYears;

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getSubjects() { return subjects; }
    public void setSubjects(String subjects) { this.subjects = subjects; }

    public String getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(String hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
}
