package klu.com.model;

import klu.com.repository.FriendsRepository;
import klu.com.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FriendsManager {

    @Autowired
    private FriendsRepository friendsRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private JWTManager jwtManager;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Send a friend request from one user to another
     */
    public Map<String, Object> sendFriendRequest(String token, Long receiverId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String senderEmail = jwtManager.validateToken(token);
        if (senderEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get sender user
        Optional<Users> senderOpt = usersRepository.findByEmail(senderEmail);
        if (senderOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Sender user not found");
            return response;
        }
        
        // Get receiver user
        Optional<Users> receiverOpt = usersRepository.findById(receiverId);
        if (receiverOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Receiver user not found");
            return response;
        }
        
        Users sender = senderOpt.get();
        Users receiver = receiverOpt.get();
        Long senderId = sender.getId();
        
        // Check if users are the same
        if (senderId.equals(receiverId)) {
            response.put("status", "error");
            response.put("message", "Cannot send friend request to yourself");
            return response;
        }
        
        try {
            // Check if a friendship already exists between these users
            String checkQuery = "SELECT * FROM friends WHERE " +
                              "((user_id = ? AND friend_id = ?) OR " +
                              "(user_id = ? AND friend_id = ?))";
            
            List<Map<String, Object>> existingFriendships = jdbcTemplate.queryForList(
                checkQuery, 
                senderId, receiverId, 
                receiverId, senderId
            );
            
            if (!existingFriendships.isEmpty()) {
                Map<String, Object> existingFriendship = existingFriendships.get(0);
                String status = (String) existingFriendship.get("status");
                
                switch (status) {
                    case "accepted":
                        response.put("status", "error");
                        response.put("message", "Users are already friends");
                        return response;
                        
                    case "pending":
                        Long existingSenderId = (Long) existingFriendship.get("sender_id");
                        if (existingSenderId.equals(senderId)) {
                            response.put("status", "error");
                            response.put("message", "Friend request already sent");
                        } else {
                            // If the other user already sent a request, accept it
                            String updateQuery = "UPDATE friends SET status = 'accepted', updated_at = NOW() " +
                                                "WHERE id = ?";
                            jdbcTemplate.update(updateQuery, existingFriendship.get("id"));
                            
                            response.put("status", "success");
                            response.put("message", "Friend request accepted");
                        }
                        return response;
                        
                    case "rejected":
                        // Allow sending request again if previously rejected
                        String updateQuery = "UPDATE friends SET " +
                                          "user_id = ?, friend_id = ?, status = 'pending', " +
                                          "sender_id = ?, receiver_id = ?, updated_at = NOW() " +
                                          "WHERE id = ?";
                        jdbcTemplate.update(
                            updateQuery, 
                            senderId, receiverId, 
                            senderId, receiverId,
                            existingFriendship.get("id")
                        );
                        
                        response.put("status", "success");
                        response.put("message", "Friend request sent");
                        return response;
                        
                    case "blocked":
                        response.put("status", "error");
                        response.put("message", "Cannot send friend request to this user");
                        return response;
                }
            }
            
            // If no friendship exists, create a new one
            String insertQuery = "INSERT INTO friends " +
                                "(user_id, friend_id, status, sender_id, receiver_id) " +
                                "VALUES (?, ?, 'pending', ?, ?)";
            
            jdbcTemplate.update(
                insertQuery, 
                senderId, receiverId, 
                senderId, receiverId
            );
            
            response.put("status", "success");
            response.put("message", "Friend request sent successfully");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error sending friend request: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Accept a friend request
     */
    public Map<String, Object> acceptFriendRequest(String token, Long requestId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        // Get friend request
        Optional<Friends> friendshipOpt = friendsRepository.findById(requestId);
        if (friendshipOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Friend request not found");
            return response;
        }
        
        Friends friendship = friendshipOpt.get();
        Users user = userOpt.get();
        
        // Check if this user is the receiver of the request
        if (!friendship.getReceiver().getId().equals(user.getId())) {
            response.put("status", "error");
            response.put("message", "Cannot accept this friend request");
            return response;
        }
        
        // Check if request is pending
        if (!friendship.getStatus().equals("pending")) {
            response.put("status", "error");
            response.put("message", "This friend request cannot be accepted (status: " + friendship.getStatus() + ")");
            return response;
        }
        
        // Accept the request
        friendship.setStatus("accepted");
        friendsRepository.save(friendship);
        
        response.put("status", "success");
        response.put("message", "Friend request accepted successfully");
        return response;
    }
    
    /**
     * Reject a friend request
     */
    public Map<String, Object> rejectFriendRequest(String token, Long requestId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        // Get friend request
        Optional<Friends> friendshipOpt = friendsRepository.findById(requestId);
        if (friendshipOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Friend request not found");
            return response;
        }
        
        Friends friendship = friendshipOpt.get();
        Users user = userOpt.get();
        
        // Check if this user is the receiver of the request
        if (!friendship.getReceiver().getId().equals(user.getId())) {
            response.put("status", "error");
            response.put("message", "Cannot reject this friend request");
            return response;
        }
        
        // Check if request is pending
        if (!friendship.getStatus().equals("pending")) {
            response.put("status", "error");
            response.put("message", "This friend request cannot be rejected (status: " + friendship.getStatus() + ")");
            return response;
        }
        
        // Reject the request
        friendship.setStatus("rejected");
        friendsRepository.save(friendship);
        
        response.put("status", "success");
        response.put("message", "Friend request rejected successfully");
        return response;
    }
    
    /**
     * Remove a friend
     */
    public Map<String, Object> removeFriend(String token, Long friendId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        // Get friend
        Optional<Users> friendOpt = usersRepository.findById(friendId);
        if (friendOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Friend user not found");
            return response;
        }
        
        Users user = userOpt.get();
        Users friend = friendOpt.get();
        
        // Check if they are friends
        Optional<Friends> friendshipOpt = friendsRepository.findFriendship(user, friend);
        if (friendshipOpt.isEmpty() || !friendshipOpt.get().getStatus().equals("accepted")) {
            response.put("status", "error");
            response.put("message", "Users are not friends");
            return response;
        }
        
        // Remove friendship
        friendsRepository.delete(friendshipOpt.get());
        
        response.put("status", "success");
        response.put("message", "Friend removed successfully");
        return response;
    }
    
    /**
     * Block a user
     */
    public Map<String, Object> blockUser(String token, Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String blockerEmail = jwtManager.validateToken(token);
        if (blockerEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get blocker user
        Optional<Users> blockerOpt = usersRepository.findByEmail(blockerEmail);
        if (blockerOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        // Get target user
        Optional<Users> targetOpt = usersRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Target user not found");
            return response;
        }
        
        Users blocker = blockerOpt.get();
        Users target = targetOpt.get();
        
        // Check if users are the same
        if (blocker.getId().equals(target.getId())) {
            response.put("status", "error");
            response.put("message", "Cannot block yourself");
            return response;
        }
        
        // Check if a relationship exists
        Optional<Friends> relationshipOpt = friendsRepository.findFriendship(blocker, target);
        
        if (relationshipOpt.isPresent()) {
            Friends relationship = relationshipOpt.get();
            // Set blocker as sender for consistency in blocked relationships
            relationship.setSender(blocker);
            relationship.setReceiver(target);
            relationship.setStatus("blocked");
            friendsRepository.save(relationship);
        } else {
            // Create new blocked relationship
            Friends blockedRelationship = new Friends(blocker, target, "blocked");
            friendsRepository.save(blockedRelationship);
        }
        
        response.put("status", "success");
        response.put("message", "User blocked successfully");
        return response;
    }
    
    /**
     * Unblock a user
     */
    public Map<String, Object> unblockUser(String token, Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String unblockerEmail = jwtManager.validateToken(token);
        if (unblockerEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get unblocker user
        Optional<Users> unblockerOpt = usersRepository.findByEmail(unblockerEmail);
        if (unblockerOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        // Get target user
        Optional<Users> targetOpt = usersRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Target user not found");
            return response;
        }
        
        Users unblocker = unblockerOpt.get();
        Users target = targetOpt.get();
        
        // Check if relationship exists and is blocked
        Optional<Friends> relationshipOpt = friendsRepository.findBySenderAndReceiver(unblocker, target);
        if (relationshipOpt.isEmpty() || !relationshipOpt.get().getStatus().equals("blocked")) {
            response.put("status", "error");
            response.put("message", "This user is not blocked");
            return response;
        }
        
        // Remove the blocked relationship
        friendsRepository.delete(relationshipOpt.get());
        
        response.put("status", "success");
        response.put("message", "User unblocked successfully");
        return response;
    }
    
    /**
     * Get all friends of current user
     */
    public Map<String, Object> getFriends(String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users user = userOpt.get();
        
        // Get friends
        List<Users> friends = friendsRepository.findAllFriends(user);
        
        // Format friend data
        List<Map<String, Object>> friendsData = friends.stream()
            .map(friend -> {
                Map<String, Object> friendData = new HashMap<>();
                friendData.put("id", friend.getId());
                friendData.put("fullname", friend.getFullname());
                friendData.put("email", friend.getEmail());
                // Don't include password
                return friendData;
            })
            .collect(Collectors.toList());
        
        response.put("status", "success");
        response.put("friends", friendsData);
        return response;
    }
    
    /**
     * Get all pending friend requests received by current user
     */
    public Map<String, Object> getPendingRequests(String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users user = userOpt.get();
        
        // Get pending requests
        List<Friends> pendingRequests = friendsRepository.findByReceiverAndStatus(user, "pending");
        
        // Format request data
        List<Map<String, Object>> requestsData = pendingRequests.stream()
            .map(request -> {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("id", request.getId());
                requestData.put("sender", Map.of(
                    "id", request.getSender().getId(),
                    "fullname", request.getSender().getFullname(),
                    "email", request.getSender().getEmail()
                ));
                requestData.put("createdAt", request.getCreatedAt().toString());
                return requestData;
            })
            .collect(Collectors.toList());
        
        response.put("status", "success");
        response.put("requests", requestsData);
        return response;
    }
    
    /**
     * Get all sent friend requests by current user
     */
    public Map<String, Object> getSentRequests(String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users user = userOpt.get();
        
        // Get sent requests
        List<Friends> sentRequests = friendsRepository.findBySenderAndStatus(user, "pending");
        
        // Format request data
        List<Map<String, Object>> requestsData = sentRequests.stream()
            .map(request -> {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("id", request.getId());
                requestData.put("receiver", Map.of(
                    "id", request.getReceiver().getId(),
                    "fullname", request.getReceiver().getFullname(),
                    "email", request.getReceiver().getEmail()
                ));
                requestData.put("createdAt", request.getCreatedAt().toString());
                return requestData;
            })
            .collect(Collectors.toList());
        
        response.put("status", "success");
        response.put("requests", requestsData);
        return response;
    }
    
    /**
     * Search for users who are not already friends with current user
     */
    public Map<String, Object> searchUsers(String token, String query) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get current user
        Optional<Users> currentUserOpt = usersRepository.findByEmail(userEmail);
        if (currentUserOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users currentUser = currentUserOpt.get();
        
        // Get all users that match the query
        // This is a simplified search - in production you might want more complex logic
        List<Users> allUsers = usersRepository.findByFullnameContainingIgnoreCase(query);
        
        // Get current user's friends
        List<Users> friends = friendsRepository.findAllFriends(currentUser);
        
        // Filter out current user and existing friends
        List<Map<String, Object>> filteredUsers = allUsers.stream()
            .filter(user -> !user.getId().equals(currentUser.getId()) && !friends.contains(user))
            .map(user -> {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("fullname", user.getFullname());
                userData.put("email", user.getEmail());
                // Check if there's a pending request
                Optional<Friends> pendingRequest = friendsRepository.findBySenderAndReceiver(currentUser, user);
                userData.put("requestSent", pendingRequest.isPresent() && "pending".equals(pendingRequest.get().getStatus()));
                return userData;
            })
            .collect(Collectors.toList());
        
        response.put("status", "success");
        response.put("users", filteredUsers);
        return response;
    }
    
    /**
     * Get friend suggestions (friends of friends)
     */
    public Map<String, Object> getFriendSuggestions(String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get user
        Optional<Users> userOpt = usersRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users currentUser = userOpt.get();
        
        // Get user's direct friends
        List<Users> directFriends = friendsRepository.findAllFriends(currentUser);
        
        // Get friends of friends (potential suggestions)
        Set<Users> friendSuggestions = new HashSet<>();
        for (Users friend : directFriends) {
            List<Users> friendsOfFriend = friendsRepository.findAllFriends(friend);
            friendSuggestions.addAll(friendsOfFriend);
        }
        
        // Remove the current user from suggestions
        friendSuggestions.remove(currentUser);
        
        // Remove direct friends from suggestions (since they're already friends)
        friendSuggestions.removeAll(directFriends);
        
        // Check if the current user has pending requests with any suggestion
        List<Friends> sentRequests = friendsRepository.findBySenderAndStatus(currentUser, "pending");
        List<Users> pendingReceivers = sentRequests.stream()
            .map(Friends::getReceiver)
            .collect(Collectors.toList());
        
        // Format suggestion data
        List<Map<String, Object>> suggestionsData = friendSuggestions.stream()
            .map(user -> {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("fullname", user.getFullname());
                userData.put("email", user.getEmail());
                // Add avatar URL if available (using default for now)
                userData.put("avatar", "https://randomuser.me/api/portraits/" + 
                    (Math.random() > 0.5 ? "men" : "women") + "/" + 
                    (int)(Math.random() * 100) + ".jpg");
                userData.put("requestSent", pendingReceivers.contains(user));
                return userData;
            })
            .collect(Collectors.toList());
        
        response.put("status", "success");
        response.put("suggestions", suggestionsData);
        return response;
    }
    
    /**
     * Check friendship status between current user and another user
     */
    public Map<String, Object> checkFriendshipStatus(String token, Long otherUserId) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate token
        String userEmail = jwtManager.validateToken(token);
        if (userEmail.equals("401")) {
            response.put("status", "error");
            response.put("message", "Invalid or expired token");
            return response;
        }
        
        // Get current user
        Optional<Users> currentUserOpt = usersRepository.findByEmail(userEmail);
        if (currentUserOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }
        
        Users currentUser = currentUserOpt.get();
        Long currentUserId = currentUser.getId();
        
        try {
            // Check if there's a friendship between these users
            String query = "SELECT * FROM friends WHERE " +
                          "((user_id = ? AND friend_id = ?) OR " +
                          "(user_id = ? AND friend_id = ?))";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                query, 
                currentUserId, otherUserId, 
                otherUserId, currentUserId
            );
            
            if (results.isEmpty()) {
                response.put("status", "success");
                response.put("friendshipStatus", "none");
            } else {
                Map<String, Object> friendship = results.get(0);
                String status = (String) friendship.get("status");
                response.put("status", "success");
                response.put("friendshipStatus", status);
                
                // Include additional info for pending requests
                if ("pending".equals(status)) {
                    Long senderId = (Long) friendship.get("sender_id");
                    response.put("requestSent", senderId.equals(currentUserId));
                }
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error checking friendship status: " + e.getMessage());
        }
        
        return response;
    }
}
