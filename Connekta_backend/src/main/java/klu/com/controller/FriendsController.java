package klu.com.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import klu.com.model.FriendsManager;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/friends")
public class FriendsController {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendsController.class);
    
    @Autowired
    private FriendsManager friendsManager;
    
    /**
     * Send a friend request to another user
     */
    @PostMapping("/send-request")
    public Map<String, Object> sendFriendRequest(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = (String) request.get("csrid");
            if (token == null) {
                logger.error("Token is missing in the request");
                response.put("status", "error");
                response.put("message", "Authentication token is missing");
                return response;
            }
            
            Object receiverIdObj = request.get("receiverId");
            if (receiverIdObj == null) {
                logger.error("receiverId is missing in the request");
                response.put("status", "error");
                response.put("message", "Receiver ID is required");
                return response;
            }
            
            Long receiverId;
            try {
                receiverId = Long.valueOf(receiverIdObj.toString());
            } catch (NumberFormatException e) {
                logger.error("Invalid receiverId format: " + receiverIdObj);
                response.put("status", "error");
                response.put("message", "Invalid receiver ID format");
                return response;
            }
            
            logger.info("Processing friend request: token={}, receiverId={}", 
                    token.substring(0, Math.min(10, token.length())) + "...", receiverId);
            
            return friendsManager.sendFriendRequest(token, receiverId);
            
        } catch (Exception e) {
            logger.error("Error processing friend request", e);
            response.put("status", "error");
            response.put("message", "Server error: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Accept a friend request
     */
    @PostMapping("/accept-request")
    public Map<String, Object> acceptFriendRequest(@RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("csrid");
            Long requestId = Long.valueOf(request.get("requestId").toString());
            
            Map<String, Object> response = friendsManager.acceptFriendRequest(token, requestId);
            
            // Log for debugging
            System.out.println("Friend request accepted: " + response);
            
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error accepting friend request: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * Reject a friend request
     */
    @PostMapping("/reject-request")
    public Map<String, Object> rejectFriendRequest(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long requestId = Long.valueOf(request.get("requestId").toString());
        return friendsManager.rejectFriendRequest(token, requestId);
    }
    
    /**
     * Remove a friend
     */
    @PostMapping("/remove-friend")
    public Map<String, Object> removeFriend(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long friendId = Long.valueOf(request.get("friendId").toString());
        return friendsManager.removeFriend(token, friendId);
    }
    
    /**
     * Block a user
     */
    @PostMapping("/block-user")
    public Map<String, Object> blockUser(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long userId = Long.valueOf(request.get("userId").toString());
        return friendsManager.blockUser(token, userId);
    }
    
    /**
     * Unblock a user
     */
    @PostMapping("/unblock-user")
    public Map<String, Object> unblockUser(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long userId = Long.valueOf(request.get("userId").toString());
        return friendsManager.unblockUser(token, userId);
    }
    
    /**
     * Get all friends of the current user
     */
    @PostMapping("/get-friends")
    public Map<String, Object> getFriends(@RequestBody Map<String, String> request) {
        String token = request.get("csrid");
        return friendsManager.getFriends(token);
    }
    
    /**
     * Get all pending friend requests for the current user
     */
    @PostMapping("/pending-requests")
    public Map<String, Object> getPendingRequests(@RequestBody Map<String, String> request) {
        String token = request.get("csrid");
        return friendsManager.getPendingRequests(token);
    }
    
    /**
     * Get all friend requests sent by the current user
     */
    @PostMapping("/sent-requests")
    public Map<String, Object> getSentRequests(@RequestBody Map<String, String> request) {
        String token = request.get("csrid");
        return friendsManager.getSentRequests(token);
    }
    
    /**
     * Search for users to add as friends
     */
    @PostMapping("/search-users")
    public Map<String, Object> searchUsers(@RequestBody Map<String, String> request) {
        String token = request.get("csrid");
        String query = request.get("query");
        return friendsManager.searchUsers(token, query);
    }
    
    /**
     * Get friend suggestions (friends of friends)
     */
    @PostMapping("/suggestions")
    public Map<String, Object> getFriendSuggestions(@RequestBody Map<String, String> request) {
        String token = request.get("csrid");
        return friendsManager.getFriendSuggestions(token);
    }
    
    /**
     * Check friendship status between current user and another user
     */
    @PostMapping("/check-status")
    public Map<String, Object> checkFriendshipStatus(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long otherUserId = Long.valueOf(request.get("otherUserId").toString());
        return friendsManager.checkFriendshipStatus(token, otherUserId);
    }
}
