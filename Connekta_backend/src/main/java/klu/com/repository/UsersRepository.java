package klu.com.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import klu.com.model.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    @Query("SELECT COUNT(u) FROM Users u WHERE u.email = :email")
    int validateEmail(@Param("email") String email);

    Optional<Users> findByEmail(String email);

    List<Users> findByFullnameContainingIgnoreCase(String fullname);

    List<Users> findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullname, String email);
}
