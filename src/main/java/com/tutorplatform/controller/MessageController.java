package com.tutorplatform.controller;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.BookingStatus;
import com.tutorplatform.model.Message;
import com.tutorplatform.model.User;
import com.tutorplatform.repository.MessageRepository;
import com.tutorplatform.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * MESSAGE CONTROLLER
 * =====================================================
 * Handles in-app messaging between a student and tutor
 * within the context of a specific booking.
 *
 * Chat is only available once payment has been made
 * (booking status != PENDING_PAYMENT) so both parties
 * have a confirmed interest in the session.
 *
 * URL pattern: /booking/{bookingId}/chat
 * =====================================================
 */
@Controller
public class MessageController {

    @Autowired private BookingService bookingService;
    @Autowired private MessageRepository messageRepository;

    // =====================================================
    // SHOW CHAT PAGE
    // =====================================================

    @GetMapping("/booking/{bookingId}/chat")
    public String showChat(@PathVariable Long bookingId,
                           @AuthenticationPrincipal User currentUser,
                           Model model) {

        Booking booking = bookingService.findById(bookingId);

        // Security: only the student or tutor of this booking can chat
        if (!isParticipant(booking, currentUser)) {
            return "redirect:/";
        }

        // Chat only available after payment (not on PENDING_PAYMENT bookings)
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            return "redirect:/student/bookings";
        }

        // Load all messages and mark unread ones as read for current user
        List<Message> messages = messageRepository.findByBookingOrderBySentAtAsc(booking);
        messageRepository.markAllReadInBooking(booking, currentUser);

        // Determine the other participant's name for the chat header
        boolean isTutor = booking.getTutorProfile().getUser().getId().equals(currentUser.getId());
        String otherPartyName = isTutor
                ? booking.getStudentProfile().getUser().getFullName()
                : booking.getTutorProfile().getUser().getFullName();

        model.addAttribute("booking", booking);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherPartyName", otherPartyName);
        model.addAttribute("isTutor", isTutor);

        return "booking/chat";
    }

    // =====================================================
    // SEND A MESSAGE
    // =====================================================

    @PostMapping("/booking/{bookingId}/chat/send")
    public String sendMessage(@PathVariable Long bookingId,
                              @AuthenticationPrincipal User currentUser,
                              @RequestParam String content,
                              RedirectAttributes redirectAttrs) {

        Booking booking = bookingService.findById(bookingId);

        if (!isParticipant(booking, currentUser)) {
            return "redirect:/";
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            return "redirect:/student/bookings";
        }

        String trimmed = content != null ? content.trim() : "";
        if (trimmed.isEmpty() || trimmed.length() > 2000) {
            redirectAttrs.addFlashAttribute("errorMessage", "Message must be between 1 and 2000 characters.");
            return "redirect:/booking/" + bookingId + "/chat";
        }

        Message message = new Message();
        message.setBooking(booking);
        message.setSender(currentUser);
        message.setContent(trimmed);
        messageRepository.save(message);

        return "redirect:/booking/" + bookingId + "/chat";
    }

    // =====================================================
    // HELPER
    // =====================================================

    private boolean isParticipant(Booking booking, User user) {
        return booking.getStudentProfile().getUser().getId().equals(user.getId())
                || booking.getTutorProfile().getUser().getId().equals(user.getId());
    }
}
