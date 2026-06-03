package nodingo.core.friendship.repository;
import nodingo.core.friendship.domain.Friendship;
import nodingo.core.friendship.repository.custom.FriendshipRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long>, FriendshipRepositoryCustom {

}
