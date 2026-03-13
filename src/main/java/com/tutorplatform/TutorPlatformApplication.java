package com.tutorplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TUTOR BOOKING PLATFORM - MAIN APPLICATION CLASS
 * =====================================================
 * This is the ENTRY POINT of the entire Spring Boot application.
 * Running this class starts the embedded Tomcat web server
 * and boots up the entire Spring context.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   @Configuration    - This class contains Spring bean definitions
 *   @EnableAutoConfiguration - Let Spring guess/auto-configure beans
 *   @ComponentScan    - Scan this package and sub-packages for @Component,
 *                       @Service, @Repository, @Controller, etc.
 *
 * HOW TO RUN THIS APP:
 * Option 1: Right-click → Run 'TutorPlatformApplication'  (in IntelliJ/Eclipse)
 * Option 2: mvn spring-boot:run                           (in terminal)
 * Option 3: mvn package && java -jar target/*.jar         (build and run)
 *
 * Once running, open: http://localhost:8080
 *
 * DEFAULT ACCOUNTS (created by DataInitializer.java):
 *   Admin:   admin@tutor.com   / admin123
 *   Tutor:   alice@tutor.com   / tutor123
 *   Student: student@tutor.com / student123
 * =====================================================
 */
@SpringBootApplication
public class TutorPlatformApplication {

    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the entire application:
        // 1. Creates the Spring ApplicationContext
        // 2. Auto-configures Spring components
        // 3. Starts the embedded Tomcat server on port 8080
        // 4. Runs CommandLineRunner beans (like our DataInitializer)
        SpringApplication.run(TutorPlatformApplication.class, args);
    }
}
