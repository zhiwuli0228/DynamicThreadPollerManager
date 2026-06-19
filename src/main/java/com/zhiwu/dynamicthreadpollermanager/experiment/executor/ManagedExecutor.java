package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ManagedExecutor implements AutoCloseable {

    /**
     * Practical upper bound for pool size and concurrency to prevent
     * accidental resource exhaustion.
     */
    public static final int MAX_POOL_SIZE = 10_000;

    // ----- PLATFORM mode fields -----
    private final ThreadPoolExecutor platformExecutor;
    private final int platformQueueCapacity;

    // ----- VIRTUAL mode fields -----
    private final ExecutorService virtualExecutor;
    private final Semaphore semaphore;
    private final BlockingQueue<Runnable> pendingQueue;
    private volatile RejectedExecutionHandler virtualRejectionHandler;
    private final AtomicLong virtualCompletedTaskCount;
    private final AtomicLong virtualSubmittedTaskCount;
    private final AtomicInteger peakConcurrency;
    private volatile int virtualMaxConcurrency;
    private volatile boolean virtualShutdown;
    private volatile Thread drainerThread;
    private volatile boolean drainerPaused;
    private final long virtualKeepAliveTimeMs;

    // ----- Common -----
    private final ThreadMode threadMode;
    private final java.util.concurrent.atomic.AtomicLong rejectedTaskCount;
    private volatile RejectedExecutionHandler platformRejectionPolicy;
    private volatile RejectedExecutionHandler virtualRejectionPolicyOriginal;

    // ============================================================
    // PLATFORM constructors (unchanged API)
    // ============================================================

    public ManagedExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                           TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    public ManagedExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                           TimeUnit unit, BlockingQueue<Runnable> workQueue,
                           ThreadFactory threadFactory, RejectedExecutionHandler rejectionHandler) {
        Objects.requireNonNull(unit, "time unit must not be null");
        Objects.requireNonNull(workQueue, "workQueue must not be null");
        Objects.requireNonNull(threadFactory, "threadFactory must not be null");
        Objects.requireNonNull(rejectionHandler, "rejectionHandler must not be null");
        if (corePoolSize < 1) {
            throw new IllegalArgumentException("corePoolSize must be >= 1, was " + corePoolSize);
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maxPoolSize must be >= corePoolSize, was " + maxPoolSize);
        }
        if (maxPoolSize > MAX_POOL_SIZE) {
            throw new IllegalArgumentException(
                    "maxPoolSize must be <= " + MAX_POOL_SIZE + ", was " + maxPoolSize);
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("keepAliveTime must be >= 0, was " + keepAliveTime);
        }
        ThreadMode detected = detectThreadMode(threadFactory);
        if (detected == ThreadMode.VIRTUAL) {
            throw new IllegalArgumentException(
                    "Virtual thread factories are not supported in platform constructors. "
                    + "Use ManagedExecutor.virtual() instead.");
        }
        this.rejectedTaskCount = new java.util.concurrent.atomic.AtomicLong(0);
        this.platformRejectionPolicy = rejectionHandler;
        RejectedExecutionHandler countingHandler = (Runnable r, ThreadPoolExecutor executor) -> {
            rejectedTaskCount.incrementAndGet();
            rejectionHandler.rejectedExecution(r, executor);
        };
        this.platformExecutor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, unit,
                workQueue, threadFactory, countingHandler);
        this.platformQueueCapacity = workQueue.remainingCapacity() + workQueue.size();
        this.threadMode = ThreadMode.PLATFORM;

        this.virtualExecutor = null;
        this.semaphore = null;
        this.pendingQueue = null;
        this.virtualRejectionHandler = null;
        this.virtualCompletedTaskCount = null;
        this.virtualSubmittedTaskCount = null;
        this.peakConcurrency = null;
        this.virtualMaxConcurrency = 0;
        this.virtualKeepAliveTimeMs = 0;
    }

    // ============================================================
    // VIRTUAL constructor (private, called by factory)
    // ============================================================

    private ManagedExecutor(int maxConcurrency, int queueCapacity,
                            long keepAliveTimeMs, RejectedExecutionHandler rejectionHandler) {
        this.platformExecutor = null;
        this.platformQueueCapacity = queueCapacity;
        this.threadMode = ThreadMode.VIRTUAL;

        this.rejectedTaskCount = new java.util.concurrent.atomic.AtomicLong(0);
        this.virtualRejectionPolicyOriginal = rejectionHandler;
        RejectedExecutionHandler virtualCountingHandler = (Runnable r, ThreadPoolExecutor executor) -> {
            rejectedTaskCount.incrementAndGet();
            rejectionHandler.rejectedExecution(r, executor);
        };
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(maxConcurrency);
        this.pendingQueue = queueCapacity > 0
                ? new LinkedBlockingQueue<>(queueCapacity)
                : new LinkedBlockingQueue<>();
        this.virtualRejectionHandler = virtualCountingHandler;
        this.virtualCompletedTaskCount = new AtomicLong(0);
        this.virtualSubmittedTaskCount = new AtomicLong(0);
        this.peakConcurrency = new AtomicInteger(0);
        this.virtualMaxConcurrency = maxConcurrency;
        this.virtualKeepAliveTimeMs = keepAliveTimeMs;
        this.virtualShutdown = false;

        startDrainer();
    }

    public static ManagedExecutor virtual(int maxConcurrency, int queueCapacity,
                                           long keepAliveTime, TimeUnit unit,
                                           RejectedExecutionHandler rejectionHandler) {
        Objects.requireNonNull(unit, "time unit must not be null");
        Objects.requireNonNull(rejectionHandler, "rejectionHandler must not be null");
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1, was " + maxConcurrency);
        }
        if (maxConcurrency > MAX_POOL_SIZE) {
            throw new IllegalArgumentException(
                    "maxConcurrency must be <= " + MAX_POOL_SIZE + ", was " + maxConcurrency);
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be non-negative, was " + queueCapacity);
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("keepAliveTime must be >= 0, was " + keepAliveTime);
        }
        return new ManagedExecutor(maxConcurrency, queueCapacity,
                unit.toMillis(keepAliveTime), rejectionHandler);
    }

    private void startDrainer() {
        Thread drainer = new Thread(() -> {
            try {
                while (!virtualShutdown || !pendingQueue.isEmpty()) {
                    try {
                        while (drainerPaused && (!virtualShutdown || !pendingQueue.isEmpty())) {
                            Thread.sleep(10);
                        }
                        Runnable task = pendingQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            virtualExecutor.submit(task);
                        }
                    } catch (RejectedExecutionException e) {
                        // virtualExecutor shut down; exit drainer
                        break;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                virtualExecutor.shutdown();
            }
        }, "virtual-executor-drainer");
        drainer.setDaemon(true);
        drainer.start();
        this.drainerThread = drainer;
    }

    /**
     * Pauses the drainer thread (VIRTUAL mode only). Used in tests to
     * observe queue state without the drainer consuming tasks.
     */
    void pauseDrainer() {
        drainerPaused = true;
    }

    /**
     * Resumes the drainer thread after a pause.
     */
    void resumeDrainer() {
        drainerPaused = false;
    }

    // ============================================================
    // submit
    // ============================================================

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.submit(task);
        }
        return (Future<T>) virtualSubmit(task);
    }

    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.submit(task);
        }
        return virtualSubmit(task);
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> virtualSubmit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        virtualSubmittedTaskCount.incrementAndGet();

        if (virtualShutdown) {
            virtualSubmittedTaskCount.decrementAndGet();
            future.completeExceptionally(new RejectedExecutionException("Executor has been shut down"));
            return future;
        }

        Runnable wrapped = () -> {
            boolean acquired = false;
            try {
                semaphore.acquire();
                acquired = true;
                int active = virtualMaxConcurrency - semaphore.availablePermits();
                peakConcurrency.updateAndGet(prev -> Math.max(prev, active));
                future.complete(task.call());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (acquired) {
                    semaphore.release();
                }
                virtualCompletedTaskCount.incrementAndGet();
            }
        };

        if (!pendingQueue.offer(wrapped)) {
            virtualSubmittedTaskCount.decrementAndGet();
            throw new RejectedExecutionException("Queue full; task rejected");
        }

        return future;
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> virtualSubmit(Runnable task) {
        Callable<T> callable = () -> {
            task.run();
            return null;
        };
        return virtualSubmit(callable);
    }

    // ============================================================
    // pool size getters
    // ============================================================

    public int getCorePoolSize() {
        return threadMode == ThreadMode.PLATFORM
                ? platformExecutor.getCorePoolSize()
                : virtualMaxConcurrency;
    }

    public int getMaximumPoolSize() {
        return threadMode == ThreadMode.PLATFORM
                ? platformExecutor.getMaximumPoolSize()
                : virtualMaxConcurrency;
    }

    public long getKeepAliveTime(TimeUnit unit) {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getKeepAliveTime(unit);
        }
        return unit.convert(virtualKeepAliveTimeMs, TimeUnit.MILLISECONDS);
    }

    public int getActiveCount() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getActiveCount();
        }
        return virtualMaxConcurrency - semaphore.availablePermits();
    }

    public int getPoolSize() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getPoolSize();
        }
        return virtualMaxConcurrency - semaphore.availablePermits();
    }

    public int getQueueSize() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getQueue().size();
        }
        return pendingQueue.size();
    }

    public long getCompletedTaskCount() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getCompletedTaskCount();
        }
        return virtualCompletedTaskCount.get();
    }

    public int getLargestPoolSize() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getLargestPoolSize();
        }
        return peakConcurrency.get();
    }

    public long getTaskCount() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.getTaskCount();
        }
        return virtualSubmittedTaskCount.get();
    }

    public int getQueueCapacity() {
        return threadMode == ThreadMode.PLATFORM
                ? platformQueueCapacity
                : pendingQueue.remainingCapacity() + pendingQueue.size();
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public long getRejectedTaskCount() {
        return rejectedTaskCount.get();
    }

    // ============================================================
    // setters
    // ============================================================

    public void setCorePoolSize(int corePoolSize) {
        if (threadMode == ThreadMode.PLATFORM) {
            platformExecutor.setCorePoolSize(corePoolSize);
            return;
        }
        adjustSemaphore(corePoolSize);
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (threadMode == ThreadMode.PLATFORM) {
            platformExecutor.setMaximumPoolSize(maximumPoolSize);
            return;
        }
        adjustSemaphore(maximumPoolSize);
    }

    private synchronized void adjustSemaphore(int newMaxConcurrency) {
        if (newMaxConcurrency < 1) {
            throw new IllegalArgumentException(
                    "concurrency must be >= 1, was " + newMaxConcurrency);
        }
        int delta = newMaxConcurrency - virtualMaxConcurrency;
        virtualMaxConcurrency = newMaxConcurrency;
        if (delta > 0) {
            semaphore.release(delta);
        } else if (delta < 0) {
            semaphore.acquireUninterruptibly(-delta);
        }
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (threadMode == ThreadMode.PLATFORM) {
            platformExecutor.setKeepAliveTime(time, unit);
            return;
        }
        // stored for observation only; virtual threads have no keep-alive
    }

    // ============================================================
    // rejection policy
    // ============================================================

    public RejectedExecutionHandler getRejectionPolicy() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformRejectionPolicy;
        }
        return virtualRejectionPolicyOriginal;
    }

    public void setRejectionPolicy(RejectedExecutionHandler newPolicy) {
        Objects.requireNonNull(newPolicy, "rejectionPolicy must not be null");
        if (threadMode == ThreadMode.PLATFORM) {
            platformRejectionPolicy = newPolicy;
            RejectedExecutionHandler countingHandler = (Runnable r, ThreadPoolExecutor executor) -> {
                rejectedTaskCount.incrementAndGet();
                newPolicy.rejectedExecution(r, executor);
            };
            platformExecutor.setRejectedExecutionHandler(countingHandler);
            return;
        }
        virtualRejectionPolicyOriginal = newPolicy;
        RejectedExecutionHandler countingHandler = (Runnable r, ThreadPoolExecutor executor) -> {
            rejectedTaskCount.incrementAndGet();
            newPolicy.rejectedExecution(r, executor);
        };
        virtualRejectionHandler = countingHandler;
    }

    // ============================================================
    // lifecycle
    // ============================================================

    public void shutdown() {
        if (threadMode == ThreadMode.PLATFORM) {
            platformExecutor.shutdown();
            return;
        }
        virtualShutdown = true;
    }

    public List<Runnable> shutdownNow() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.shutdownNow();
        }
        virtualShutdown = true;
        if (drainerThread != null) {
            drainerThread.interrupt();
        }
        List<Runnable> drained = new ArrayList<>();
        pendingQueue.drainTo(drained);
        semaphore.drainPermits();
        semaphore.release(virtualMaxConcurrency);
        virtualExecutor.shutdownNow();
        return drained;
    }

    public boolean isShutdown() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.isShutdown();
        }
        return virtualShutdown;
    }

    public boolean isStopped() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.isTerminating() || platformExecutor.isTerminated();
        }
        return virtualShutdown || (semaphore.availablePermits() == virtualMaxConcurrency);
    }

    public boolean isTerminated() {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.isTerminated();
        }
        return virtualShutdown
                && semaphore.availablePermits() == virtualMaxConcurrency
                && (drainerThread == null || !drainerThread.isAlive());
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (threadMode == ThreadMode.PLATFORM) {
            return platformExecutor.awaitTermination(timeout, unit);
        }
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            if (isTerminated()) {
                return true;
            }
            Thread.sleep(50);
        }
        return isTerminated();
    }

    public ThreadPoolExecutor unwrap() {
        if (threadMode == ThreadMode.VIRTUAL) {
            return null;
        }
        return platformExecutor;
    }

    // ============================================================
    // snapshot
    // ============================================================

    public ExecutorStateSnapshot toSnapshot() {
        return ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(getCorePoolSize())
                .maximumPoolSize(getMaximumPoolSize())
                .activeCount(getActiveCount())
                .poolSize(getPoolSize())
                .queueSize(getQueueSize())
                .queueCapacity(getQueueCapacity())
                .completedTaskCount(getCompletedTaskCount())
                .keepAliveTimeSeconds(getKeepAliveTime(TimeUnit.SECONDS))
                .largestPoolSize(getLargestPoolSize())
                .taskCount(getTaskCount())
                .build();
    }

    // ============================================================
    // helpers
    // ============================================================

    private static ThreadMode detectThreadMode(ThreadFactory factory) {
        Thread t = factory.newThread(() -> {});
        return t.isVirtual() ? ThreadMode.VIRTUAL : ThreadMode.PLATFORM;
    }

    @Override
    public void close() {
        shutdown();
    }
}
