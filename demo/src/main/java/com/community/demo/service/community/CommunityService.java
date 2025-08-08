package com.community.demo.service.community;

import com.community.demo.domain.community.CommunityBookmark;
import com.community.demo.domain.community.CommunityImage;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.domain.community.Community;
import com.community.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityImageRepository imageRepository;
    private final CommunityBookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    @Value("${file.dir}")
    private String fileDir;

    // 글 작성
    public CommunityResponse createPost(String title, String text, List<String> tags, List<MultipartFile> images, User me) {
        Community post = new Community(title, text, me);
        if (tags != null) post.setTags(new HashSet<>(tags));
        communityRepository.save(post);

        saveImages(images, post);

        return toResponse(post, me);
    }


    // 글 수정
    @Transactional
    public CommunityResponse updatePost(Long postId, String title, String text, List<String> tags, List<MultipartFile> images, User me) {
        Community post = communityRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("해당 글이 존재하지 않습니다"));

        if (!isOwnerOrAdmin(post, me)) {
            throw new AccessDeniedException("수정 권한이 없습니다");
        }

        post.setTitle(title);
        post.setText(text);
        post.setTags(tags != null ? new HashSet<>(tags) : new HashSet<>());

        // 기존 이미지 제거 후 새로 저장
        imageRepository.deleteByPost(post);
        saveImages(images, post);

        return toResponse(post, me);
    }

    private void saveImages(List<MultipartFile> images, Community post) {
        if (images != null) {
            for (MultipartFile file : images) {
                try {
                    String originalFilename = file.getOriginalFilename();
                    String storedFileName = UUID.randomUUID() + "_" + originalFilename;
                    String fullPath = fileDir + storedFileName;

                    file.transferTo(new File(fullPath));

                    CommunityImage img = new CommunityImage();
                    img.setPost(post);
                    img.setImageUrl("/images/" + storedFileName); // 상대경로
                    imageRepository.save(img);
                } catch (IOException e) {
                    log.error("이미지 저장 실패: {}", e.getMessage());
                }
            }
        }
    }

    // 글 삭제
    @Transactional
    public void deletePost(Long id, User me) {
        Community post = communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("글이 존재하지 않습니다"));

        System.out.println("post.author.id = " + post.getAuthor().getId());
        System.out.println("me.id = " + me.getId());
        System.out.println("me.role = " + me.getRoleType());

        if (!isOwnerOrAdmin(post, me)) {
            throw new AccessDeniedException("삭제 권한이 없습니다");
        }

        //  강제로 자식 엔티티 로딩
        post.getComments().size();
        post.getReactions().size();
        post.getImages().size();
        post.getBookmarks().size();

        communityRepository.delete(post);
    }

    // 전체 조회 (페이징)
//    public Page<CommunityResponse> getAllPosts(int page, User me) {
//        PageRequest pageable = PageRequest.of(page, 6, Sort.by(Sort.Direction.DESC, "updatedAt"));
//        Page<Community> result = communityRepository.findAll(pageable);
//
//        return result.map(post -> toResponse(post, me));
//    }

    public Page<CommunityResponse> getAllPosts(int page, User me) {
        PageRequest pageable = PageRequest.of(page, 6, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Community> result = communityRepository.findAll(pageable);

        return result.map(post -> {
            CommunityResponse response = toResponse(post, me);

            Reaction reaction = reactionRepository.findByPostAndUser(post, me).orElse(null);
            if (reaction != null) {
                response.setMyReaction(reaction.getType().name());
            }

            return response;
        });
    }

    // 단일 글 조회
    public CommunityResponse getPostById(Long id, User me) {
        Community post = communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("글이 존재하지 않습니다"));
        return toResponse(post, me);
    }

    // 내가 북마크한 글
    public List<CommunityResponse> getBookmarkedPosts(User me) {
        List<CommunityBookmark> bookmarks = bookmarkRepository.findByUser(me);
        return bookmarks.stream()
                .map(bookmark -> toResponse(bookmark.getPost(), me))
                .collect(Collectors.toList());
    }

    // 내가 작성한 글
    public List<CommunityResponse> getMyPosts(User me) {
        List<Community> posts = communityRepository.findByAuthor(me);
        return posts.stream()
                .map(post -> toResponse(post, me, null))
                .collect(Collectors.toList());
    }


    // 권한 체크
    private boolean isOwnerOrAdmin(Community post, User user) {
        return post.getAuthor().getId().equals(user.getId()) || user.getRoleType() == RoleType.ADMIN;
    }


    // 기본 버전 - myReaction 없음
    private CommunityResponse toResponse(Community post, User me) {
        return toResponse(post, me, null);
    }

    // 응답 변환
    private CommunityResponse toResponse(Community post, User me, String myReaction) {
        List<String> imageUrls = post.getImages().stream()
                .map(CommunityImage::getImageUrl)
                .toList();

        int commentCount = commentRepository.countByPostId(post.getId());
        boolean isBookmarked = bookmarkRepository.existsByUserAndPost(me, post);

        return new CommunityResponse(
                post.getId(),
                post.getTitle(),
                post.getText(),
                post.getAuthor().getUsername(),
                post.getAuthor().getDepartment(),
                post.getAuthor().getRoleType().name(),
                post.getUpdatedAt(),
                imageUrls,
                new ArrayList<>(post.getTags()),
                post.getLikeCount(),
                post.getDislikeCount(),
                commentCount,
                isBookmarked,
                myReaction
        );
    }

}
