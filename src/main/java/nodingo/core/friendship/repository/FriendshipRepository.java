package nodingo.core.friendship.repository;
import nodingo.core.friendship.domain.Friendship;
import nodingo.core.friendship.repository.custom.FriendshipRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long>, FriendshipRepositoryCustom {

}
