package com.community.demo.service.user;


import com.community.demo.domain.user.Inquiry;
import com.community.demo.domain.user.User;
import com.community.demo.dto.inquiry.InquiryCreateRequest;
import com.community.demo.dto.inquiry.InquiryCreatedResponse;
import com.community.demo.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final PublicUrlResolver url;

    @Transactional
    public InquiryCreatedResponse create(User author, InquiryCreateRequest req) {
        Inquiry q = new Inquiry();
        q.setTitle(req.getTitle());
        q.setContent(req.getContent()); // 내용은 저장되지만 일반 사용자 응답에는 포함 X
        q.setAuthor(author);

        // 스냅샷(알림리스트식 표시용) — DB 에는 상대경로(원본값)를 그대로 저장
        q.setAuthorName(author.getUsername());
        q.setAuthorDepartment(author.getDepartment());
        q.setAuthorProfileImageUrl(author.getProfileImageUrl());

        Inquiry saved = inquiryRepository.save(q);

        // 응답에서는 절대 URL로 변환해서 내려줌 (null 안전)
        String absProfile = url.toAbsolute(saved.getAuthorProfileImageUrl());

        return new InquiryCreatedResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getAuthorName(),
                saved.getAuthorDepartment(),
                absProfile,              // <- 절대 URL 또는 null
                saved.getCreatedAt()
        );
    }
}