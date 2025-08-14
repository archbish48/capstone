package com.community.demo.controller;

import com.community.demo.dto.community.CommentRequest;
import com.community.demo.dto.community.CommentResponse;
import com.community.demo.dto.community.ReactionRequest;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.service.community.CommentService;
import com.community.demo.service.community.CommunityService;
import com.community.demo.service.community.ReactionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;    // 커뮤니티 전반적 총괄
    private final ReactionService reactionService;      //  토글 담당
    private final CommentService commentService;        //  댓글 담당

    // 게시글 작성
    // 문자열 하나로 받아서 ,로 나눈 뒤 List<String>으로 변환
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityResponse> create(
            @RequestPart("title") String title,
            @RequestPart("text") String text,
            //  배열로 선언 + Swagger 에 배열임을 명시
            @Parameter(
                    description = "반복해서 넣을 수 있는 태그",
                    array = @ArraySchema(schema = @Schema(type = "string"))
            )
            @RequestPart(value = "tags", required = false) String tagsString,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> tags = (tagsString != null && !tagsString.isBlank())
                ? Arrays.stream(tagsString.split(",")).map(String::trim).toList()
                : null;

        return ResponseEntity.ok(communityService.createPost(title, text, tags, images, me));
    }


    // 게시글 수정
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityResponse> update(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("text") String text,
            @RequestPart(value = "tags", required = false) String tagsString,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 콤마로 분리해서 리스트로 변환
        List<String> tags = (tagsString != null && !tagsString.isBlank())
                ? Arrays.stream(tagsString.split(",")).map(String::trim).toList()
                : null;

        return ResponseEntity.ok(communityService.updatePost(id, title, text, tags, images, me));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        communityService.deletePost(id, me);
        return ResponseEntity.noContent().build();
    }

    // 게시글 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getPost(@PathVariable Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.getPostById(id, me));
    }

    // 전체 게시글 목록 (페이징)
    @GetMapping
    public ResponseEntity<Page<CommunityResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword,                     // 제목/내용/태그 공통 검색
            @RequestParam(defaultValue = "latest") String sort                  // latest | popular
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.getAllPosts(page, size, keyword, sort, me));
    }

    // 북마크한 게시글 목록 (검색+정렬+페이징)
    @GetMapping("/bookmarked")
    public ResponseEntity<Page<CommunityResponse>> getBookmarkedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.getBookmarkedPosts(page, size, keyword, sort, me));
    }

    // 내가 작성한 게시글 목록 (검색+정렬+페이징)
    @GetMapping("/mine")
    public ResponseEntity<Page<CommunityResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort  // latest | popular
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(communityService.getMyPosts(page, size, keyword, sort, me));
    }

    // 반응 (좋아요 / 싫어요)
    @PostMapping("/{postId}/reactions")
    public ResponseEntity<Void> toggleReaction(@PathVariable Long postId, @RequestBody @Valid ReactionRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reactionService.toggle(postId, me, request.getType());
        return ResponseEntity.ok().build();
    }

    // 댓글 조회 (페이징)
    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> listComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(commentService.list(postId, page, size));
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId, @RequestBody @Valid CommentRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponse res = commentService.create(postId, me, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 댓글 수정
    @PatchMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable("id") Long commentId, @RequestBody @Valid CommentRequest request) {

        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponse updated = commentService.update(commentId, me, request.getContent());
        return ResponseEntity.ok(updated);
    }

    // 댓글 삭제
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

