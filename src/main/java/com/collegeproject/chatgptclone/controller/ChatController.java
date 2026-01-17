
package com.collegeproject.chatgptclone.controller;

import com.collegeproject.chatgptclone.model.ChatMessage;
import com.collegeproject.chatgptclone.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // NEW import
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // NEW import
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling chat-related API requests.
 * Manages sending messages and retrieving chat history.
 */
@Controller
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Endpoint for sending a new message to the chat.
     * It expects a JSON body with a "message" field.
     * Requires authentication.
     *
     * @param payload   A map containing the user's message.
     * @param principal The authenticated user's principal.
     * @return A ResponseEntity containing the bot's response message.
     */
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<String> sendMessage(@RequestBody Map<String, String> payload, Principal principal) { // NEW:
                                                                                                               // Principal
                                                                                                               // parameter
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }
        String userId = principal.getName();
        String userMessage = payload.get("message");
        String requestId = payload.get("requestId"); // New: Extract requestId
        String imageBase64 = payload.get("imageBase64"); // NEW: Extract image

        if ((userMessage == null || userMessage.trim().isEmpty()) && (imageBase64 == null || imageBase64.isEmpty())) {
            return ResponseEntity.badRequest().body("Message or Image is required.");
        }
        System.out.println("Received message from " + userId + " (ID: " + requestId + "): " + userMessage);

        String botResponse = chatService.sendMessage(userId, userMessage, requestId, imageBase64); // Pass image
        return ResponseEntity.ok(botResponse);
    }

    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<String> cancelRequest(@RequestBody Map<String, String> payload) {
        String requestId = payload.get("requestId");
        if (requestId != null && !requestId.isEmpty()) {
            chatService.cancelRequest(requestId);
            return ResponseEntity.ok("Cancellation requested.");
        }
        return ResponseEntity.badRequest().body("Missing requestId");
    }

    /**
     * Endpoint for retrieving the entire chat history for the authenticated user.
     * Requires authentication.
     *
     * @param principal The authenticated user's principal.
     * @return A ResponseEntity containing a list of ChatMessage objects.
     */
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(Principal principal) { // NEW: Principal parameter
        if (principal == null) {
            // This should ideally be caught by Spring Security before reaching here.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Or an empty list
        }
        String userId = principal.getName(); // In Spring Security, principal.getName() is the username
        List<ChatMessage> history = chatService.getChatHistory(userId); // Pass userId
        return ResponseEntity.ok(history);
    }
}
