//
//package com.collegeproject.chatgptclone.service;
//
//import com.collegeproject.chatgptclone.model.ChatMessage;
//import com.collegeproject.chatgptclone.model.QnADocument;
//import com.collegeproject.chatgptclone.repository.ChatRepository;
//import com.collegeproject.chatgptclone.repository.QnARepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.EventListener;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Service layer for handling chat operations, now interacting with a MongoDB
// * Q&A knowledge base instead of an external LLM API.
// * Manages chat messages in MongoDB and provides predefined answers.
// */
//@Service
//public class ChatService {
//
//    private final ChatRepository chatRepository;
//    private final QnARepository qnaRepository;
//
//    // Constructor injection for ChatRepository and QnARepository
//    public ChatService(ChatRepository chatRepository, QnARepository qnaRepository) {
//        this.chatRepository = chatRepository;
//        this.qnaRepository = qnaRepository;
//    }
//
//    @EventListener
//    public void onApplicationEvent(ContextRefreshedEvent event) {
//        if (qnaRepository.count() == 0) {
//            System.out.println("Populating initial Q&A data...");
//            qnaRepository.save(new QnADocument("hello", "Hi there! How can I help you today?"));
//            qnaRepository.save(new QnADocument("how are you", "I'm just a bot, but I'm functioning perfectly!"));
//            qnaRepository.save(new QnADocument("what is your name", "I am a simple chat bot created for a college project."));
//            qnaRepository.save(new QnADocument("who created you", "I was created by a talented college student for their project."));
//            qnaRepository.save(new QnADocument("what is spring boot", "Spring Boot is an open source Java-based framework used to create a micro Service."));
//            qnaRepository.save(new QnADocument("what is mongodb", "MongoDB is a source-available, cross-platform, document-oriented database program."));
//            qnaRepository.save(new QnADocument("thank you", "You're welcome! Is there anything else I can assist you with?"));
//            qnaRepository.save(new QnADocument("bye", "Goodbye! Have a great day!"));
//            System.out.println("Initial Q&A data populated.");
//        }
//    }
//
//    /**
//     * Processes a user message: saves it to MongoDB, queries the Q&A knowledge base,
//     * saves the bot's response, and returns the bot's message.
//     *
//     * @param userId The ID of the currently authenticated user.
//     * @param userMessage The message sent by the user.
//     * @return The response message found in the Q&A knowledge base or a default message.
//     */
//    public String sendMessage(String userId, String userMessage) { // NEW: userId parameter
//        // 1. Save user message to MongoDB with userId
//        ChatMessage userChatMessage = new ChatMessage(userId, "user", userMessage); // Pass userId
//        chatRepository.save(userChatMessage);
//        System.out.println("User message saved for user " + userId + ": " + userMessage);
//
//        String botResponse;
//        Optional<QnADocument> qnaEntry = qnaRepository.findByQuestionIgnoreCase(userMessage.trim());
//
//        if (qnaEntry.isPresent()) {
//            botResponse = qnaEntry.get().getAnswer();
//            System.out.println("Found Q&A response: " + botResponse);
//        } else {
//            botResponse = "I'm sorry, I don't have an answer for that in my knowledge base. Please try asking something else.";
//            System.out.println("No Q&A response found for: " + userMessage);
//        }
//
//        // 2. Save bot's response to MongoDB with userId
//        ChatMessage botChatMessage = new ChatMessage(userId, "bot", botResponse); // Pass userId
//        chatRepository.save(botChatMessage);
//        System.out.println("Bot message saved for user " + userId + ": " + botResponse);
//
//        return botResponse;
//    }
//
//    /**
//     * Retrieves the chat history for a specific user from MongoDB, ordered by timestamp.
//     *
//     * @param userId The ID of the user whose chat history is to be retrieved.
//     * @return A list of ChatMessage objects for the given user.
//     */
//    public List<ChatMessage> getChatHistory(String userId) { // NEW: userId parameter
//        return chatRepository.findByUserIdOrderByTimestampAsc(userId);
//    }
//}
//

package com.collegeproject.chatgptclone.service;

