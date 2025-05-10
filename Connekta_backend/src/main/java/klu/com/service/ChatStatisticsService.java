package klu.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatStatisticsService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Get statistics about a user's chat activity
     */
    public Map<String, Object> getChatStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // Total number of messages sent by the user
            String sentQuery = "SELECT COUNT(*) FROM chat_messages WHERE sender_id = ?";
            int sentCount = jdbcTemplate.queryForObject(sentQuery, Integer.class, userId);
            statistics.put("messagesSent", sentCount);
            
            // Total number of messages received by the user
            String receivedQuery = "SELECT COUNT(*) FROM chat_messages WHERE receiver_id = ?";
            int receivedCount = jdbcTemplate.queryForObject(receivedQuery, Integer.class, userId);
            statistics.put("messagesReceived", receivedCount);
            
            // Number of unread messages
            String unreadQuery = "SELECT COUNT(*) FROM chat_messages WHERE receiver_id = ? AND read_status = 0";
            int unreadCount = jdbcTemplate.queryForObject(unreadQuery, Integer.class, userId);
            statistics.put("unreadMessages", unreadCount);
            
            // Number of conversations
            String conversationsQuery = 
                "SELECT COUNT(DISTINCT IF(sender_id = ?, receiver_id, sender_id)) " +
                "FROM chat_messages " +
                "WHERE sender_id = ? OR receiver_id = ?";
            int conversationsCount = jdbcTemplate.queryForObject(conversationsQuery, Integer.class, userId, userId, userId);
            statistics.put("conversationsCount", conversationsCount);
            
            statistics.put("status", "success");
        } catch (Exception e) {
            statistics.put("status", "error");
            statistics.put("message", "Failed to fetch chat statistics: " + e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * Get counts of unread messages per sender
     */
    public Map<String, Object> getUnreadMessageCounts(Long userId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> unreadCounts = new ArrayList<>();
        
        try {
            String query = 
                "SELECT sender_id, u.fullname, COUNT(*) as unread_count " +
                "FROM chat_messages cm " +
                "JOIN users u ON cm.sender_id = u.id " +
                "WHERE cm.receiver_id = ? AND cm.read_status = 0 " +
                "GROUP BY cm.sender_id, u.fullname";
            
            unreadCounts = jdbcTemplate.queryForList(query, userId);
            
            response.put("status", "success");
            response.put("unreadCounts", unreadCounts);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch unread counts: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Get the last message for each conversation
     */
    public Map<String, Object> getLastMessages(Long userId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> lastMessages = new ArrayList<>();
        
        try {
            String query = 
                // This complex query gets the last message for each conversation
                "WITH RankedMessages AS ( " +
                "    SELECT " +
                "        cm.*, " +
                "        ROW_NUMBER() OVER(PARTITION BY " +
                "            CASE " +
                "                WHEN cm.sender_id = ? THEN cm.receiver_id " +
                "                ELSE cm.sender_id " +
                "            END " +
                "        ORDER BY cm.timestamp DESC) as rn, " +
                "        CASE " +
                "            WHEN cm.sender_id = ? THEN cm.receiver_id " +
                "            ELSE cm.sender_id " +
                "        END as other_user_id " +
                "    FROM chat_messages cm " +
                "    WHERE cm.sender_id = ? OR cm.receiver_id = ? " +
                ") " +
                "SELECT " +
                "    rm.*, " +
                "    u.fullname as other_user_name " +
                "FROM RankedMessages rm " +
                "JOIN users u ON rm.other_user_id = u.id " +
                "WHERE rm.rn = 1 " +
                "ORDER BY rm.timestamp DESC";
            
            lastMessages = jdbcTemplate.queryForList(query, userId, userId, userId, userId);
            
            response.put("status", "success");
            response.put("lastMessages", lastMessages);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch last messages: " + e.getMessage());
        }
        
        return response;
    }
}
