package nodingo.core.user.repository;

import nodingo.core.user.domain.User;
import nodingo.core.user.repository.custom.UserRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User,Long>, UserRepositoryCustom {
    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.interests WHERE u.id = :userId")
    Optional<User> findByIdWithInterests(@Param("userId") Long userId);

    Optional<User> findByNicknameAndIdNot(String nickname, Long myUserId);

    @Query("select distinct u from User u join fetch u.personas where u.id in :ids")
    List<User> findAllByIdWithPersonas(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.weeklyXp = 0")
    void resetAllWeeklyXp();
}
