package com.community.demo.service.user;

import java.time.LocalDate;

public final class GradeSemesterParser {
    private GradeSemesterParser() {}

    // "1학년 2학기" -> [1, 2]
    public static int[] parseLabel(String label) {
        String digits = label.replaceAll("[^0-9 ]", " ").replaceAll("\\s+", " ").trim();
        String[] parts = digits.split(" ");
        if (parts.length < 2) throw new IllegalArgumentException("학년 문자열 형식 오류: " + label);
        int grade = Integer.parseInt(parts[0]);
        int semester = Integer.parseInt(parts[1]);
        if (semester != 1 && semester != 2) throw new IllegalArgumentException("학기는 1 또는 2여야 합니다.");
        if (grade < 1) grade = 1;
        if (grade > 4) grade = 4;
        return new int[]{grade, semester};
    }

    // entryYear/entrySemester 가 있을 때 초기 앵커 값을 구성
    public static Anchor defaultAnchorFromEntry(Integer entryYear, Integer entrySemester) {
        if (entryYear == null) return null;
        int sem = (entrySemester != null && entrySemester == 2) ? 2 : 1;
        LocalDate anchorDate = AcademicTermCalculator.semesterStartDate(entryYear, sem);
        return new Anchor(1, sem, anchorDate); // 입학 시점은 1학년 1or2학기
    }

    public record Anchor(int gradeYear, int semester, LocalDate date) {}
}