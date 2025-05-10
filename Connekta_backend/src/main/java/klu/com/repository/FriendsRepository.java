package klu.com.repository;

import klu.com.model.Friends;
import klu.com.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendsRepository extends JpaRepository<Friends, Long> {
    
    // Find friendship by sender and receiver
    Optional<Friends> findBySenderAndReceiver(Users sender, Users receiver);
    
    // Find existing friendship between two users (in either direction)
    @Query("SELECT f FROM Friends f WHERE (f.sender = :user1 AND f.receiver = :user2) OR (f.sender = :user2 AND f.receiver = :user1)")
    Optional<Friends> findFriendship(@Param("user1") Users user1, @Param("user2") Users user2);
    
    // Find all friend requests sent by a user
    List<Friends> findBySenderAndStatus(Users sender, String status);
    
    // Find all friend requests received by a user
    List<Friends> findByReceiverAndStatus(Users receiver, String status);
    
    // Count pending requests for a user
    long countByReceiverAndStatus(Users receiver, String status);
    
    // Find all accepted friendships for a user (either as sender or receiver)
    @Query("SELECT f FROM Friends f WHERE (f.sender = :user OR f.receiver = :user) AND f.status = 'accepted'")
    List<Friends> findAcceptedFriendships(@Param("user") Users user);
    
    // Find all friends of a user (returns Users objects directly)
    @Query("SELECT f.receiver FROM Friends f WHERE f.sender = :user AND f.status = 'accepted' " +
           "UNION " +
           "SELECT f.sender FROM Friends f WHERE f.receiver = :user AND f.status = 'accepted'")
    List<Users> findAllFriends(@Param("user") Users user);
    
    // Check if two users are friends
    @Query("SELECT COUNT(f) > 0 FROM Friends f WHERE " +
           "((f.sender = :user1 AND f.receiver = :user2) OR (f.sender = :user2 AND f.receiver = :user1)) " +
           "AND f.status = 'accepted'")
    boolean areFriends(@Param("user1") Users user1, @Param("user2") Users user2);
}
