package com.community.demo.service.chat;

import com.community.demo.dto.chat.ChatRequest;
import com.community.demo.dto.chat.ChatResponse;
import com.community.demo.registry.UserBusyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatProxyService {

    private final WebClient fastApiWebClient;
    private final UserBusyRegistry busy;

    @Value("${app.fastapi.path}")
    private String fastApiChatPath;

    public ChatResponse ask(ChatRequest req, Long userId) {
        long t0 = System.currentTimeMillis();

        // 1) 사용자 락 즉시 획득. 실패하면 바로 423 반환(대기하지 않음)
        if (!busy.tryAcquire(userId)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Chat in progress for this user");
        }

        try {
            // 2) FastAPI는 question만 받음
            FastApiRawResponse res = fastApiWebClient.post()
                    .uri(fastApiChatPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("question", req.getQuestion()))
                    .retrieve()
                    .bodyToMono(FastApiRawResponse.class)
                    .block(); // 요청 1건 내부는 동기 처리

            long latency = System.currentTimeMillis() - t0;
            return new ChatResponse(res != null ? res.answer : null, latency);

        } catch (WebClientResponseException ex) {
            long latency = System.currentTimeMillis() - t0;
            // FastAPI 4xx/5xx → 그대로 메시지 내려주되 상태는 502로 래핑하고 싶다면 여기서 throw
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FastAPI error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - t0;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gateway exception: " + ex.getMessage(), ex);
        } finally {
            // 3) 반드시 해제
            busy.release(userId);
        }
    }

    // FastAPI 응답: { "answer": "..." }
    private static class FastApiRawResponse { public String answer; }
}