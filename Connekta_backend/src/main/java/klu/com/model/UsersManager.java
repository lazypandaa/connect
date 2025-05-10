package klu.com.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import klu.com.repository.UsersRepository;
import klu.com.util.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsersManager {

    @Autowired
    private UsersRepository usersRepo;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    JWTManager JM;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public String addUser(Users user) {
        try {
            // Check if email already exists
            if (usersRepo.findByEmail(user.getEmail()).isPresent()) {
                return "Email already exists. Please use a different email.";
            }
            
            // Encrypt the password before saving
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);
            
            usersRepo.save(user);
            return "User Added Successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error adding user: " + e.getMessage();
        }
    }

    public String login(String email, String password) {
        // Get user by email
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            
            // Verify password match using BCrypt
            if (passwordEncoder.matches(password, user.getPassword())) {
                String token = JM.generateToken(email);
                return token;
            }
        }
        
        return "401::Invalid Credentials";
    }

    public String getFullname(String token) {
        String email = JM.validateToken(token);
        if (email.compareTo("401") == 0) {
            return "401::Token Expired";
        }
        Users U = usersRepo.findByEmail(email).get();
        return U.getFullname();
    }

    public String getUserId(String token) {
        String email = JM.validateToken(token);
        if (email.compareTo("401") == 0) {
            return "401::Token Expired";
        }
        
        Users U = usersRepo.findByEmail(email).orElse(null);
        if (U == null) return "404::User Not Found";
        return String.valueOf(U.getId());
    }
    
    public List<Map<String, Object>> getAllUsers() {
        List<Users> usersList = usersRepo.findAll();
        List<Map<String, Object>> usersData = new ArrayList<>();
        
        for (Users user : usersList) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", user.getId());
            userData.put("fullname", user.getFullname());
            userData.put("email", user.getEmail());
            // Don't include sensitive information like password
            usersData.add(userData);
        }
        
        return usersData;
    }
}
