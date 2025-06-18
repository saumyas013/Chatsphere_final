package com.collegeproject.chatgptclone.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a Question and Answer document stored in MongoDB.
 * This will serve as the knowledge base for the bot.
 * Uses Lombok for boilerplate code reduction.
 */
@Data // Generates getters, setters, toString, equals, and hashCode methods
@Document(collection = "qna_documents") // Specifies the MongoDB collection name for Q&A
public class QnADocument {

    @Id // Marks this field as the primary identifier for the document
    private String id;

    private String question; // The question or query that matches user input
    private String answer;   // The predefined answer for the question

    // Default constructor (required by Spring Data MongoDB)
    public QnADocument() {}

    // Constructor for creating new Q&A entries
    public QnADocument(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}
