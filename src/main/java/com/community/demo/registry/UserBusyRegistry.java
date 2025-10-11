package com.community.demo.registry;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
public class UserBusyRegistry {
    private final ConcurrentHashMap<Long, Semaphore> gates = new ConcurrentHashMap<>();

    private Semaphore gateOf(Long userId) {
        return gates.computeIfAbsent(userId, id -> new Semaphore(1, true)); // permit=1, 공정
    }

    /** 사용자가 바쁜 상태인지 */
    public boolean isBusy(Long userId) {
        return gateOf(userId).availablePermits() == 0;
    }

    /** 즉시 획득; 실패 시 false (→ 바로 거부) */
    public boolean tryAcquire(Long userId) {
        return gateOf(userId).tryAcquire();
    }

    /** 반드시 finally에서 release */
    public void release(Long userId) {
        gateOf(userId).release();
    }
}