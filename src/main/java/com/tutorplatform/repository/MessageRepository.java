package com.tutorplatform.repository;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.Message;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** All messages for a booking, oldest first */
    List<Message> findByBookingOrderBySentAtAsc(Booking booking);

    /** Count unread messages for a recipient in a booking */
    long countByBookingAndReadFalseAndSenderNot(Booking booking, User sender);

    /** Count ALL unread messages across all bookings where the user is a participant but NOT the sender */
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.read = false AND m.sender <> :user " +
           "AND (m.booking.studentProfile.user = :user OR m.booking.tutorProfile.user = :user)")
    long countUnreadForUser(User user);

    /** Mark all messages in a booking as read (for a specific recipient) */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.read = true " +
           "WHERE m.booking = :booking AND m.sender <> :reader")
    void markAllReadInBooking(Booking booking, User reader);
}
