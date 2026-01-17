
package com.collegeproject.chatgptclone.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a chat message document stored in MongoDB.
 * Now includes a userId to associate messages with a specific user.
 * Uses Lombok for boilerplate code reduction (getters, setters, constructors).
 */
@Data // Generates getters, setters, toString, equals, and hashCode methods
@Document(collection = "chat_messages") // Specifies the MongoDB collection name
public class ChatMessage {

    @Id // Marks this field as the primary identifier for the document
    private String id;

    private String userId; // NEW: ID of the user this message belongs to
    private String sender; // e.g., "user", "bot"
    private String message;
    private String imageBase64; // NEW: Store image data if present
    private LocalDateTime timestamp; // Timestamp when the message was created

    // Default constructor (required by Spring Data MongoDB)
    public ChatMessage() {
        this.timestamp = LocalDateTime.now(); // Set timestamp on creation
    }

    // Constructor for creating new messages with a userId
    public ChatMessage(String userId, String sender, String message) {
        this(userId, sender, message, null);
    }

    public ChatMessage(String userId, String sender, String message, String imageBase64) {
        this.userId = userId;
        this.sender = sender;
        this.message = message;
        this.imageBase64 = imageBase64;
        this.timestamp = LocalDateTime.now(); // Set timestamp on creation
    }
}
