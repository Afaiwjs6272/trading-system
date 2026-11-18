package ru.ukhanov.spvb.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class InstrumentLockRegistry {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(String seccode, Supplier<T> action) {
        Object lock = locks.computeIfAbsent(seccode, k -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }
}