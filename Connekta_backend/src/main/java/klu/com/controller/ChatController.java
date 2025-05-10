package klu.com.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import klu.com.model.ChatMessage;
import klu.com.model.JWTManager;
import klu.com.repository.ChatMessageRepository;
import klu.com.repository.UsersRepository;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private JWTManager jwtManager;
    
    // Get messages between two users
    @PostMapping("/messages")
    public ResponseEntity<?> getMessages(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("csrid");
            Long userId = Long.valueOf(String.valueOf(request.get("userId")));
            Long friendId = Long.valueOf(String.valueOf(request.get("friendId")));
            
            // Validate token
            String email = jwtManager.validateToken(token);
            if (email.equals("401")) {
                return ResponseEntity.status(401).body("Invalid token");
            }
            
            // Get messages between the users
            List<ChatMessage> messages = chatMessageRepository.findMessagesBetweenUsers(userId, friendId);
            
            // Mark messages as read if the current user is the receiver
            chatMessageRepository.markMessagesAsRead(friendId, userId);
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching messages: " + e.getMessage());
        }
    }
    
    // Send a new message
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("csrid");
            Long senderId = Long.valueOf(String.valueOf(request.get("senderId")));
            Long receiverId = Long.valueOf(String.valueOf(request.get("receiverId")));
            String messageText = (String) request.get("messageText");
            
            // Validate token
            String email = jwtManager.validateToken(token);
            if (email.equals("401")) {
                return ResponseEntity.status(401).body("Invalid token");
            }
            
            // Create and save the message
            ChatMessage message = new ChatMessage();
            message.setMessageText(messageText);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setReadStatus(0); // Unread
            message.setTimestamp(new Date());
            
            chatMessageRepository.save(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("messageId", message.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error sending message: " + e.getMessage());
        }
    }
    
    // Mark messages as read
    @PostMapping("/read")
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("csrid");
            Long senderId = Long.valueOf(String.valueOf(request.get("senderId")));
            Long receiverId = Long.valueOf(String.valueOf(request.get("receiverId")));
            
            // Validate token
            String email = jwtManager.validateToken(token);
            if (email.equals("401")) {
                return ResponseEntity.status(401).body("Invalid token");
            }
            
            int updatedCount = chatMessageRepository.markMessagesAsRead(senderId, receiverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error marking messages as read: " + e.getMessage());
        }
    }
}

