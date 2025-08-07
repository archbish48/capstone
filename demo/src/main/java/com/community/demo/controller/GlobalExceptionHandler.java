package com.community.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
//이 코드 주석 해제 시 코드 버전 충돌 남 해제하지 말 것
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {
//        ex.printStackTrace();  // 진짜 콘솔에 찍히게 함
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(Map.of(
//                        "error", ex.getClass().getSimpleName(),
//                        "message", ex.getMessage() != null ? ex.getMessage() : "No message"
//                ));
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
//        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                .body(Map.of("error", "AccessDenied", "message", ex.getMessage()));
//    }
//}