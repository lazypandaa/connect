package klu.com.service;

import klu.com.model.JWTManager;
import klu.com.model.Post;
import klu.com.model.Users;
import klu.com.repository.PostRepository;
import klu.com.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PostService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private JWTManager jwtManager;
    
    /**
     * Create a new post with image data
     */
    public Map<String, Object> createPost(String token, String caption, String imageData, String visibility) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Creating post with caption: " + caption);
            System.out.println("Image data present: " + (imageData != null && !imageData.isEmpty()));
            
            // Validate token
            String email = jwtManager.validateToken(token);
            if ("401".equals(email)) {
                System.out.println("Invalid token");
                response.put("status", "error");
                response.put("message", "Invalid or expired token");
                return response;
            }
            
            // Get user
            Optional<Users> userOpt = usersRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                System.out.println("User not found");
                response.put("status", "error");
                response.put("message", "User not found");
                return response;
            }
            
            Users user = userOpt.get();
            System.out.println("User found: " + user.getFullname());
            
            // Create post entity
            Post post = new Post();
            post.setUser(user);
            post.setCaption(caption);
            post.setImageData(imageData);
            post.setCreatedAt(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());
            post.setPrivacy(visibility != null ? visibility : "public");
            
            // Save post to database
            Post savedPost = postRepository.save(post);
            System.out.println("Post saved with ID: " + savedPost.getId());
            
            response.put("status", "success");
            response.put("message", "Post created successfully");
            response.put("postId", savedPost.getId());
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating post: " + e.getMessage());
            response.put("status", "error");
            response.put("message", "Error creating post: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Get recent posts for feed
     */
    public List<Map<String, Object>> getRecentPosts(String token) {
        // Validate token
        String email = jwtManager.validateToken(token);
        if ("401".equals(email)) {
            return List.of();
        }
        
        // Get posts from database
        String query = "SELECT p.*, u.fullname, u.email FROM posts p " +
                       "JOIN users u ON p.user_id = u.id " +
                       "ORDER BY p.created_at DESC LIMIT 20";
        
        return jdbcTemplate.queryForList(query);
    }
    
    /**
     * Get posts by a specific user
     */
    public List<Map<String, Object>> getUserPosts(String token, Long userId) {
        // Validate token
        String email = jwtManager.validateToken(token);
        if ("401".equals(email)) {
            return List.of();
        }
        
        // Get posts from database
        String query = "SELECT p.*, u.fullname, u.email FROM posts p " +
                       "JOIN users u ON p.user_id = u.id " +
                       "WHERE p.user_id = ? " +
                       "ORDER BY p.created_at DESC";
        
        return jdbcTemplate.queryForList(query, userId);
    }
    
    /**
     * Like a post
     */
    public Map<String, Object> likePost(String token, Long postId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate token
            String email = jwtManager.validateToken(token);
            if ("401".equals(email)) {
                response.put("status", "error");
                response.put("message", "Invalid or expired token");
                return response;
            }
            
            // Increment likes count
            String updateQuery = "UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?";
            int updated = jdbcTemplate.update(updateQuery, postId);
            
            if (updated > 0) {
                // Get updated likes count
                String countQuery = "SELECT likes_count FROM posts WHERE id = ?";
                Integer likesCount = jdbcTemplate.queryForObject(countQuery, Integer.class, postId);
                
                response.put("status", "success");
                response.put("message", "Post liked successfully");
                response.put("likesCount", likesCount);
            } else {
                response.put("status", "error");
                response.put("message", "Post not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Error liking post: " + e.getMessage());
        }
        
        return response;
    }
}
