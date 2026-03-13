package com.tutorplatform.controller;

import com.tutorplatform.service.TutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HOME CONTROLLER
 * =====================================================
 * Handles the public-facing home page of the platform.
 *
 * @Controller = Spring MVC controller (returns HTML view names)
 * vs @RestController which returns JSON data.
 *
 * WHAT CONTROLLERS DO:
 * 1. Receive HTTP requests (GET, POST, etc.)
 * 2. Call service methods to get/process data
 * 3. Add data to the Model (passed to HTML templates)
 * 4. Return the name of the HTML template to render
 * =====================================================
 */
@Controller
public class HomeController {

    @Autowired
    private TutorService tutorService;

    /**
     * Handle GET requests to "/" and "/home"
     * Shows the public landing page with platform overview.
     *
     * @param model Spring's Model object - we add data here that
     *              becomes available as variables in the HTML template
     * @return "index" → Spring looks for templates/index.html
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Show some approved tutors on the homepage as a preview
        model.addAttribute("featuredTutors", tutorService.getAvailableTutors()
                .stream().limit(3).toList()); // Show only 3 featured tutors

        // Count stats for the homepage hero section
        model.addAttribute("totalTutors", tutorService.countApprovedTutors());

        return "index"; // Renders: src/main/resources/templates/index.html
    }
}
