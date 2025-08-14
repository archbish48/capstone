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

        Optional<Reaction> opt = reactionRepository.findByPostAndUser(post, me);

        if (opt.isEmpty()) {                // 첫 투표
            reactionRepository.save(new Reaction(post, me, newType));
            applyCounter(postId, newType, +1);   //  엔티티 저장 없이 직접 증감
        } else {
            Reaction r = opt.get();
            if (r.getType() == newType) {    // 같은 버튼 → 취소
                reactionRepository.delete(r);
                applyCounter(postId, newType, -1);
            } else {                         // 다른 버튼 → 교체
                applyCounter(postId, r.getType(), -1);
                r.setType(newType);          // Reaction 만 변경 (post 는 건드리지 않음)
                // JPA 가 r 변경은 flush 할 것임
                applyCounter(postId, newType, +1);
            }
        }
    }

    private void applyCounter(Long postId, ReactionType type, int delta) {
        if (type == ReactionType.LIKE) {
            communityRepository.bumpLikeCount(postId, delta);
        } else {
            communityRepository.bumpDislikeCount(postId, delta);
        }
    }
}
