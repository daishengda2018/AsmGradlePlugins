package com.mrcd.transform.concurrent;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by im_dsd on 2021/3/26
 */
public class Worker {
    protected final LinkedList<Future<?>> futures = new LinkedList<>();
    protected ExecutorService executor;

    Worker(ExecutorService executor) {
        this.executor = executor;
    }

    public void execute(Runnable runnable) {
        futures.add(executor.submit(runnable));
    }

    public <T> Future<T> submit(Callable<T> callable) {
        Future<T> future = executor.submit(callable);
        futures.add(future);
        return future;
    }

    public void await() throws IOException {
        Future<?> future;
        while ((future = futures.pollFirst()) != null) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }
        }
    }
}
