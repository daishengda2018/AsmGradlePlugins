package com.mrcd.transform.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by im_dsd on 2021/3/26
 */
public class Schedulers {
    private static final int cpuCount = Runtime.getRuntime().availableProcessors();
    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(
            0, cpuCount * 3,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    public static Worker newWorker() {
        return new Worker(EXECUTOR);
    }
}
