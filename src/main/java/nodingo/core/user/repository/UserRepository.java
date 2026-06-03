package nodingo.core.user.repository;

import nodingo.core.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByUsername(String username);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.interests WHERE u.id = :userId")
    Optional<User> findByIdWithInterests(@Param("userId") Long userId);
}
