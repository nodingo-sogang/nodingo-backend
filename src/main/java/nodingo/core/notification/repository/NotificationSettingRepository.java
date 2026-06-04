package nodingo.core.notification.repository;

import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.notification.repository.custom.NotificationSettingRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long>, NotificationSettingRepositoryCustom {
    Optional<NotificationSetting> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserQuizResult u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}