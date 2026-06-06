package nodingo.core.friendship.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.friendship.domain.Friendship;
import nodingo.core.friendship.dto.command.FriendActionCommand;
import nodingo.core.friendship.repository.FriendshipRepository;
import nodingo.core.global.exception.friendship.AlreadyFriendException;
import nodingo.core.global.exception.friendship.FriendRequestNotFoundException;
import nodingo.core.global.exception.friendship.SelfFriendRequestException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FriendshipService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public void sendFriendRequest(FriendActionCommand command) {
        log.info(">>>> [Friendship] sendFriendRequest. requesterId={}, receiverId={}",
                command.getMyUserId(), command.getTargetUserId());
        createFriendRequest(command.getMyUserId(), command.getTargetUserId());
        log.info(">>>> [Friendship] Friend request sent. requesterId={}, receiverId={}",
                command.getMyUserId(), command.getTargetUserId());
    }

    public void acceptFriendRequest(FriendActionCommand command) {
        log.info(">>>> [Friendship] acceptFriendRequest. receiverId={}, requesterId={}",
                command.getMyUserId(), command.getTargetUserId());
        Friendship friendship = findPendingFriendship(command);
        friendship.accept();
        log.info(">>>> [Friendship] Friend request accepted. receiverId={}, requesterId={}",
                command.getMyUserId(), command.getTargetUserId());
    }

    private void createFriendRequest(Long requesterId, Long receiverId) {
        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("내 사용자 정보를 찾을 수 없습니다."));
        User target = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("요청할 상대방 사용자 정보를 찾을 수 없습니다."));

        validateFriendRequest(requesterId, receiverId);

        try {
            friendshipRepository.save(Friendship.createRequest(user, target));
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyFriendException("이미 친구 관계이거나 요청이 대기 중입니다.");
        }
    }

    private void validateFriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new SelfFriendRequestException("본인에게는 친구 요청을 보낼 수 없습니다.");
        }

        if (friendshipRepository.existsRelation(requesterId, receiverId)) {
            throw new AlreadyFriendException("이미 친구 관계이거나 요청이 대기 중입니다.");
        }
    }

    private Friendship findPendingFriendship(FriendActionCommand command) {
        Long receiverId = command.getMyUserId();
        Long requesterId = command.getTargetUserId();

        Friendship friendship = friendshipRepository.findPendingRelation(requesterId, receiverId)
                .orElseThrow(() -> new FriendRequestNotFoundException("수락 대기 중인 친구 요청을 찾을 수 없습니다."));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new FriendRequestNotFoundException("수락 권한이 없는 친구 요청입니다.");
        }

        return friendship;
    }
}