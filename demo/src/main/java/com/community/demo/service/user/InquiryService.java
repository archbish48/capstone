package com.community.demo.service.user;


import com.community.demo.domain.user.Inquiry;
import com.community.demo.domain.user.User;
import com.community.demo.dto.inquiry.InquiryCreateRequest;
import com.community.demo.dto.inquiry.InquiryCreatedResponse;
import com.community.demo.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    @Transactional
    public InquiryCreatedResponse create(User author, InquiryCreateRequest req) {
        Inquiry q = new Inquiry();
        q.setTitle(req.getTitle());
        q.setContent(req.getContent()); // 내용은 저장되지만 일반 사용자 응답에는 포함 X
        q.setAuthor(author);

        // 스냅샷(알림리스트식 표시용)
        q.setAuthorName(author.getUsername());
        q.setAuthorDepartment(author.getDepartment());
        q.setAuthorProfileImageUrl(author.getProfileImageUrl());

        Inquiry saved = inquiryRepository.save(q);

        return new InquiryCreatedResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getAuthorName(),
                saved.getAuthorDepartment(),
                saved.getAuthorProfileImageUrl(),
                saved.getCreatedAt()
        );
    }
}