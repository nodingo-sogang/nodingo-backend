package nodingo.core.friendship.repository;
import nodingo.core.friendship.domain.Friendship;
import nodingo.core.friendship.repository.custom.FriendshipRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long>, FriendshipRepositoryCustom {
    @Modifying
    @Query("delete from Friendship f where f.requester.id = :requesterId or f.receiver.id = :receiverId")
    void deleteByRequesterIdOrReceiverId(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);
}
