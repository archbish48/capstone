package com.community.demo.service.notice;

import com.community.demo.domain.notice.Bookmark;
import com.community.demo.domain.user.User;
import com.community.demo.dto.bookmark.BookmarkAuthorResponse;
import com.community.demo.repository.BookmarkRepository;
import com.community.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    //  북마크 등록 (작성자 ID 기준)
    public void addBookmark(User me, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("작성자 없음"));

        if (bookmarkRepository.existsByUserAndAuthor(me, author)) {
            throw new IllegalStateException("이미 북마크된 사용자입니다.");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(me);
        bookmark.setAuthor(author);
        bookmarkRepository.save(bookmark);
    }

    //  북마크 취소
    public void removeBookmark(User me, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("작성자 없음"));

        Bookmark bookmark = bookmarkRepository.findByUserAndAuthor(me, author)
                .orElseThrow(() -> new NoSuchElementException("북마크되지 않은 사용자입니다."));

        bookmarkRepository.delete(bookmark);
    }

    //  특정 공지사항 작성자에 대해 북마크했는지 여부
    public boolean isAuthorBookmarked(User me, User author) {
        return bookmarkRepository.existsByUserAndAuthor(me, author);
    }


    //  현재 사용자가 북마크한 모든 작성자 ID 목록
    public Set<Long> getBookmarkedAuthorIds(User me) {
        return bookmarkRepository.findAllByUser(me).stream()
                .map(bookmark -> bookmark.getAuthor().getId())
                .collect(Collectors.toSet());
    }



    // 현재 사용자가 북마크한 모든 작성자의 ID, 이름 목록 리스트 반환
    @Transactional(readOnly = true)
    public List<BookmarkAuthorResponse> getBookmarkedAuthors(User me) {
        // fetch join 또는 @EntityGraph 적용된 메서드 사용
        List<Bookmark> bookmarks = bookmarkRepository.findAllByUser(me);

        // 혹시 모를 중복 방지
        Map<Long, BookmarkAuthorResponse> map = new LinkedHashMap<>();
        for (Bookmark b : bookmarks) {
            var a = b.getAuthor();
            map.putIfAbsent(
                    a.getId(),
                    new BookmarkAuthorResponse(a.getId(), a.getUsername(), a.getDepartment(), a.getProfileImageUrl(), a.getRoleType(),true)
            );
        }
        return new ArrayList<>(map.values());
    }



    // 자동 북마크 등록 매서드
    public void autoSubscribeToDepartmentManager(User user) {
        String department = user.getDepartment();
        if (department == null || department.isBlank()) return;

        // 학과 이름과 동일한 이름의 유저 찾기
        Optional<User> maybeManager = userRepository.findByUsername(department);
        if (maybeManager.isEmpty()) return;

        User manager = maybeManager.get();

        // 자기 자신이면 제외
        if (manager.getId().equals(user.getId())) return;

        // 이미 북마크 되어 있으면 무시
        if (bookmarkRepository.existsByUserAndAuthor(user, manager)) return;

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setAuthor(manager);
        bookmarkRepository.save(bookmark);
    }

}