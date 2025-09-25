package com.community.demo.service.user;


import com.community.demo.domain.user.Inquiry;
import com.community.demo.dto.inquiry.InquiryAdminDeleteRequest;
import com.community.demo.dto.inquiry.InquiryAdminListItemResponse;
import com.community.demo.dto.inquiry.InquiryAdminPageResponse;
import com.community.demo.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final PublicUrlResolver url;

    @Transactional(readOnly = true)
    public InquiryAdminPageResponse<InquiryAdminListItemResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> result = inquiryRepository.findAll(pageable);

        var items = result.getContent().stream()
                .map(i -> {
                    String raw = i.getAuthorProfileImageUrl();
                    String abs = url.toAbsolute(raw); // null-safe 변환
                    log.debug("Mapping Inquiry id={} rawProfile='{}' -> absProfile='{}'", i.getId(), raw, abs);

                    return new InquiryAdminListItemResponse(
                            i.getId(),
                            i.getTitle(),
                            i.getContent(),               // ADMIN 이므로 내용 포함
                            i.getAuthorName(),
                            i.getAuthorDepartment(),
                            abs,                          // 절대 URL로 변경해서 넣음
                            i.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        return new InquiryAdminPageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }


    @Transactional
    public void deleteBatch(InquiryAdminDeleteRequest req) {
        // 존재하지 않는 pid 가 섞여 있어도 deleteInBatch 는 조용히 무시 (일괄 삭제 성격)
        inquiryRepository.deleteAllByIdInBatch(req.getPids());
    }
}