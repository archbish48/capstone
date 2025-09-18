package com.community.demo.service.user;

import com.community.demo.domain.user.User;
import com.community.demo.dto.user.*;
import com.community.demo.repository.UserRepository;
import com.community.demo.service.notice.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return MyProfileResponse.from(user);
    }

    // 유저의 이름, role, 학번,
    public MyBriefProfileResponse getMyBrief(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return MyBriefProfileResponse.from(u); // new 사용 안 함
    }

    // 유저의 전공, 기초전공, 교양필수, 총이수학점, 학점평점 조회
    public CreditInfoResponse getMyCredits(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return CreditInfoResponse.from(u);
    }

    //절대경로로 바꾸기 위한 함수
    private String toPublicUrlCompat(String stored) {
        if (stored == null || stored.isBlank()) return null;
        // 이미 "/files/"로 저장된 과거 데이터도 호환
        if (stored.startsWith("/files/")) return stored;
        return "/files/" + stored; // 표준 공개 URL로 변환
    }



    // ===== 조회 (5단계 캡/앵커 로직 유지, profileImageUrl 포함) =====
    public Object getMyBasicInfo(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 앵커 없으면 entry 기반으로 기본 앵커 구성(기존 로직과 동일)
        if (u.getProgressAnchorGradeYear() == null || u.getProgressAnchorSemester() == null || u.getProgressAnchorDate() == null) {
            LocalDate now = AcademicTermCalculator.nowSeoul();
            u.setProgressAnchorGradeYear(1);
            u.setProgressAnchorSemester(1);
            u.setProgressAnchorDate(now); // 최소 기본값
            // 필요 시 userRepository.save(u);
        }

        var now = AcademicTermCalculator.nowSeoul();
        var result = AcademicTermCalculator.advanceFromAnchor(
                u.getProgressAnchorGradeYear(),
                u.getProgressAnchorSemester(),
                u.getProgressAnchorDate(),
                now
        );

        boolean hasMinor = StringUtils.hasText(u.getMinorDepartment());
        boolean hasDouble = StringUtils.hasText(u.getDoubleMajorDepartment());
        if (hasMinor && hasDouble) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "부전공과 복수전공은 동시에 설정할 수 없습니다.");
        }

        String img = toPublicUrlCompat(u.getProfileImageUrl()); //추가한 코드


        if (hasMinor) {
            return BasicInfoWithMinorResponse.builder()
                    .username(u.getUsername())
                    .department(u.getDepartment())
                    .studentNumber(u.getStudent_number())
                    .gradeYear(result.gradeYear())
                    .semester(result.semester())
                    .gradeLabel(result.label())
                    .minor(u.getMinorDepartment())
                    .profileImageUrl(img)
                    .build();
        }
        if (hasDouble) {
            return BasicInfoWithDoubleMajorResponse.builder()
                    .username(u.getUsername())
                    .department(u.getDepartment())
                    .studentNumber(u.getStudent_number())
                    .gradeYear(result.gradeYear())
                    .semester(result.semester())
                    .gradeLabel(result.label())
                    .doubleMajor(u.getDoubleMajorDepartment())
                    .profileImageUrl(img)
                    .build();
        }
        // 둘 다 없으면 부전공 포맷으로 null
        return BasicInfoWithMinorResponse.builder()
                .username(u.getUsername())
                .department(u.getDepartment())
                .studentNumber(u.getStudent_number())
                .gradeYear(result.gradeYear())
                .semester(result.semester())
                .gradeLabel(result.label())
                .minor(null)
                .profileImageUrl(img)
                .build();
    }

    // ===== 수정 =====
    public Object updateMyInfo(Long userId, UpdateMyInfoRequest req, MultipartFile profileImage) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 1) 기본 문자열 필드 업데이트 (null이면 기존 유지, 빈문자면 비우기)
        if (req.getUsername() != null)        u.setUsername(blankToNull(req.getUsername()));
        if (req.getDepartment() != null)      u.setDepartment(blankToNull(req.getDepartment()));
        if (req.getStudentNumber() != null)   u.setStudent_number(blankToNull(req.getStudentNumber()));

        // 2) 학년 라벨 → 앵커 갱신 (요청이 왔을 때만)
        if (StringUtils.hasText(req.getGradeLabel())) {
            int[] parsed = GradeSemesterParser.parseLabel(req.getGradeLabel()); // [grade, semester]
            int gy = clampGrade(parsed[0]);
            int sem = clampSemester(parsed[1]);
            u.setProgressAnchorGradeYear(gy);
            u.setProgressAnchorSemester(sem);
            u.setProgressAnchorDate(AcademicTermCalculator.nowSeoul()); // 지금을 앵커로
        }

        // 3) 부전공/복수전공 상호배타 업데이트
        boolean hasMinor  = StringUtils.hasText(req.getMinor());
        boolean hasDouble = StringUtils.hasText(req.getDoubleMajor());
        if (hasMinor && hasDouble) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "부전공과 복수전공은 동시에 설정할 수 없습니다.");
        }
        if (req.getMinor() != null) { // 빈문자면 null로 저장(비우기 허용)
            u.setMinorDepartment(toNullIfBlank(req.getMinor()));
            if (StringUtils.hasText(req.getMinor())) {
                u.setDoubleMajorDepartment(null);
            }
        }
        if (req.getDoubleMajor() != null) {
            u.setDoubleMajorDepartment(toNullIfBlank(req.getDoubleMajor()));
            if (StringUtils.hasText(req.getDoubleMajor())) {
                u.setMinorDepartment(null);
            }
        }

        // 4) 프로필 이미지 저장 (있을 때만)
        if (profileImage != null && !profileImage.isEmpty()) {
            String storagePath = fileStorageService.save(profileImage, "profiles/" + u.getId());
            // 혹시 "/files/"가 앞에 붙어 넘어오는 경우 제거
            if (storagePath.startsWith("/files/")) {
                storagePath = storagePath.substring("/files/".length());
            }
            u.setProfileImageUrl(storagePath);  // DB에는 "profiles/5/증명사진.jpg" 형태로 저장
        }

        userRepository.save(u);

        // 5) 갱신 후 조회 DTO로 반환 (현재 시각 기준 자동 계산 + 캡)
        return getMyBasicInfo(userId);
    }

    private static String toNullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
    private static String blankToNull(String s) {
        return (s != null && s.isBlank()) ? null : s;
    }
    private static int clampGrade(int g) {
        if (g < 1) return 1;
        return Math.min(g, 4);
    }
    private static int clampSemester(int s) {
        return (s == 2) ? 2 : 1;
    }

    // 사용자가 현재 학년/학기를 수정하면 그 시점을 새 앵커로 기록
    public void updateMyAnchor(Long userId, int gradeYear, int semester, LocalDate anchorDate) {
        if (gradeYear < 1) gradeYear = 1;
        if (gradeYear > 4) gradeYear = 4;
        if (semester != 1 && semester != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학기는 1 또는 2여야 합니다.");
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        u.setProgressAnchorGradeYear(gradeYear);
        u.setProgressAnchorSemester(semester);
        u.setProgressAnchorDate(anchorDate);
        userRepository.save(u);
    }

    private Integer tryDeriveEntryYearFromStudentNumber(String studentNumber) {
        if (!StringUtils.hasText(studentNumber)) return null;
        String s = studentNumber.trim();
        if (s.length() >= 4 && s.substring(0, 4).chars().allMatch(Character::isDigit)) {
            try {
                return Integer.parseInt(s.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}