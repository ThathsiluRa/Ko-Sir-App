package com.tutorplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MESSAGE ENTITY
 * =====================================================
 * Represents a chat message between a student and tutor
 * within the context of a specific booking.
 *
 * Each message belongs to one booking (conversation thread).
 * Messages are always sent by one user (sender) and
 * visible to both participants of that booking.
 *
 * DATABASE TABLE: "messages"
 * =====================================================
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The booking this message belongs to.
     * Chat is always in context of a specific booking.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * The user who sent this message (student or tutor).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The message content.
     */
    @Column(nullable = false, length = 2000)
    private String content;

    /**
     * When the message was sent.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    /**
     * Whether the recipient has read this message.
     */
    @Column(nullable = false)
    private boolean read = false;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
