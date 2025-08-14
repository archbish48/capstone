package com.community.demo.service.community;

import com.community.demo.domain.community.Comment;
import com.community.demo.domain.community.Community;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommentResponse;
import com.community.demo.repository.CommentRepository;
import com.community.demo.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 댓글 목록 (페이징)
    @Transactional(readOnly = true)
    public Page<CommentResponse> list(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")) // 오래된 순 + 안정적 2차 정렬
        );

        Page<Comment> comments = commentRepo.findByPostId(postId, pageable);

        return comments.map(c -> new CommentResponse(
                c.getId(),
                c.getAuthor().getId(),
                c.getContent(),
                c.getAuthor().getDepartment(),
                c.getCreatedAt()
        ));
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

        return new CommentResponse(c.getId(), c.getAuthor().getId(), c.getContent(), me.getDepartment(), c.getCreatedAt());
    }
    // 댓글 수정
    @Transactional
    public CommentResponse update(Long commentId, User me, String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("댓글 내용이 비어 있습니다.");
        }

        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("comment"));

        boolean owner = c.getAuthor().getId().equals(me.getId());
        boolean admin = me.getRoleType() == RoleType.ADMIN;
        if (!(owner || admin)) {
            throw new AccessDeniedException("수정 권한 없음");
        }

        c.setContent(newContent.trim());
        // 필요 시: c.setUpdatedAt(LocalDateTime.now());  // 엔티티에 필드/콜백이 있으면 자동 처리됨
        commentRepo.save(c); // 명시 저장 (JPA dirty checking만으로도 되지만 안전하게)

        return new CommentResponse(
                c.getId(),
                c.getAuthor().getId(),
                c.getContent(),
                c.getAuthor().getDepartment(),
                c.getCreatedAt()
        );
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
