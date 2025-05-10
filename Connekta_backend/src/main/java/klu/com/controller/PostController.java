package klu.com.controller;

import klu.com.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/posts")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    /**
     * Endpoint to create a new post
     */
    @PostMapping("/create")
    public Map<String, Object> createPost(@RequestBody Map<String, Object> request) {
        System.out.println("POST /api/posts/create received");
        
        try {
            String token = (String) request.get("csrid");
            String caption = (String) request.get("caption");
            String imageUrl = (String) request.get("imageUrl");
            String visibility = (String) request.get("visibility");
            
            System.out.println("Creating post with token present: " + (token != null));
            System.out.println("Caption: " + caption);
            System.out.println("Image URL present: " + (imageUrl != null && !imageUrl.isEmpty()));
            
            return postService.createPost(token, caption, imageUrl, visibility);
            
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error processing request: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Endpoint to get recent posts for feed
     */
    @PostMapping("/feed")
    public Map<String, Object> getRecentPosts(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = request.get("csrid");
            List<Map<String, Object>> posts = postService.getRecentPosts(token);
            
            response.put("status", "success");
            response.put("posts", posts);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Error fetching posts: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Endpoint to get posts by a specific user
     */
    @PostMapping("/user")
    public Map<String, Object> getUserPosts(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = (String) request.get("csrid");
            Long userId = Long.valueOf(request.get("userId").toString());
            
            List<Map<String, Object>> posts = postService.getUserPosts(token, userId);
            
            response.put("status", "success");
            response.put("posts", posts);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Error fetching user posts: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Endpoint to like a post
     */
    @PostMapping("/like")
    public Map<String, Object> likePost(@RequestBody Map<String, Object> request) {
        String token = (String) request.get("csrid");
        Long postId = Long.valueOf(request.get("postId").toString());
        
        return postService.likePost(token, postId);
    }
    
    /**
     * Test endpoint to verify API availability
     */
    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "API is running");
        return response;
    }
}
