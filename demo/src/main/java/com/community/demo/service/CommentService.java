package com.community.demo.service;

import com.community.demo.domain.community.Comment;
import com.community.demo.domain.community.Community;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommentResponse;
import com.community.demo.repository.CommentRepository;
import com.community.demo.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final CommunityRepository communityRepo;

    // 댓글 목록
    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAt(postId)
                .stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        c.getContent(),
                        c.getAuthor().getDepartment(),
                        c.getCreatedAt()))
                .toList();
    }

    // 작성
    @Transactional
    public CommentResponse create(Long postId, User me, String content) {
        Community post = communityRepo.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("post"));

        Comment c = new Comment();
        c.setPost(post);
        c.setAuthor(me);
        c.setContent(content);
        commentRepo.save(c);

        return new CommentResponse(c.getId(), c.getContent(), me.getDepartment(), c.getCreatedAt());
    }

    // 삭제 – 소유자 or ADMIN
    @Transactional
    public void delete(Long commentId, User me) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("comment"));
        boolean owner = c.getAuthor().getId().equals(me.getId());
        boolean admin = me.getRoleType() == RoleType.ADMIN;
        if (!(owner || admin)) {
            throw new AccessDeniedException("삭제 권한 없음");
        }

        commentRepo.delete(c);
    }
}
