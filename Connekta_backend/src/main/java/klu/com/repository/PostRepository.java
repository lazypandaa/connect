package klu.com.repository;

import klu.com.model.Post;
import klu.com.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    List<Post> findByUserOrderByCreatedAtDesc(Users user);
    
    List<Post> findTop20ByOrderByCreatedAtDesc();
}
