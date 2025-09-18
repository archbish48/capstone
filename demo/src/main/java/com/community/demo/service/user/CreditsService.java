package com.community.demo.service.user;

import com.community.demo.domain.user.User;
import com.community.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.*;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditsService {

    private final WebClient fastApiWebClient;
    private final UserRepository userRepository;

    @Value("${app.fastapi.ocr-path:/ocr/extract-credits}")
    private String ocrPath;

    /**
     * 1) 파일을 FastAPI로 그대로 multipart 전송
     * 2) 응답 JSON을 사용자 전공 상태에 맞게 키명만 변경
     * 3) "학점평점": null 추가
     * 4) 주요 값들을 User DB에 저장(교양필수/기초전공/총 취득학점)
     * 5) 가공된 JSON을 반환
     */
    @Transactional
    public Map<String, Object> forwardToOcrAndSave(User me, MultipartFile pdf) {
        Map<String, Object> raw = callOcr(pdf);                 // FastAPI 원본 응답
        Map<String, Object> transformed = transformKeys(me, raw); // 키명 변경 + 학점평점 추가
        persistCoreFields(me, transformed);                     // DB 반영
        return transformed;
    }

    /**
     * 사용자가 수정/입력한 JSON(특히 학점평점)을 DB에 반영
     * - 프런트가 upload 응답을 편집해 보내는 걸 가정
     */
    @Transactional
    public void applyEditedPayload(User me, Map<String, Object> edited) {
        Integer gReq  = getNestedInt(edited, "교양 필수", "취득학점");
        if (gReq != null) me.setCreditsGeneralRequired(gReq);

        Integer basic = getNestedInt(edited, "기초전공", "취득학점");
        if (basic != null) me.setCreditsBasicMajor(basic);

        Integer total = getTopLevelInt(edited, "취득학점");
        if (total != null) me.setCreditsTotal(total);

        // [ADD] majorCredits 매핑
        Integer major = getNestedInt(edited, "단일전공자 최소전공이수학점", "취득학점");
        if (major != null) me.setCreditsMajor(major);

        BigDecimal gpa = getTopLevelBigDecimal(edited, "학점평점");
        if (gpa != null) me.setGpa(gpa);

        userRepository.save(me);
    }

    /* ========== 내부 유틸/핵심 로직 ========== */

    private Map<String, Object> callOcr(MultipartFile pdf) {
        try {
            MultipartBodyBuilder mb = new MultipartBodyBuilder();
            mb.part("file", pdf.getResource())
                    .filename(Objects.requireNonNullElse(pdf.getOriginalFilename(), "transcript.pdf"))
                    .contentType(MediaType.APPLICATION_PDF);

            MultiValueMap<String, HttpEntity<?>> parts = mb.build();

            return fastApiWebClient.post()
                    .uri(ocrPath)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromMultipartData(parts))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("OCR 서버 오류: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }


    private enum Track { NONE, MINOR, DOUBLE }

    /** 사용자 전공 상태에 따라 키명 변경 + "학점평점": null 추가 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> transformKeys(User me, Map<String, Object> raw) {
        if (raw == null) throw new IllegalStateException("OCR 응답이 비었습니다.");

        Track track = detectTrack(me.getMinorDepartment(), me.getDoubleMajorDepartment());

        // 로그로 실제 값/판정을 찍어 원인 파악
        log.debug("[CREDITS] minor='{}', double='{}' -> track={}",
                me.getMinorDepartment(), me.getDoubleMajorDepartment(), track);

        final String MIX_BASIC_KEY = "복수,부,연계전공 기초전공";
        final String MIX_MIN_KEY   = "복수,부,연계전공 최소전공이수학점";

        // 원본 복사(순서 유지)
        Map<String, Object> out = new LinkedHashMap<>();
        raw.forEach(out::put);

        Object mixBasicVal = out.get(MIX_BASIC_KEY);
        Object mixMinVal   = out.get(MIX_MIN_KEY);

        switch (track) {
            case NONE -> {
                // ✅ 둘 다 없음 → 복합 키를 아예 제거
                out.remove(MIX_BASIC_KEY);
                out.remove(MIX_MIN_KEY);
            }
            case MINOR -> {
                // ✅ 부전공만 있음 → 부전공 키로 대체
                out.remove(MIX_BASIC_KEY);
                out.remove(MIX_MIN_KEY);
                out.put("부전공 기초전공", ensureNode(mixBasicVal));
                out.put("부전공 최소전공이수학점", ensureNode(mixMinVal));
            }
            case DOUBLE -> {
                // ✅ 복수전공만 있음 → 복수전공 키로 대체
                out.remove(MIX_BASIC_KEY);
                out.remove(MIX_MIN_KEY);
                out.put("복수전공 기초전공", ensureNode(mixBasicVal));
                out.put("복수전공 최소전공이수학점", ensureNode(mixMinVal));
            }
        }

        // 공통: 학점평점 키는 항상 존재(없으면 null)
        out.putIfAbsent("학점평점", null);

        return out;
    }

    private Track detectTrack(String minor, String dbl) {
        String m = norm(minor);
        String d = norm(dbl);

        if (m == null && d == null) return Track.NONE;
        if (m != null && d == null) return Track.MINOR;
        if (m == null && d != null) return Track.DOUBLE;

        // 이론상 둘 다 채워지지 않지만, 방어적으로 우선순위 정함(원하면 Exception으로 바꿔도 됨)
        log.warn("[CREDITS] Both minor and double major are set. Falling back to DOUBLE.");
        return Track.DOUBLE;
    }

    private String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        // 빈 문자열 외에 ‘null’, ‘없음’, ‘미선택’ 같은 값도 없음으로 간주 (필요 시 추가)
        String lower = t.toLowerCase();
        if (lower.equals("null") || lower.equals("없음") || lower.equals("미선택")) return null;
        return t;
    }

    /** FastAPI가 해당 키를 안 주거나 null이면 {이수기준:null, 취득학점:null} 기본 노드로 채워줌 */
    private Map<String, Object> ensureNode(Object val) {
        Map<String, Object> node = new LinkedHashMap<>();

        if (val instanceof Map<?, ?> m) {
            // 키를 String으로 강제 변환해 안전하게 복사
            m.forEach((k, v) -> node.put(String.valueOf(k), v));
            // 필요한 키가 없으면 기본값 채우기
            node.putIfAbsent("이수기준", null);
            node.putIfAbsent("취득학점", null);
            return node;
        }

        // 값이 없거나 맵이 아니면 기본 구조로 리턴
        node.put("이수기준", null);
        node.put("취득학점", null);
        return node;
    }

    /** 업로드 응답 직후 DB에 반영할 핵심 필드 저장 */
    private void persistCoreFields(User me, Map<String, Object> transformed) {
        Integer gReq  = getNestedInt(transformed, "교양 필수", "취득학점");
        Integer basic = getNestedInt(transformed, "기초전공", "취득학점");
        Integer total = getTopLevelInt(transformed, "취득학점");

        // [ADD] 전공 이수학점 = "단일전공자 최소전공이수학점"의 "취득학점"
        Integer major = getNestedInt(transformed, "단일전공자 최소전공이수학점", "취득학점");

        if (gReq != null)  me.setCreditsGeneralRequired(gReq);
        if (basic != null) me.setCreditsBasicMajor(basic);
        if (total != null) me.setCreditsTotal(total);

        // [ADD]
        if (major != null) me.setCreditsMajor(major);

        userRepository.save(me);
    }

    /* ===== 파서 유틸 ===== */

    @SuppressWarnings("unchecked")
    private Integer getNestedInt(Map<String, Object> map, String parentKey, String childKey) {
        Object obj = map.get(parentKey);
        if (obj instanceof Map<?, ?> m) {
            Object v = ((Map<?, ?>) m).get(childKey);
            if (v == null) return null;
            if (v instanceof Number n) return n.intValue();
            try { return Integer.valueOf(String.valueOf(v)); } catch (Exception ignore) {}
        }
        return null;
    }

    private Integer getTopLevelInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(v)); } catch (Exception ignore) {}
        return null;
    }

    private BigDecimal getTopLevelBigDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception ignore) {}
        return null;
    }
}