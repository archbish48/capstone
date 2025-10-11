package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.service.notice.CommunityBookmarkService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
@SecurityRequirement(name = "JWT")
public class CommunityBookmarkController {

    private final CommunityBookmarkService bookmarkService;

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<Map<String, Boolean>> toggleBookmark(@PathVariable Long postId) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean bookmarked = bookmarkService.toggle(postId, me);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }
}