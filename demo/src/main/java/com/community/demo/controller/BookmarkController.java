package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.bookmark.BookmarkedAuthorDto;
import com.community.demo.service.notice.BookmarkService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
@SecurityRequirement(name = "JWT")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    //  북마크 등록 ( 유저의 ID 지정해서 해당 유저 북마크)
    @PostMapping("/{authorId}")
    public ResponseEntity<Void> addBookmark(@PathVariable Long authorId) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        bookmarkService.addBookmark(me, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //  북마크 해제 ( 유저의 ID 지정해서 해당 유저 북마크 해제)
    @DeleteMapping("/{authorId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long authorId) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        bookmarkService.removeBookmark(me, authorId);
        return ResponseEntity.noContent().build();
    }

    //  내가 북마크한 모든 작성자 ID 리스트
    @GetMapping
    public ResponseEntity<Set<Long>> getBookmarkedAuthorIds() {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<Long> authorIds = bookmarkService.getBookmarkedAuthorIds(me);
        return ResponseEntity.ok(authorIds);
    }

    // 내가 북마크한 모든 작성자의 ID, 이름 리스트 리턴
    @GetMapping("authors")
    public ResponseEntity<List<BookmarkedAuthorDto>> getBookmarkedAuthors() {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<BookmarkedAuthorDto> authors = bookmarkService.getBookmarkedAuthors(me);
        return ResponseEntity.ok(authors);
    }
}
