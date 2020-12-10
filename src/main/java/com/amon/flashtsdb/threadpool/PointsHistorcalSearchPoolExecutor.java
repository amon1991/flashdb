package com.amon.flashtsdb.threadpool;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/26.
 */
public final class PointsHistorcalSearchPoolExecutor {

    private static AtomicInteger threadNum = new AtomicInteger(0);

    private static final ExecutorService executorService = new ThreadPoolExecutor(
            50,
            50,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("PointsHistorcalSearch-thread-" + UUID.randomUUID().toString() + "-" + threadNum.incrementAndGet());
                    if (t.isDaemon()) {
                        t.setDaemon(false);
                    }
                    if (Thread.NORM_PRIORITY != t.getPriority()) {
                        t.setPriority(Thread.NORM_PRIORITY);
                    }
                    return t;
                }
            });

    private PointsHistorcalSearchPoolExecutor() {
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
