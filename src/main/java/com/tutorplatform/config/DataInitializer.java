package com.tutorplatform.config;

import com.tutorplatform.model.*;
import com.tutorplatform.repository.*;
import com.tutorplatform.service.PlatformSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * DATA INITIALIZER
 * =====================================================
 * Seeds the database with initial data when the app starts.
 * This runs ONCE automatically after the app boots up.
 *
 * WHY WE NEED THIS:
 * - We use H2 in-memory DB, so data is lost on every restart
 * - We need at least one ADMIN account to manage the platform
 * - Sample tutors and students make it easy to test the app
 *
 * HOW IT WORKS:
 * CommandLineRunner is a Spring Boot interface.
 * Its run() method executes after the Spring context is ready.
 * The @Component annotation makes Spring auto-detect and run it.
 *
 * DEFAULT ACCOUNTS CREATED:
 *   Admin:   admin@tutor.com        / admin123
 *   Tutor 1: alice@tutor.com        / tutor123
 *   Tutor 2: bob@tutor.com          / tutor123
 *   Student: student@tutor.com      / student123
 * =====================================================
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * PlatformSettingsService is used to seed the single settings row
     * in the platform_settings table.
     * getOrCreateDefault() is idempotent — safe to call on every boot.
     */
    @Autowired private PlatformSettingsService platformSettingsService;

    /**
     * This method runs automatically when Spring Boot starts.
     * We use it to create default data for testing.
     */
    @Override
    public void run(String... args) throws Exception {
        // =====================================================
        // SEED PLATFORM SETTINGS (always runs, idempotent)
        // This creates the single platform_settings row (id=1)
        // with safe defaults if it doesn't exist yet.
        // Must run even if users already exist, because the
        // settings row is independent of the user table.
        // =====================================================
        platformSettingsService.getOrCreateDefault();

        // Only seed users if the database is empty (prevents duplicate data)
        if (userRepository.count() > 0) {
            System.out.println(">>> DataInitializer: Database already has data, skipping seed.");
            return;
        }

        System.out.println(">>> DataInitializer: Seeding database with sample data...");

        // =====================================================
        // CREATE ADMIN ACCOUNT
        // The platform admin - has full access to everything.
        // =====================================================
        User admin = new User();
        admin.setFullName("Platform Admin");
        admin.setEmail("admin@tutor.com");
        admin.setPassword(passwordEncoder.encode("admin123")); // BCrypt hash
        admin.setPhone("+94-11-000-0001");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        System.out.println(">>> Created Admin: admin@tutor.com / admin123");

        // =====================================================
        // CREATE SAMPLE TUTORS
        // These are pre-approved so they show up in searches.
        // =====================================================

        // Tutor 1: Amali - Mathematics and Physics (Colombo)
        User aliceUser = new User();
        aliceUser.setFullName("Amali Perera");
        aliceUser.setEmail("alice@tutor.com");
        aliceUser.setPassword(passwordEncoder.encode("tutor123"));
        aliceUser.setPhone("+94-777-000-002");
        aliceUser.setRole(Role.TUTOR);
        aliceUser.setActive(true);
        userRepository.save(aliceUser);

        TutorProfile aliceProfile = new TutorProfile();
        aliceProfile.setUser(aliceUser);
        aliceProfile.setSubjects("Mathematics, Physics, Combined Maths");
        aliceProfile.setBio("Passionate Mathematics and Physics tutor with 8 years of experience. " +
                "I specialize in A/L and O/L exam preparation and have helped over 200 students " +
                "achieve A grades in national exams. Home visits available in Colombo.");
        aliceProfile.setQualification("BSc Mathematics & Physics, University of Colombo");
        aliceProfile.setExperienceYears(8);
        aliceProfile.setHourlyRate(new BigDecimal("2500.00")); // LKR 2500/hr
        aliceProfile.setLocation("Colombo - Nugegoda, Maharagama, Colombo 5");
        aliceProfile.setAvailability("Mon-Fri: 4pm-9pm, Sat-Sun: 9am-5pm");
        aliceProfile.setTeachingMode("HOME_VISIT"); // Travels to students' homes
        aliceProfile.setApproved(true); // Pre-approved for demo
        aliceProfile.setAcceptingBookings(true);
        aliceProfile.setAverageRating(4.8);
        aliceProfile.setTotalReviews(24);
        tutorProfileRepository.save(aliceProfile);
        System.out.println(">>> Created Tutor: alice@tutor.com / tutor123");

        // Tutor 2: Kasun - English and Languages (Colombo/Gampaha)
        User bobUser = new User();
        bobUser.setFullName("Kasun Fernando");
        bobUser.setEmail("bob@tutor.com");
        bobUser.setPassword(passwordEncoder.encode("tutor123"));
        bobUser.setPhone("+94-777-000-003");
        bobUser.setRole(Role.TUTOR);
        bobUser.setActive(true);
        userRepository.save(bobUser);

        TutorProfile bobProfile = new TutorProfile();
        bobProfile.setUser(bobUser);
        bobProfile.setSubjects("English, Sinhala, Literature, History");
        bobProfile.setBio("Experienced language and humanities tutor with 12 years of teaching. " +
                "Former teacher at Royal College Colombo. " +
                "I help students master O/L and A/L English and Sinhala with proven techniques.");
        bobProfile.setQualification("MA English Literature, University of Kelaniya");
        bobProfile.setExperienceYears(12);
        bobProfile.setHourlyRate(new BigDecimal("2000.00")); // LKR 2000/hr
        bobProfile.setLocation("Colombo - Kelaniya, Gampaha, Ja-Ela");
        bobProfile.setAvailability("Mon-Sat: 5pm-9pm");
        bobProfile.setTeachingMode("BOTH"); // Home visits OR students come to his place
        bobProfile.setTeachingAddress("78 Kandy Road, Kelaniya (opposite Kelaniya Temple, blue gate)");
        bobProfile.setApproved(true);
        bobProfile.setAcceptingBookings(true);
        bobProfile.setAverageRating(4.6);
        bobProfile.setTotalReviews(18);
        tutorProfileRepository.save(bobProfile);
        System.out.println(">>> Created Tutor: bob@tutor.com / tutor123");

        // Tutor 3: Dilini - Sciences (pending approval - to show admin workflow)
        User carolUser = new User();
        carolUser.setFullName("Dilini Jayawardena");
        carolUser.setEmail("carol@tutor.com");
        carolUser.setPassword(passwordEncoder.encode("tutor123"));
        carolUser.setPhone("+94-777-000-004");
        carolUser.setRole(Role.TUTOR);
        carolUser.setActive(true);
        userRepository.save(carolUser);

        TutorProfile carolProfile = new TutorProfile();
        carolProfile.setUser(carolUser);
        carolProfile.setSubjects("Chemistry, Biology, Science");
        carolProfile.setBio("Science tutor specializing in O/L and A/L exam preparation in Kandy. " +
                "BSc graduate from University of Peradeniya with 3 years of private tutoring experience.");
        carolProfile.setQualification("BSc Biochemistry, University of Peradeniya");
        carolProfile.setExperienceYears(3);
        carolProfile.setHourlyRate(new BigDecimal("1800.00")); // LKR 1800/hr
        carolProfile.setLocation("Kandy - Peradeniya, Katugastota, Kundasale");
        carolProfile.setAvailability("Weekends only: 10am-5pm");
        carolProfile.setTeachingMode("FIXED_LOCATION"); // Students come to her teaching centre
        carolProfile.setTeachingAddress("12 Peradeniya Road, Kandy (3rd floor, Peiris Building, above Cargills)");
        carolProfile.setApproved(false); // Pending admin approval - demo!
        carolProfile.setAcceptingBookings(false);
        carolProfile.setAverageRating(0.0);
        carolProfile.setTotalReviews(0);
        tutorProfileRepository.save(carolProfile);
        System.out.println(">>> Created Tutor (PENDING): carol@tutor.com / tutor123");

        // =====================================================
        // CREATE SAMPLE STUDENT
        // =====================================================
        User studentUser = new User();
        studentUser.setFullName("Nimesh Rajapaksa");
        studentUser.setEmail("student@tutor.com");
        studentUser.setPassword(passwordEncoder.encode("student123"));
        studentUser.setPhone("+94-777-000-005");
        studentUser.setRole(Role.STUDENT);
        studentUser.setActive(true);
        userRepository.save(studentUser);

        StudentProfile studentProfile = new StudentProfile();
        studentProfile.setUser(studentUser);
        studentProfile.setGrade("Grade 11 (O/L)");
        studentProfile.setSchool("Nalanda College, Colombo");
        studentProfile.setSubjectsNeeded("Mathematics, Physics");
        studentProfile.setAddress("15/3 Kotte Road, Rajagiriya");
        studentProfile.setCity("Colombo");
        studentProfile.setParentName("Pradeep Rajapaksa");
        studentProfile.setParentPhone("+94-777-000-006");
        studentProfileRepository.save(studentProfile);
        System.out.println(">>> Created Student: student@tutor.com / student123");

        System.out.println(">>> DataInitializer: Done! Sample data created successfully.");
        System.out.println(">>> ============================================");
        System.out.println(">>> LOGIN CREDENTIALS:");
        System.out.println(">>>   Admin:   admin@tutor.com / admin123");
        System.out.println(">>>   Tutor:   alice@tutor.com / tutor123");
        System.out.println(">>>   Student: student@tutor.com / student123");
        System.out.println(">>> ============================================");
    }
}
