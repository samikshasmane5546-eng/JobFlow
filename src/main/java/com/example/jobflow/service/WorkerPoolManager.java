package com.example.jobflow.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class WorkerPoolManager {

    private final JobService jobService;

    private static final int POOL_SIZE = 5;
    private static final int BATCH_SIZE = 1;
    private static final long POLL_INTERVAL_MS = 1000;

    private ExecutorService executorService;
    private volatile boolean shuttingDown = false; // NEW

    @PostConstruct
    public void startWorkers() {
        executorService = Executors.newFixedThreadPool(POOL_SIZE);

        for (int i = 0; i < POOL_SIZE; i++) {
            executorService.submit(this::workerLoop);
        }

        System.out.println("Started " + POOL_SIZE + " worker threads.");
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted() && !shuttingDown) { // NEW check
            try {
                jobService.processNextBatch(BATCH_SIZE);
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Worker error: " + e.getMessage());
            }
        }
        System.out.println(Thread.currentThread().getName() + " shut down cleanly.");
    }

    @PreDestroy // NEW — runs automatically when Spring context shuts down
    public void stopWorkers() {
        System.out.println("Shutdown signal received — stopping workers gracefully...");
        shuttingDown = true;

        executorService.shutdown(); // stop accepting new tasks, let running ones finish current loop iteration

        try {
            // give in-flight jobs up to 30s to finish their current job before forcing shutdown
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Workers did not finish in time — forcing shutdown.");
                executorService.shutdownNow();
            } else {
                System.out.println("All workers finished cleanly.");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}