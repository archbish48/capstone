package com.community.demo.service.community;

import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.community.ReactionType;
import com.community.demo.domain.user.User;
import com.community.demo.repository.CommunityRepository;
import com.community.demo.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final CommunityRepository communityRepository;

    /** 토글: 없으면 추가, 같은 타입이면 취소, 다른 타입이면 교체 */
    @Transactional
    public void toggle(Long postId, User me, ReactionType newType) {
        Community post = communityRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("post"));

        Optional<Reaction> opt = reactionRepository.findByPostIdAndUserId(postId, me.getId());

        if (opt.isEmpty()) {                 // 첫 투표
            reactionRepository.save(new Reaction(post, me, newType));
            applyCounter(postId, newType, +1);
            return;
        }

        Reaction r = opt.get();
        ReactionType oldType = r.getType();

        if (oldType == newType) {            // 같은 버튼 → 취소
            reactionRepository.delete(r);
            applyCounter(postId, newType, -1);
            return;
        }

        //  다른 버튼 → 교체: 타입 변경을 "먼저" 확정 저장
        r.setType(newType);
        reactionRepository.save(r);          // 또는 entityManager.flush()

        // 그런 다음 카운터 벌크 업데이트 (벌크가 컨텍스트 clear 하지 않도록 1번에서 수정)
        applyCounter(postId, oldType, -1);
        applyCounter(postId, newType, +1);
    }

    private void applyCounter(Long postId, ReactionType type, int delta) {
        if (type == ReactionType.LIKE) {
            communityRepository.bumpLikeCount(postId, delta);
        } else {
            communityRepository.bumpDislikeCount(postId, delta);
        }
    }
}