import com.collegeproject.chatgptclone.model.ChatMessage;
import com.collegeproject.chatgptclone.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final RestTemplate restTemplate;

    // URL of your Python LLM Service (running Ollama/LLaVA)
    private static final String LLM_API_URL = "http://127.0.0.1:5000/predict";

    // Constructor Injection
    // We REMOVED QnARepository because we are now using the Python AI instead.
    @Autowired
    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
        this.restTemplate = new RestTemplate(); // Initialize the REST client for HTTP calls
    }

    /**
     * Processes a user message:
     * 1. Saves user message to MongoDB.
     * 2. Sends message to Python LLM Service.
     * 3. Saves LLM response to MongoDB.
     * 4. Returns the response.
     */
    private final java.util.Set<String> cancelledRequestIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Marks a specific request ID as cancelled.
     */
    public void cancelRequest(String requestId) {
        cancelledRequestIds.add(requestId);
        System.out.println("Request marked for cancellation: " + requestId);
    }

    /**
     * Processes a user message:
     * 1. Saves user message to MongoDB.
     * 2. Sends message to Python LLM Service.
     * 3. Saves LLM response to MongoDB (UNLESS CANCELLED).
     * 4. Returns the response.
     */
    public String sendMessage(String userId, String userMessage, String requestId, String imageBase64) {
        // 1. Save user message to MongoDB
        ChatMessage userChatMessage = new ChatMessage(userId, "user", userMessage, imageBase64); // Pass image
        chatRepository.save(userChatMessage);
        System.out.println("User message saved for user " + userId + ": " + userMessage
                + (imageBase64 != null ? " [Image Attached]" : ""));

        // Check cancellation before calling LLM (fast fail)
        if (requestId != null && cancelledRequestIds.contains(requestId)) {
            cancelledRequestIds.remove(requestId); // Cleanup
            saveStoppedMessage(userId);
            return "Request Stopped by you";
        }

        // 2. Call the Python LLM Service
        // This is a BLOCKING call. While this runs, cancelRequest might be called in
        // another thread.
        String botResponse = getResponseFromLLM(userId, userMessage, imageBase64); // Pass image AND userId

        // Check cancellation AGAIN after LLM returns
        if (requestId != null && cancelledRequestIds.contains(requestId)) {
            System.out.println("Request " + requestId + " was cancelled during generation.");
            cancelledRequestIds.remove(requestId); // Cleanup

            // NEW: Save "Request Stopped by you" to DB
            saveStoppedMessage(userId);

            return "Request Stopped by you";
        }

        // 3. Save bot's response to MongoDB
        ChatMessage botChatMessage = new ChatMessage(userId, "bot", botResponse);
        chatRepository.save(botChatMessage);
        System.out.println("Bot message saved for user " + userId + ": " + botResponse);

        return botResponse;
    }

    private void saveStoppedMessage(String userId) {
        String stopMsg = "Request Stopped by you";
        ChatMessage stopChatMessage = new ChatMessage(userId, "bot", stopMsg);
        chatRepository.save(stopChatMessage);
        System.out.println("Saved STOPPED message for user " + userId);
    }

    /**
     * Helper method to send the request to the Python Microservice.
     */
    /**
     * Helper method to send the request to the Python Microservice.
     */
    private String getResponseFromLLM(String userId, String message, String imageBase64) {
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Fetch recent history (DISABLED FOR SPEED as per user request)
            // List<ChatMessage> historyList =
            // chatRepository.findTop5ByUserIdOrderByTimestampDesc(userId);
            List<ChatMessage> historyList = new java.util.ArrayList<>();
            // Reverse to chronological order
            // java.util.Collections.reverse(historyList);

            // Map to JSON-friendly format
            List<Map<String, String>> historyJson = new java.util.ArrayList<>();
            for (ChatMessage msg : historyList) {
                Map<String, String> entry = new HashMap<>();
                // Map 'sender' to Ollama roles
                entry.put("role", "bot".equals(msg.getSender()) ? "assistant" : "user");
                entry.put("content", msg.getMessage());
                historyJson.add(entry);
            }
            // Temporarily print history size
            System.out.println("Attaching history of size: " + historyJson.size());

            // Create JSON payload: {"message": "User's query", "image": "base64...",
            // "history": [...]}
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            requestBody.put("history", historyJson); // Add history

            if (imageBase64 != null && !imageBase64.isEmpty()) {
                requestBody.put("image", imageBase64);
            }

            // NEW: Fetch and attach chat history for context
            // We can't easily get userId here without passing it.
            // So we need to update getResponseFromLLM signature or assume we passed it.
            // Wait, I need userId to fetch history.
            // Let's refactor getResponseFromLLM to take userId.

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request to http://localhost:5000/predict
            ResponseEntity<Map> response = restTemplate.postForEntity(LLM_API_URL, entity, Map.class);

            // Extract "response" field from JSON result
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return response.getBody().get("response").toString();
            } else {
                return "Error: The AI model returned an empty response.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: Could not connect to the AI Service. Is 'python app.py' running?";
        }
    }

    /**
     * Retrieves the chat history for a specific user.
     * Keeps the original functionality from your report[cite: 746].
     */
    public List<ChatMessage> getChatHistory(String userId) {
        // Matches the repository method defined in your report [cite: 646]
        return chatRepository.findByUserIdOrderByTimestampAsc(userId);
    }
}