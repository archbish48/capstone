package com.community.demo.service.user;

import java.time.LocalDate;
import java.time.ZoneId;

// Asia/Seoul 기준 학기 계산 유틸
public final class AcademicTermCalculator {
    private AcademicTermCalculator() {}

    // 한국 학사 달력 가정: 1학기(3~8월), 2학기(9~2월)
    public static int semesterOfMonth(int month) {
        return (month >= 3 && month <= 8) ? 1 : 2;
    }

    // 학기 시작일(단순화): 1학기=3월1일, 2학기=9월1일
    public static LocalDate semesterStartDate(int year, int semester) {
        return (semester == 1)
                ? LocalDate.of(year, 3, 1)
                : LocalDate.of(year, 9, 1);
    }

    public static LocalDate nowSeoul() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    // 날짜를 "절대 학기 인덱스"로 치환: index = year*2 + (semester-1)
    public static int absoluteSemesterIndex(LocalDate date) {
        int s = semesterOfMonth(date.getMonthValue()); // 1 or 2
        return date.getYear() * 2 + (s - 1);
    }

    // 앵커(학년/학기, 날짜) 기준으로 현재 학년/학기 계산 + 4학년 2학기 캡
    public static Result advanceFromAnchor(int anchorGrade, int anchorSemester, LocalDate anchorDate, LocalDate now) {
        if (now.isBefore(anchorDate)) {
            // 미래 앵커는 허용하지 않음: 그대로 반환
            return clamp(anchorGrade, anchorSemester);
        }

        int idxNow   = absoluteSemesterIndex(now);
        int idxAnchor= absoluteSemesterIndex(anchorDate);
        int elapsed  = Math.max(0, idxNow - idxAnchor); // 경과 학기 수

        int base = (anchorGrade - 1) * 2 + (anchorSemester - 1);
        int curr = base + elapsed;

        int gy = 1 + curr / 2;        // 계산 학년
        int sem = (curr % 2) + 1;     // 계산 학기

        return clamp(gy, sem);
    }

    // 최대 4학년 2학기로 캡
    private static Result clamp(int gradeYear, int semester) {
        if (gradeYear < 1) gradeYear = 1;
        if (semester < 1) semester = 1;

        if (gradeYear > 4) {
            return new Result(4, 2, "4학년 2학기");
        } else if (gradeYear == 4 && semester > 2) {
            return new Result(4, 2, "4학년 2학기");
        } else {
            return new Result(gradeYear, semester, gradeYear + "학년 " + semester + "학기");
        }
    }

    public record Result(int gradeYear, int semester, String label) {}
}