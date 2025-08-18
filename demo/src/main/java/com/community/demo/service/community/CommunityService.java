package com.community.demo.service.community;

import com.community.demo.domain.community.CommunityBookmark;
import com.community.demo.domain.community.CommunityImage;
import com.community.demo.domain.community.Reaction;
import com.community.demo.domain.user.RoleType;
import com.community.demo.domain.user.User;
import com.community.demo.dto.community.CommunityResponse;
import com.community.demo.domain.community.Community;
import com.community.demo.dto.community.CommunityUpdateRequest;
import com.community.demo.dto.community.ImageItemResponse;
import com.community.demo.repository.*;
import com.community.demo.service.notice.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
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
    private final FileStorageService fileStorageService;

    // 권한 체크: ADMIN / MANAGER 이면 항상 허용, 아니면 작성자 본인만 허용
    private static final EnumSet<RoleType> CAN_EDIT_ANY =
            EnumSet.of(RoleType.ADMIN, RoleType.MANAGER);


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
    public CommunityResponse updatePost(Long postId,
                                        CommunityUpdateRequest dto,
                                        List<MultipartFile> newImages,
                                        User me) {
        Community post = communityRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("해당 글이 존재하지 않습니다"));

        if (!canModify(post, me)) {
            throw new AccessDeniedException("수정 권한이 없습니다");
        }

        // 1) 부분 수정 (null이 아닐 때만 갱신)
        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getText() != null)  post.setText(dto.getText());
        if (dto.getTags() != null)  post.setTags(new HashSet<>(dto.getTags()));

        // 2) 기존 이미지 중 '삭제 대상'만 제거
        Set<Long> removeIds = new HashSet<>(Optional.ofNullable(dto.getRemoveImageIds()).orElse(List.of()));
        if (!removeIds.isEmpty()) {
            // 순회-삭제 안전하게 복사
            List<CommunityImage> toRemove = post.getImages().stream()
                    .filter(img -> img.getId() != null && removeIds.contains(img.getId()))
                    .toList();

            for (CommunityImage img : toRemove) {
                // (선택) 물리 파일도 삭제하고 싶다면 여기서 삭제
                // String logical = stripPublicPrefix(img.getImageUrl());
                // Files.deleteIfExists(fileStorage.resolve(logical));
                post.removeImage(img);  // orphanRemoval=true면 DB 삭제
            }
        }

        // 3) 새 이미지 추가(있다면) — fileStorageService 기반으로 교체
        if (newImages != null) {
            for (MultipartFile file : newImages) {
                if (file.isEmpty()) continue;

                // 논리 경로 저장 (예: community/images/2025/08/...)
                String logicalPath = fileStorageService.save(file, "community/images");
                String url = "/files/" + logicalPath; // 공지와 동일한 공개 URL 프리픽스

                CommunityImage img = new CommunityImage();
                img.setImageUrl(url);

                // 편의 메서드가 있다면:
                // post.addImage(img);

                // 편의 메서드가 없다면:
                img.setPost(post);
                post.getImages().add(img);
            }
        }

        return toResponse(post, me); // myReaction은 단건 조회엔 포함하지 않는 정책 유지
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

        if (!canModify(post, me)) {
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
            String my = reactionRepository.findByPostIdAndUserId(post.getId(), me.getId())
                    .map(r -> r.getType().name())
                    .orElse(null);
            res.setMyReaction(my);
            return res;
        });
    }

    // 내가 북마크한 글 조회
    @Transactional(readOnly = true)
    public Page<CommunityResponse> getBookmarkedPosts(int page, int size, String keyword, String sort, User me) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Community> result = "popular".equalsIgnoreCase(sort)
                ? communityRepository.searchPopularFlexible(keyword, null, me, true, pageable)
                : communityRepository.searchLatestFlexible(keyword, null, me, true, pageable);

        // 배치로 내 반응 미리 조회
        List<Community> posts = result.getContent();
        Map<Long, String> myReactions = reactionRepository.findByUserAndPostIn(me, posts).stream()
                .collect(Collectors.toMap(r -> r.getPost().getId(), r -> r.getType().name()));

        // Page.map을 그대로 쓰면 content를 다시 읽으므로, 직접 재조립
        List<CommunityResponse> content = posts.stream()
                .map(p -> toResponse(p, me, myReactions.get(p.getId())))
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    // 내가 작성한 글 조회
    @Transactional(readOnly = true)
    public Page<CommunityResponse> getMyPosts(int page, int size, String keyword, String sort, User me) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Community> result = "popular".equalsIgnoreCase(sort)
                ? communityRepository.searchPopularFlexible(keyword, me.getId(), null, false, pageable)
                : communityRepository.searchLatestFlexible(keyword, me.getId(), null, false, pageable);

        List<Community> posts = result.getContent();
        Map<Long, String> myReactions = reactionRepository.findByUserAndPostIn(me, posts).stream()
                .collect(Collectors.toMap(r -> r.getPost().getId(), r -> r.getType().name()));

        List<CommunityResponse> content = posts.stream()
                .map(p -> toResponse(p, me, myReactions.get(p.getId())))
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    // 단일 글 조회
    @Transactional(readOnly = true)
    public CommunityResponse getPostById(Long id, User me) {
        Community post = communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("글이 존재하지 않습니다"));

        String myReaction = reactionRepository.findByPostIdAndUserId(post.getId(), me.getId())
                .map(r -> r.getType().name())
                .orElse(null);

        return toResponse(post, me, myReaction); // 3-인자 버전 호출
    }

    // 현재 유저의 reaction 값 조회 함수
    private String findMyReaction(Community post, User me) {
        return reactionRepository.findByPostAndUser(post, me)
                .map(r -> r.getType().name())
                .orElse(null);
    }



    // 권한 체크
    private boolean canModify(Community post, User user) {
        if (user == null || user.getRoleType() == null) return false;
        if (CAN_EDIT_ANY.contains(user.getRoleType())) return true;
        return Objects.equals(post.getAuthor().getId(), user.getId());
    }




    // 기본 버전 - myReaction 없음
    private CommunityResponse toResponse(Community post, User me) {
        return toResponse(post, me, null);
    }

    // 응답 변환
    private CommunityResponse toResponse(Community post, User me, String myReaction) {
        //  이미지 id+url로 매핑
        List<ImageItemResponse> images = post.getImages().stream()
                .map(img -> new ImageItemResponse(img.getId(), img.getImageUrl()))
                .toList();

        int commentCount = commentRepository.countByPostId(post.getId());
        boolean isBookmarked = bookmarkRepository.existsByUserAndPost(me, post);

        return new CommunityResponse(
                post.getId(),
                post.getTitle(),
                post.getText(),
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getDepartment(),
                post.getAuthor().getRoleType().name(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                images,
                new ArrayList<>(post.getTags()),
                post.getLikeCount(),
                post.getDislikeCount(),
                commentCount,
                isBookmarked,
                myReaction
        );
    }

}
