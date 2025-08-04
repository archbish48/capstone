package com.community.demo.controller;

import com.community.demo.dto.community.CommentRequest;
import com.community.demo.dto.community.CommentResponse;
import com.community.demo.dto.community.ReactionRequest;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityRequest;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.service.community.CommentService;
import com.community.demo.service.community.CommunityService;
import com.community.demo.service.community.ReactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;    // 커뮤니티 전반적 총괄
    private final ReactionService reactionService;      //  토글 담당
    private final CommentService commentService;        //  댓글 담당

    @PostMapping("/create")
    public ResponseEntity<CommunityResponse> create(@RequestBody CommunityRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.createPost(request, me.getId()));
    }

    @GetMapping
    public ResponseEntity<List<CommunityResponse>> findAll() {
        return ResponseEntity.ok(communityService.getAllPosts());
    }
    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getPostById(id));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<CommunityResponse> update(@PathVariable Long id, @RequestBody @Valid CommunityRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.updatePost(id, request, me.getId()));

    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        communityService.deletePost(id, me.getId());
        return ResponseEntity.noContent().build();
    }

    // 반응
    @PostMapping("/{postId}/reactions")
    public ResponseEntity<Void> toggleReaction(@PathVariable Long postId, @RequestBody @Valid ReactionRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.toggle(postId, me, request.getType());
        return ResponseEntity.ok().build();      // 200 – 본문 없음
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.list(postId));
    }

    // 댓글 작성 (로그인 필요)
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment (@PathVariable Long postId, @RequestBody @Valid CommentRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponse res = commentService.create(postId, me, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 댓글 삭제 (작성자 or ADMIN)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        commentService.delete(commentId, me);
        return ResponseEntity.noContent().build();
    }

    //작성 권한 체크 예시
//    @PreAuthorize("""
//    hasRole('ADMIN') or
//    ( #postType == 'NOTICE' ? hasRole('MANAGER') : hasAnyRole('STUDENT','STAFF','MANAGER') )""")
//    @PostMapping("/posts")
//    public void createPost(@RequestBody PostRequest dto, Authentication auth) {
//        User me = (User) auth.getPrincipal();
//        communityService.createPost(dto, me);
//    }






}

