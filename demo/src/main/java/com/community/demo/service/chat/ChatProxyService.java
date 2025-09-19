package com.community.demo.service.chat;

import com.community.demo.domain.user.ChatQA;
import com.community.demo.domain.user.User;
import com.community.demo.dto.chat.ChatPairDetail;
import com.community.demo.dto.chat.ChatQuestionItem;
import com.community.demo.dto.chat.ChatRequest;
import com.community.demo.dto.chat.ChatResponse;
import com.community.demo.registry.UserBusyRegistry;
import com.community.demo.repository.ChatQARepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatProxyService {

    private final WebClient fastApiWebClient;
    private final UserBusyRegistry busy;
    private final ChatQARepository chatQARepo;
    private final EntityManager em;

    @Value("${app.fastapi.path}")
    private String fastApiChatPath;

    public ChatResponse ask(ChatRequest req, Long userId) {
        long t0 = System.currentTimeMillis();

        if (!busy.tryAcquire(userId)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Chat in progress for this user");
        }
        try {
            String raw = fastApiWebClient.post()
                    .uri(fastApiChatPath) // "/api/v1/chat/chat"
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(new FastApiQuestion(req.getQuestion()))
                    .exchangeToMono(res -> {
                        if (res.statusCode().is2xxSuccessful()) {
                            return res.bodyToMono(String.class);
                        } else {
                            return res.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_GATEWAY,
                                            "FastAPI " + res.statusCode() + " body=" + body)));
                        }
                    })
                    .block();

            long latency = System.currentTimeMillis() - t0;

            // JSON 파싱
            var mapper = new ObjectMapper();
            String answer;
            try {
                answer = mapper.readTree(raw).path("answer").asText(null);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "FastAPI returned non-JSON or unexpected JSON: " + raw, e);
            }
            if (answer == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "FastAPI JSON missing 'answer' field: " + raw);
            }

            // 저장
            var userRef = em.getReference(User.class, userId);
            chatQARepo.save(new ChatQA(userRef, req.getQuestion(), answer, latency));

            return new ChatResponse(answer, latency);

        } catch (io.netty.handler.timeout.ReadTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI read timeout", e);
        } catch (WebClientRequestException e) {
            // 연결 자체가 안될 때 (호스트/포트/도커 네트워크 오류)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cannot connect to FastAPI: " + e.getMessage(), e);
        } finally {
            busy.release(userId);
        }
    }

    // === 질문 목록(질문만) ===
    public List<ChatQuestionItem> listMyQuestions(Long userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 200); // 1~200 사이로 고정
        return chatQARepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size))
                .map(q -> new ChatQuestionItem(q.getId(), q.getQuestion(), q.getCreatedAt()))
                .getContent();
    }

    // === 특정 질문의 상세(질문+답변) ===
    public ChatPairDetail getMyQuestionDetail(Long userId, Long qaId) {
        var qa = chatQARepo.findByIdAndUserId(qaId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        return new ChatPairDetail(qa.getId(), qa.getQuestion(), qa.getAnswer(), qa.getCreatedAt(), qa.getLatencyMs());
    }

    //private static class FastApiRawResponse { public String answer; }
    private static class FastApiQuestion {
        public String question;
        FastApiQuestion(String q) { this.question = q; }
    }
}

