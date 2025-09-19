package com.community.demo.repository;

import com.community.demo.domain.user.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
