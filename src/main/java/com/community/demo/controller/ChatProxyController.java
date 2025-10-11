package com.community.demo.controller;

import com.community.demo.domain.user.User;
import com.community.demo.dto.chat.ChatPairDetail;
import com.community.demo.dto.chat.ChatQuestionItem;
import com.community.demo.dto.chat.ChatRequest;
import com.community.demo.dto.chat.ChatResponse;
import com.community.demo.service.chat.ChatProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Tag(name = "Chatbot", description = "Chat 프록시 API (Spring -> FastAPI 동기 호출)")
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatProxyController {

    private final ChatProxyService chatService;

//    @Operation(
//            summary = "질문-응답 동기 처리",
//            description = """
//            프런트는 본 API로 POST하면 됩니다.
//            예시 요청 바디:
//            {
//              "question": "AI융합학부는 어떤 세부 과로 갈라지지?"
//            }
//            """
//    )
//    @PostMapping(
//            value = "/ask",
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE
//    )
//    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
//        var auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !(auth.getPrincipal() instanceof User me)) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
//        }
//        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'question' is required");
//        }
//        return ResponseEntity.ok(chatService.ask(request, ((User) auth.getPrincipal()).getId()));
//    }

    //임시 테스트용
    @PostMapping(
            value = "/ask",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.TEXT_PLAIN_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ChatResponse> ask(
            @RequestBody(required = false) ChatRequest body,            // JSON 본문(선호)
            @RequestParam(value = "question", required = false) String paramQuestion,  // 쿼리/폼 파라미터 대체
            HttpServletRequest req
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User me)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        // 1) JSON 본문에서 먼저 시도
        String q = (body != null) ? body.getQuestion() : null;

        // 2) 쿼리/폼 파라미터로 대체
        if (q == null || q.isBlank()) q = paramQuestion;

        // 3) text/plain 본문으로 마지막 대체 (Postman가 content-type을 text로 보낸 경우)
        if (q == null || q.isBlank()) {
            try {
                String raw = req.getReader().lines().collect(java.util.stream.Collectors.joining("\n")).trim();
                if (!raw.isBlank()) {
                    // 순수 텍스트면 그대로 사용, JSON이면 파싱 시도
                    if (raw.startsWith("{")) {
                        q = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree(raw).path("question").asText(null);
                    } else {
                        q = raw; // text/plain 본문 자체를 질문으로 간주
                    }
                }
            } catch (IOException ignored) {}
        }

        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'question' is required");
        }

        ChatRequest dto = new ChatRequest();
        dto.setQuestion(q);
        return ResponseEntity.ok(chatService.ask(dto, ((User) auth.getPrincipal()).getId()));
    }


    @Operation(summary = "질문 목록(질문만)", description = "최신순, 기본 50개. limit는 1~200")
    @GetMapping("/history")
    public ResponseEntity<List<ChatQuestionItem>> history(
            @RequestParam(defaultValue = "50") int limit
    ) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(chatService.listMyQuestions(me.getId(), limit));
    }

    @Operation(summary = "질문 상세", description = "질문과 답변 1개씩 반환")
    @GetMapping("/history/{id}")
    public ResponseEntity<ChatPairDetail> historyDetail(@PathVariable("id") Long id) {
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(chatService.getMyQuestionDetail(me.getId(), id));
    }
}
