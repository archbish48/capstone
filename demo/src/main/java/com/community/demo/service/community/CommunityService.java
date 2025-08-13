package com.community.demo.service.community;

import com.community.demo.domain.community.CommunityBookmark;
import com.community.demo.domain.community.CommunityImage;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.domain.community.Community;
import com.community.demo.repository.*;
import com.community.demo.service.notice.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

//    @Value("${file.dir}")
//    private String fileDir;

    // 공용 파일 저장 서비스 사용
    private final FileStorageService fileStorage;

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


    // 이미지 저장 로직
    private void saveImages(List<MultipartFile> images, Community post) {
        if (images == null || images.isEmpty()) return;

        for (MultipartFile file : images) {
            try {
                // 논리 경로 예: "community/images/uuid_filename.jpg"
                String logicalPath = fileStorage.save(file, "community/images");
                String url = "/files/" + logicalPath; // 정적 리소스 매핑에 의해 바로 접근 가능

                CommunityImage img = new CommunityImage();
                img.setPost(post);
                img.setImageUrl(url); // 또는 논리경로를 저장하고 응답에서 URL 로 가공해도 됨
                imageRepository.save(img);
            } catch (RuntimeException e) {
                // FileStorageService 내부에서 IOException 등을 RuntimeException 으로 래핑했다고 가정
                log.error("이미지 저장 실패: {}", e.getMessage(), e);
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

        // 주의: 실제 파일 삭제까지 원한다면,
        // post.getImages()를 돌며 fileStorage.resolve(url에서 "/files/" 제거한 논리경로)로 실제 파일을 찾아 삭제하는 로직을 추가할 수 있음.
    }


    // 전체 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<CommunityResponse> getAllPosts(int page, int size, String keyword, String sort, User me) {
        Pageable pageable = PageRequest.of(page, size); // ORDER BY는 JPQL에서 처리
        Page<Community> result = "popular".equalsIgnoreCase(sort)
                ? communityRepository.searchPopularFlexible(keyword, null, null, false, pageable)
                : communityRepository.searchLatestFlexible(keyword, null, null, false, pageable);

        return result.map(post -> {
            CommunityResponse res = toResponse(post, me);
            // getAllPosts에서만 myReaction 세팅
            reactionRepository.findByPostAndUser(post, me).ifPresent(r -> res.setMyReaction(r.getType().name()));
            return res;
        });
    }

    // 내가 북마크한 글
    @Transactional(readOnly = true)
    public Page<CommunityResponse> getBookmarkedPosts(int page, int size, String keyword, String sort, User me) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Community> result = "popular".equalsIgnoreCase(sort)
                ? communityRepository.searchPopularFlexible(keyword, null, me, true, pageable)
                : communityRepository.searchLatestFlexible(keyword, null, me, true, pageable);

        return result.map(post -> toResponse(post, me)); // myReaction 없음(요구사항 유지)
    }

    //내가 작성한 글
    @Transactional(readOnly = true)
    public Page<CommunityResponse> getMyPosts(int page, int size, String keyword, String sort, User me) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Community> result = "popular".equalsIgnoreCase(sort)
                ? communityRepository.searchPopularFlexible(keyword, me.getId(), null, false, pageable)
                : communityRepository.searchLatestFlexible(keyword, me.getId(), null, false, pageable);

        return result.map(post -> toResponse(post, me)); // myReaction 없음(요구사항 유지)
    }

    // 단일 글 조회
    public CommunityResponse getPostById(Long id, User me) {
        Community post = communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("글이 존재하지 않습니다"));
        return toResponse(post, me);
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
                post.getCreatedAt(),
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
