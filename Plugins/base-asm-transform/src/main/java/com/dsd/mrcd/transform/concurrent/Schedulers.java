package com.dsd.mrcd.transform.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Schedulers {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(
            0,
            CPU_COUNT * 3,
            30L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(@NotNull Runnable runnable) {
                    return new Thread(runnable, "transform-schedulers");
                }
            });

    public static Worker newWorker() {
        return new Worker(EXECUTOR);
    }
}
