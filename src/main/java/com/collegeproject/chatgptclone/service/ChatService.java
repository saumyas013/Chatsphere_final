
package com.collegeproject.chatgptclone.service;

import com.collegeproject.chatgptclone.model.ChatMessage;
import com.collegeproject.chatgptclone.model.QnADocument;
import com.collegeproject.chatgptclone.repository.ChatRepository;
import com.collegeproject.chatgptclone.repository.QnARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling chat operations, now interacting with a MongoDB
 * Q&A knowledge base instead of an external LLM API.
 * Manages chat messages in MongoDB and provides predefined answers.
 */
@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final QnARepository qnaRepository;

    // Constructor injection for ChatRepository and QnARepository
    public ChatService(ChatRepository chatRepository, QnARepository qnaRepository) {
        this.chatRepository = chatRepository;
        this.qnaRepository = qnaRepository;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (qnaRepository.count() == 0) {
            System.out.println("Populating initial Q&A data...");
            qnaRepository.save(new QnADocument("hello", "Hi there! How can I help you today?"));
            qnaRepository.save(new QnADocument("how are you", "I'm just a bot, but I'm functioning perfectly!"));
            qnaRepository.save(new QnADocument("what is your name", "I am a simple chat bot created for a college project."));
            qnaRepository.save(new QnADocument("who created you", "I was created by a talented college student for their project."));
            qnaRepository.save(new QnADocument("what is spring boot", "Spring Boot is an open source Java-based framework used to create a micro Service."));
            qnaRepository.save(new QnADocument("what is mongodb", "MongoDB is a source-available, cross-platform, document-oriented database program."));
            qnaRepository.save(new QnADocument("thank you", "You're welcome! Is there anything else I can assist you with?"));
            qnaRepository.save(new QnADocument("bye", "Goodbye! Have a great day!"));
            System.out.println("Initial Q&A data populated.");
        }
    }

    /**
     * Processes a user message: saves it to MongoDB, queries the Q&A knowledge base,
     * saves the bot's response, and returns the bot's message.
     *
     * @param userId The ID of the currently authenticated user.
     * @param userMessage The message sent by the user.
     * @return The response message found in the Q&A knowledge base or a default message.
     */
    public String sendMessage(String userId, String userMessage) { // NEW: userId parameter
        // 1. Save user message to MongoDB with userId
        ChatMessage userChatMessage = new ChatMessage(userId, "user", userMessage); // Pass userId
        chatRepository.save(userChatMessage);
        System.out.println("User message saved for user " + userId + ": " + userMessage);

        String botResponse;
        Optional<QnADocument> qnaEntry = qnaRepository.findByQuestionIgnoreCase(userMessage.trim());

        if (qnaEntry.isPresent()) {
            botResponse = qnaEntry.get().getAnswer();
            System.out.println("Found Q&A response: " + botResponse);
        } else {
            botResponse = "I'm sorry, I don't have an answer for that in my knowledge base. Please try asking something else.";
            System.out.println("No Q&A response found for: " + userMessage);
        }

        // 2. Save bot's response to MongoDB with userId
        ChatMessage botChatMessage = new ChatMessage(userId, "bot", botResponse); // Pass userId
        chatRepository.save(botChatMessage);
        System.out.println("Bot message saved for user " + userId + ": " + botResponse);

        return botResponse;
    }

    /**
     * Retrieves the chat history for a specific user from MongoDB, ordered by timestamp.
     *
     * @param userId The ID of the user whose chat history is to be retrieved.
     * @return A list of ChatMessage objects for the given user.
     */
    public List<ChatMessage> getChatHistory(String userId) { // NEW: userId parameter
        return chatRepository.findByUserIdOrderByTimestampAsc(userId);
    }
}

