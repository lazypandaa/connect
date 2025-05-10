package klu.com.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {
    
    private final BCryptPasswordEncoder bcryptEncoder;
    
    public PasswordEncoder() {
        // The strength parameter (12) determines the complexity of the hash
        // Higher values are more secure but slower
        this.bcryptEncoder = new BCryptPasswordEncoder(12);
    }
    
    /**
     * Encrypts a password using BCrypt algorithm
     * 
     * @param rawPassword The plain text password to encrypt
     * @return The encrypted password hash
     */
    public String encode(String rawPassword) {
        return bcryptEncoder.encode(rawPassword);
    }
    
    /**
     * Verifies if a raw password matches an encrypted password
     * 
     * @param rawPassword The plain text password to check
     * @param encodedPassword The encrypted password to compare with
     * @return true if the passwords match, false otherwise
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return bcryptEncoder.matches(rawPassword, encodedPassword);
    }
}
