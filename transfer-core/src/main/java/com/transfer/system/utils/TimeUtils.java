package com.transfer.system.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TimeUtils {
    private static final java.time.ZoneId KST = java.time.ZoneId.of("Asia/Seoul");
    private TimeUtils(){}

    /**
     * 현재 시간을 LocalDateTime KST로 반환
     */
    public static LocalDateTime nowKstLocalDateTime() {
        return java.time.ZonedDateTime.now(KST).toLocalDateTime();
    }

    /**
     * 현재 시간을 LocalDate KST로 반환
     */
    public static LocalDate nowKstLocalDate() {
        return java.time.ZonedDateTime.now(KST).toLocalDate();
    }

    /**
     * 오늘의 시작 시간을 KST로 반환
     */
    public static LocalDateTime startOfTodayKst() {
        return java.time.ZonedDateTime.now(KST).toLocalDate().atStartOfDay();
    }

    /**
     * 오늘의 끝 시간을 KST로 반환
     */
    public static LocalDateTime endOfTodayKst() {
        return startOfTodayKst().plusDays(1).minusNanos(1);
    }
}