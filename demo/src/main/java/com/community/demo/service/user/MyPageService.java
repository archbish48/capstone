package com.community.demo.service.user;

import com.community.demo.domain.user.User;
import com.community.demo.dto.user.MyBriefProfileResponse;
import com.community.demo.dto.user.MyProfileResponse;
import com.community.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;

    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return MyProfileResponse.from(user);
    }

    public MyBriefProfileResponse getMyBrief(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return MyBriefProfileResponse.from(user);
    }
}