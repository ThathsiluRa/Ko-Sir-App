package com.tutorplatform.config;

import com.tutorplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SPRING SECURITY CONFIGURATION
 * =====================================================
 * This is the most important security class in the application.
 * It controls:
 * 1. Which URLs require authentication (login)
 * 2. Which URLs are restricted to specific roles (STUDENT, TUTOR, ADMIN)
 * 3. How the login page works
 * 4. How logout works
 * 5. Password encoding strategy
 *
 * HOW SPRING SECURITY WORKS:
 * Every HTTP request passes through a "security filter chain".
 * This config tells the filter chain what to do with each request.
 *
 * @Configuration = This class contains Spring bean definitions
 * @EnableWebSecurity = Activates Spring Security for web requests
 * =====================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

    // PasswordEncoder is defined in PasswordConfig.java (separate class to avoid circular dependency)
    // Spring injects it here automatically since it's a @Bean in the same context
    @Autowired
    private PasswordEncoder passwordEncoder;

    // =====================================================
    // AUTHENTICATION PROVIDER
    // Tells Spring Security HOW to authenticate users:
    // - Use our UserService.loadUserByUsername() to find them
    // - Use BCrypt to check their password
    // =====================================================
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder); // inject the bean, don't call local method
        return provider;
    }

    // =====================================================
    // SECURITY FILTER CHAIN
    // The main security configuration - defines URL access rules.
    //
    // Rule order matters! Spring Security checks rules TOP-DOWN
    // and uses the FIRST matching rule.
    // =====================================================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // =====================================================
            // AUTHORIZATION RULES
            // Defines which URLs need authentication and what role.
            // =====================================================
            .authorizeHttpRequests(auth -> auth

                // PUBLIC PAGES - anyone can access these (no login required)
                .requestMatchers(
                    "/",            // Home page
                    "/home",        // Alternative home
                    "/register",    // Registration form
                    "/login",       // Login form
                    "/css/**",      // Static CSS files
                    "/js/**",       // Static JavaScript files
                    "/images/**",   // Images
                    "/h2-console/**", // H2 database console (development only)
                    // =====================================================
                    // VERIFICATION PAGES
                    // /verify and /verify/** must be accessible to users
                    // who just registered but are not yet "fully" verified.
                    // Without this, Spring Security would redirect them to
                    // /login in an infinite loop before they can enter their OTP.
                    // Authenticated-but-unverified users CAN reach /verify
                    // because it is in the permitAll() list; fully-verified users
                    // are redirected away by VerificationController itself.
                    // =====================================================
                    "/verify",      // OTP entry form (GET)
                    "/verify/**",   // OTP submission and resend (POST + ?resend=true)
                    // PayHere calls this from their servers — not a logged-in user
                    "/booking/payment/notify"
                ).permitAll()

                // ADMIN PAGES - only users with ADMIN role can access
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // TUTOR PAGES - only users with TUTOR role
                .requestMatchers("/tutor/**").hasRole("TUTOR")

                // STUDENT PAGES - only users with STUDENT role
                .requestMatchers("/student/**").hasRole("STUDENT")

                // BOOKING PAGES - students and tutors can access
                .requestMatchers("/booking/**").hasAnyRole("STUDENT", "TUTOR", "ADMIN")

                // REVIEW PAGES - any authenticated user
                .requestMatchers("/review/**").hasAnyRole("STUDENT", "ADMIN")

                // EVERYTHING ELSE requires authentication (just logged in, any role)
                .anyRequest().authenticated()
            )

            // =====================================================
            // LOGIN FORM CONFIGURATION
            // Spring Security handles the login process for us.
            // We just need to tell it where the login page is
            // and which fields to look for.
            // =====================================================
            .formLogin(form -> form
                .loginPage("/login")                    // Our custom login page URL
                .loginProcessingUrl("/login")           // Where the form POSTs to
                .usernameParameter("email")             // Form field name for email
                .passwordParameter("password")          // Form field name for password
                .successHandler((request, response, authentication) -> {
                    // After successful login, redirect based on user's role
                    String role = authentication.getAuthorities().iterator()
                            .next().getAuthority();

                    if (role.equals("ROLE_ADMIN")) {
                        response.sendRedirect("/admin/dashboard");
                    } else if (role.equals("ROLE_TUTOR")) {
                        response.sendRedirect("/tutor/dashboard");
                    } else {
                        response.sendRedirect("/student/dashboard");
                    }
                })
                .failureUrl("/login?error=true")        // Redirect here if login fails
                .permitAll()
            )

            // =====================================================
            // LOGOUT CONFIGURATION
            // Spring Security handles logout automatically.
            // =====================================================
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // Logout URL
                .logoutSuccessUrl("/login?logout=true")  // Redirect after logout
                .invalidateHttpSession(true)             // Clear the session
                .deleteCookies("JSESSIONID")             // Remove session cookie
                .permitAll()
            )

            // =====================================================
            // H2 CONSOLE CONFIGURATION
            // H2's web console uses iframes, which Spring Security
            // blocks by default. We need to allow same-origin frames.
            // REMOVE THIS IN PRODUCTION!
            // =====================================================
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // =====================================================
            // CSRF CONFIGURATION
            // CSRF protection is a security feature that prevents
            // cross-site request forgery attacks.
            // We disable it for:
            //   - H2 console (development convenience only — remove in prod)
            //   - /booking/payment/notify — PayHere is an external server;
            //     it cannot include our CSRF token in its server-to-server POST.
            //     SECURITY: The PayHere notify endpoint uses its own hash-based
            //     authentication (md5sig verification) instead of CSRF.
            // =====================================================
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/booking/payment/notify")
            );

        return http.build();
    }
}
