package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.chat.ChatRequest;
import com.community.demo.dto.chat.ChatResponse;
import com.community.demo.service.chat.ChatProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chatbot", description = "Chat 프록시 API (Spring -> FastAPI 동기 호출)")
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatProxyController {

    private final ChatProxyService chatService;

    @Operation(
            summary = "질문-응답 동기 처리",
            description = """
            프런트는 본 API로 POST하면 됩니다.
            예시 요청 바디:
            {
              "question": "AI융합학부는 어떤 세부 과로 갈라지지?"
            }
            """
    )
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = me.getId();
        return ResponseEntity.ok(chatService.ask(request, userId));
    }
}
