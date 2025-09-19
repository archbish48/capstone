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

        // 부전공/복수전공 보유 여부 판단(학과명 또는 학점/기준 중 하나라도 있으면 포함)
        boolean hasMinor =
                hasText(u.getMinorDepartment())
                        || nz(u.getCreditsMinor()) > 0
                        || nz(u.getCreditsMinorBasicMajor()) > 0
                        || nz(u.getCreditsMinorMinimumRequired()) > 0
                        || u.getReqMinorBasicMajor() != null
                        || u.getReqMinorMinimumRequired() != null;

        boolean hasDouble =
                hasText(u.getDoubleMajorDepartment())
                        || nz(u.getCreditsDoubleMajor()) > 0
                        || nz(u.getCreditsDoubleBasicMajor()) > 0
                        || nz(u.getCreditsDoubleMinimumRequired()) > 0
                        || u.getReqDoubleBasicMajor() != null
                        || u.getReqDoubleMinimumRequired() != null;

        // 둘 다 채워지는 설계 위반 방지(원하면 유지/삭제)
        if (hasMinor && hasDouble) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "부전공과 복수전공 항목은 동시에 설정할 수 없습니다.");
        }

        CreditInfoResponse.CreditInfoResponseBuilder b = CreditInfoResponse.builder()
                // 취득학점(기본)
                .majorCredits(nz(u.getCreditsMajor()))
                .basicMajorCredits(nz(u.getCreditsBasicMajor()))
                .generalRequiredCredits(nz(u.getCreditsGeneralRequired()))
                .totalCredits(nz(u.getCreditsTotal()))
                .gpa(u.getGpa())
                // 이수기준(기본)
                .requiredGeneralRequiredCredits(u.getReqGeneralRequired())
                .requiredBasicMajorCredits(u.getReqBasicMajor())
                .requiredSingleMajorMinimumCredits(u.getReqSingleMajorMinimumRequired())
                .requiredGraduationTotal(u.getReqGraduationTotal())
                .transferRecognized(u.getTransferRecognized());

        if (hasMinor) {
            b.minorCredits(nullIfZero(u.getCreditsMinor()))
                    .minorBasicMajorCredits(nullIfZero(u.getCreditsMinorBasicMajor()))
                    .minorMinimumRequiredCredits(nullIfZero(u.getCreditsMinorMinimumRequired()))
                    .requiredMinorBasicMajorCredits(u.getReqMinorBasicMajor())
                    .requiredMinorMinimumRequiredCredits(u.getReqMinorMinimumRequired());
        }

        if (hasDouble) {
            b.doubleMajorCredits(nullIfZero(u.getCreditsDoubleMajor()))
                    .doubleBasicMajorCredits(nullIfZero(u.getCreditsDoubleBasicMajor()))
                    .doubleMinimumRequiredCredits(nullIfZero(u.getCreditsDoubleMinimumRequired()))
                    .requiredDoubleBasicMajorCredits(u.getReqDoubleBasicMajor())
                    .requiredDoubleMinimumRequiredCredits(u.getReqDoubleMinimumRequired());
        }

        return b.build();
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }
    private static Integer nullIfZero(Integer v) {
        if (v == null) return null;
        return v == 0 ? null : v;
    }
    private static boolean hasText(String s) { return s != null && !s.isBlank(); }

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
    public Object updateMyInfo(Long userId,
                               UpdateMyInfoRequest req,
                               MultipartFile profileImage,
                               boolean minorPresent, boolean doubleMajorPresent,
                               boolean minorExplicitNull, boolean doubleMajorExplicitNull) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // (생략) 이름/학과/학번/gradeLabel → 앵커 갱신 로직은 기존 그대로

        // --- 부전공/복수전공 업데이트 ---
        // 규칙:
        //  - 키가 없으면 아무 것도 하지 않음(유지)
        //  - 키가 있고 값이 null이면 해당 필드만 null로 초기화
        //  - 키가 있고 값이 문자열(공백 제외)이면 해당 값으로 설정하고 반대편 필드는 null
        //  - 동시에 둘 다 "값"을 보내면 400
        if (minorPresent) {
            if (minorExplicitNull) {
                u.setMinorDepartment(null);                 // 명시적 삭제
                // 반대편 유지(요구사항대로)
            } else if (org.springframework.util.StringUtils.hasText(req.getMinor())) {
                u.setMinorDepartment(req.getMinor().trim()); // 값 설정
                u.setDoubleMajorDepartment(null);            // 상호배타
            } // else: 빈 문자열이면 무시(유지)
        }
        if (doubleMajorPresent) {
            if (doubleMajorExplicitNull) {
                u.setDoubleMajorDepartment(null);           // 명시적 삭제
                // 반대편 유지
            } else if (org.springframework.util.StringUtils.hasText(req.getDoubleMajor())) {
                u.setDoubleMajorDepartment(req.getDoubleMajor().trim());
                u.setMinorDepartment(null);                 // 상호배타
            } // else: 빈 문자열이면 무시(유지)
        }

        // 둘 다 '값'을 동시에 보낸 경우 막기(명시적 null은 허용)
        if (minorPresent && doubleMajorPresent
                && !minorExplicitNull && !doubleMajorExplicitNull
                && org.springframework.util.StringUtils.hasText(req.getMinor())
                && org.springframework.util.StringUtils.hasText(req.getDoubleMajor())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "부전공과 복수전공은 동시에 설정할 수 없습니다.");
        }

        // --- 프로필 이미지 ---
        // 요구사항 유지: 파일 파트가 없거나 비어 있으면 삭제(null), 있으면 저장
        if (profileImage == null || profileImage.isEmpty()) {
            u.setProfileImageUrl(null); // DB 삭제
        } else {
            String storagePath = fileStorageService.save(profileImage, "profiles/" + u.getId());
            if (storagePath.startsWith("/files/")) {
                storagePath = storagePath.substring("/files/".length());
            }
            u.setProfileImageUrl(storagePath);
        }

        userRepository.save(u);
        return getMyBasicInfo(userId); // 응답은 기존 조회 포맷
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