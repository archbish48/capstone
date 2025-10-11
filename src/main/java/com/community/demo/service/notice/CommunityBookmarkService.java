package com.community.demo.service.notice;


import com.community.demo.domain.community.Community;
import com.community.demo.domain.community.CommunityBookmark;
import com.community.demo.domain.user.User;
import com.community.demo.repository.CommunityBookmarkRepository;
import com.community.demo.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityBookmarkService {
    private final CommunityRepository communityRepository;
    private final CommunityBookmarkRepository bookmarkRepository;

    @Transactional
    public boolean toggle(Long postId, User user) {
        Community post = communityRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글 없음"));

        Optional<CommunityBookmark> existing = bookmarkRepository.findByUserAndPost(user, post);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return false; // 북마크 해제됨
        } else {
            CommunityBookmark bookmark = new CommunityBookmark();
            bookmark.setUser(user);
            bookmark.setPost(post);
            bookmarkRepository.save(bookmark);
            return true; // 북마크 추가됨
        }
    }

}
