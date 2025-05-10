package klu.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatMessageService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Get messages between two users, ordered by timestamp
     */
    public List<Map<String, Object>> getMessagesBetweenUsers(Long userId, Long friendId) {
        String query = "SELECT * FROM chat_messages " +
                       "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                       "ORDER BY timestamp ASC";
        
        return jdbcTemplate.queryForList(query, userId, friendId, friendId, userId);
    }
    
    /**
     * Save a new message to the chat_messages table
     */
    public Map<String, Object> saveMessage(String messageText, int readStatus, Long receiverId, Long senderId, String timestamp) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO chat_messages (message_text, read_status, receiver_id, sender_id, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, messageText);
                ps.setInt(2, readStatus);
                ps.setLong(3, receiverId);
                ps.setLong(4, senderId);
                ps.setString(5, timestamp);
                return ps;
            }, keyHolder);
            
            Long messageId = keyHolder.getKey().longValue();
            
            response.put("status", "success");
            response.put("message", "Message sent successfully");
            response.put("messageId", messageId);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send message: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Mark all messages from sender to receiver as read
     */
    public Map<String, Object> markMessagesAsRead(Long receiverId, Long senderId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String updateQuery = "UPDATE chat_messages SET read_status = 1 " +
                                "WHERE sender_id = ? AND receiver_id = ? AND read_status = 0";
            
            int updatedCount = jdbcTemplate.update(updateQuery, senderId, receiverId);
            
            response.put("status", "success");
            response.put("message", "Messages marked as read");
            response.put("updatedCount", updatedCount);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to mark messages as read: " + e.getMessage());
        }
        
        return response;
    }
}
