package klu.com.controller;

import klu.com.model.JWTManager;
import klu.com.model.Users;
import klu.com.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/search")
public class SearchController {
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private JWTManager jwtManager;
    
    /**
     * Search for users by name or email
     */
    @PostMapping("/users")
    public Map<String, Object> searchUsers(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = (String) request.get("csrid");
            String query = (String) request.get("query");
            
            // Validate token
            String email = jwtManager.validateToken(token);
            if ("401".equals(email)) {
                response.put("status", "error");
                response.put("message", "Invalid or expired token");
                return response;
            }
            
            // Get current user
            Optional<Users> currentUserOpt = usersRepository.findByEmail(email);
            if (!currentUserOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "User not found");
                return response;
            }
            
            Users currentUser = currentUserOpt.get();
            
            // Execute search
            List<Users> searchResults;
            
            if (query == null || query.trim().isEmpty()) {
                searchResults = usersRepository.findAll();
            } else {
                // Search by name or email
                searchResults = usersRepository.findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    query, query);
            }
            
            // Filter out current user
            searchResults = searchResults.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
            
            // Format results
            List<Map<String, Object>> formattedResults = new ArrayList<>();
            for (Users user : searchResults) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("fullname", user.getFullname());
                userMap.put("email", user.getEmail());
                // Don't include password
                
                formattedResults.add(userMap);
            }
            
            response.put("status", "success");
            response.put("users", formattedResults);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Error searching users: " + e.getMessage());
        }
        
        return response;
    }
}
