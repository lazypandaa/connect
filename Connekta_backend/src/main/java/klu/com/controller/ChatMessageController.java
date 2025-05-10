package klu.com.controller;

import klu.com.model.JWTManager;
import klu.com.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class ChatMessageController {
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private JWTManager jwtManager;
    
    /**
     * Fetch messages between two users
     */
    @PostMapping("/messages")
    public List<Map<String, Object>> getMessages(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long userId = Long.parseLong(request.get("userId").toString());
        Long friendId = Long.parseLong(request.get("friendId").toString());
        
        // Validate token
        String email = jwtManager.validateToken(token);
        if ("401".equals(email)) {
            return List.of();
        }
        
        return chatMessageService.getMessagesBetweenUsers(userId, friendId);
    }
    
    /**
     * Send a new message and save it to the chat_messages table
     */
    @PostMapping("/send-message")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String token = (String) request.get("csrid");
        String email = jwtManager.validateToken(token);
        if ("401".equals(email)) {
            response.put("status", "error");
            response.put("message", "Invalid token");
            return response;
        }
        
        String messageText = (String) request.get("message_text");
        int readStatus = Integer.parseInt(request.get("read_status").toString());
        Long receiverId = Long.parseLong(request.get("receiver_id").toString());
        Long senderId = Long.parseLong(request.get("sender_id").toString());
        String timestamp = (String) request.get("timestamp");
        
        return chatMessageService.saveMessage(messageText, readStatus, receiverId, senderId, timestamp);
    }
    
    /**
     * Mark messages as read
     */
    @PostMapping("/mark-messages-read")
    public Map<String, Object> markMessagesAsRead(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String token = (String) request.get("csrid");
        String email = jwtManager.validateToken(token);
        if ("401".equals(email)) {
            response.put("status", "error");
            response.put("message", "Invalid token");
            return response;
        }
        
        Long receiverId = Long.parseLong(request.get("receiver_id").toString());
        Long senderId = Long.parseLong(request.get("sender_id").toString());
        
        return chatMessageService.markMessagesAsRead(receiverId, senderId);
    }
}
