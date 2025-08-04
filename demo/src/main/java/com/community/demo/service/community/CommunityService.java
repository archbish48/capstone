package com.community.demo.service.community;

import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityRequest;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.domain.community.Community;
import com.community.demo.repository.CommunityRepository;
import com.community.demo.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;

    public CommunityResponse createPost(CommunityRequest request, Long userId) {
        // 1. 작성자 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User"));

        // 2. 게시글 생성
        Community post = new Community(request.getTitle(), request.getText(), author);

        //3. 저장 -> dto 변환
        return toResponse(communityRepository.save(post));
    }

    public List<CommunityResponse> getAllPosts() {  //모든 게시글의 제목과 내용을 표시
        return communityRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CommunityResponse getPostById(Long id) {     // 게시글을 클릭했을 때, 상세 페이지로 들어가서 좋아요, 싫어요 수 와 댓글들을 표시
        return toResponse(communityRepository.findById(id)
                .orElseThrow(()-> new NoSuchElementException("No such post")));

    }

    public CommunityResponse updatePost(Long id, @Valid CommunityRequest request, Long userId) {

        //  글 조회 ─ 없으면 404
        Community post = communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No such post"));

        //  현재 로그인 사용자 조회
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        //  권한 검사
        boolean isOwner = post.getAuthor().getId().equals(userId);
        boolean isAdmin = currentUser.getRoleType() == RoleType.ADMIN;

        if (!(isOwner || isAdmin)) {             // 이 글의 작성자 OR 관리자라면 수정 가능
            throw new AccessDeniedException("You are not allowed to modify this post");
        }

        //  수정
        post.setTitle(request.getTitle());
        post.setText(request.getText());

        // 저장 & DTO 변환
        return toResponse(communityRepository.save(post));
    }

    public void deletePost(Long id, Long userId) {
        // 글 조회 - 없으면 404
        Community post = communityRepository.findById(id)
                .orElseThrow(()-> new NoSuchElementException("No such post"));

        // 현재 유저 조회
        User currentUser = userRepository.findById(userId)
                .orElseThrow(()-> new NoSuchElementException("User not found"));

        // 권한 검사
        boolean isOwner = post.getAuthor().getId().equals(userId);
        boolean isAdmin = currentUser.getRoleType() == RoleType.ADMIN;

        if (!(isOwner || isAdmin)) {         // 이 글의 작성자 OR 관리자라면 삭제 가능
            throw new AccessDeniedException("You are not allowed to modify this post");
        }

        communityRepository.delete(post);
    }

    private CommunityResponse toResponse(Community community) {
        return new CommunityResponse(community.getId(), community.getTitle(), community.getText());
    }
}
