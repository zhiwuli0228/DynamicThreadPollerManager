package com.zhiwu.dynamicthreadpollermanager.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对照实验数据采集 — 模拟真实环境的线程池方案对比。
 *
 * 实验设计原则:
 * - 任务在固定间隔提交，不等待清空（模拟真实请求到达）
 * - 不同方案在相同负载下自然表现出差异
 * - 数据可直接用于 PPT
 */
class ExperimentDataCollectionTest {

    // 任务模拟: IO密集型, 10-50ms 随机延迟
    private static final Runnable IO_TASK = () -> {
        try {
            long sleepMs = 10 + (long) (ThreadLocalRandom.current().nextDouble() * 40);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    // ── 实验 1: 静态方案 × 多负载对比 ────────────────────────────────

    @Test
    @DisplayName("实验1: 三种静态线程池方案在四种负载下的对比")
    void experiment1_staticConfigComparison() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("实验 1: 静态线程池方案 × 负载对比");
        System.out.println("=".repeat(80));

        PoolConfig[] pools = {
                new PoolConfig("保守小池(2/2/10)", 2, 2, 10),
                new PoolConfig("均衡中池(4/4/20)", 4, 4, 20),
                new PoolConfig("宽松大池(8/8/50)", 8, 8, 50),
                new PoolConfig("弹性缓存池(cached)", 0, Integer.MAX_VALUE, 0),
        };

        // 每组实验跑 3 轮取平均值
        int rounds = 3;
        List<ExperimentResult> allResults = new ArrayList<>();

        // 四个负载场景
        String[] workloadNames = {"STEADY", "RAMP", "BURST", "SPIKE"};
        String[] workloadDescs = {
                "持续平稳(20步×5任务)",
                "逐步递增(20步:1→20任务)",
                "周期突发(20步:尖峰10/正常2)",
                "极端尖峰(20步:尖峰20/正常2)"
        };

        for (int round = 0; round < rounds; round++) {
            for (int wi = 0; wi < workloadNames.length; wi++) {
                int[] tasks = switch (wi) {
                    case 0 -> steady(20, 5);
                    case 1 -> ramp(20, 1, 20);
                    case 2 -> burst(20, 10, 2);
                    case 3 -> burst(20, 20, 2);
                    default -> throw new IllegalStateException();
                };
                String wlName = workloadNames[wi];
                String wlDesc = workloadDescs[wi];

                for (PoolConfig pool : pools) {
                    ExperimentResult r = runStaticExperiment(pool, wlName, wlDesc, tasks);
                    allResults.add(r);
                }
            }
        }

        printStaticComparisonTable(allResults, pools, workloadNames);
        assertTrue(allResults.stream().allMatch(r -> r.completed > 0));
    }

    // ── 实验 2: 静态 vs 闭环自适应 ──────────────────────────────────

    @Test
    @DisplayName("实验2: 静态配置 vs 简单闭环自适应 (BURST负荷)")
    void experiment2_staticVsAdaptive() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 2: 静态配置 vs 闭环自适应 — 30步 BURST 负载");
        System.out.println("=".repeat(80));

        int[] burstWorkload = burst(30, 15, 3);

        ExperimentResult staticSmall = runStaticExperiment(
                new PoolConfig("静态保守(2/2/10)", 2, 2, 10),
                "BURST30", "30步突发(15/3)", burstWorkload);
        ExperimentResult staticMedium = runStaticExperiment(
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                "BURST30", "30步突发(15/3)", burstWorkload);
        ExperimentResult staticLarge = runStaticExperiment(
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
                "BURST30", "30步突发(15/3)", burstWorkload);
        ExperimentResult staticCached = runStaticExperiment(
                new PoolConfig("弹性缓存池(cached)", 0, Integer.MAX_VALUE, 0),
                "BURST30", "30步突发(15/3)", burstWorkload);
        ExperimentResult adaptive = runAdaptiveExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20),
                "BURST30", "30步突发(15/3)", burstWorkload);

        System.out.println("\n── 静态 vs 自适应 最终对比 ──");
        System.out.printf("%-28s | %8s | %8s | %8s | %8s | %10s | %8s%n",
                "方案", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s", "耗时ms");
        System.out.println("-".repeat(100));
        for (ExperimentResult r : List.of(staticSmall, staticMedium, staticLarge, staticCached, adaptive)) {
            System.out.printf("%-28s | %8d | %8d | %8.1f | %8d | %10.1f | %8d%n",
                    r.poolName, r.completed, r.rejected,
                    r.avgQueueDepth, r.maxQueueDepth, r.throughput, r.durationMs);
        }
    }

    // ── 实验 3: 队列容量对拒绝率的影响 ──────────────────────────────

    @Test
    @DisplayName("实验3: 队列容量对拒绝率和吞吐量的影响")
    void experiment3_queueCapacityImpact() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 3: 固定4线程下, 队列容量对拒绝率的影响");
        System.out.println("=".repeat(80));

        int[] burstWorkload = burst(20, 12, 3);

        System.out.printf("%-18s | %8s | %8s | %8s | %8s | %10s%n",
                "队列容量", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s");
        System.out.println("-".repeat(70));

        for (int q : new int[]{2, 5, 10, 20, 50, Integer.MAX_VALUE / 2}) {
            String qLabel = q > 10000 ? "无界" : String.valueOf(q);
            ExperimentResult r = runStaticExperiment(
                    new PoolConfig("4线程/q=" + qLabel, 4, 4, q),
                    "BURST20", "", burstWorkload);
            System.out.printf("%-18s | %8d | %8d | %8.1f | %8d | %10.1f%n",
                    "q=" + qLabel, r.completed, r.rejected,
                    r.avgQueueDepth, r.maxQueueDepth, r.throughput);
        }
    }

    // ── 实验 5: 延迟分布 — p50/p95/p99 ──────────────────────────────

    @Test
    @DisplayName("实验5: 延迟分布对比 (p50/p95/p99) — 静态 vs 自适应")
    void experiment5_latencyDistribution() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 5: 延迟分布对比 — BURST 30步负载");
        System.out.println("=".repeat(80));

        int[] workload = burst(30, 15, 3);

        System.out.printf("%-24s | %6s | %6s | %6s | %6s | %6s | %8s | %8s%n",
                "方案", "p50ms", "p95ms", "p99ms", "maxms", "avgms", "完成", "吞吐/s");
        System.out.println("-".repeat(95));

        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态保守(2/2/10)", 2, 2, 10),
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
                new PoolConfig("弹性缓存池(cached)", 0, Integer.MAX_VALUE, 0),
        }) {
            LatencyResult r = runLatencyExperiment(cfg, workload);
            System.out.printf("%-24s | %6.1f | %6.1f | %6.1f | %6.1f | %6.1f | %8d | %8.1f%n",
                    cfg.name, r.p50, r.p95, r.p99, r.max, r.avg, r.completed, r.throughput);
        }

        LatencyResult adaptive = runAdaptiveLatencyExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20), workload);
        System.out.printf("%-24s | %6.1f | %6.1f | %6.1f | %6.1f | %6.1f | %8d | %8.1f%n",
                "闭环自适应(2→8/20)", adaptive.p50, adaptive.p95, adaptive.p99,
                adaptive.max, adaptive.avg, adaptive.completed, adaptive.throughput);

        // 对比结论
        System.out.println("\n  → p95 延迟是 SLA 核心指标，p99 反映长尾");
    }

    // ── 实验 6: 长期稳定性 ──────────────────────────────────────────

    @Test
    @DisplayName("实验6: 长期稳定性 — 100步持续运行")
    void experiment6_longRunningStability() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 6: 长期稳定性 — 100步 BURST 负载");
        System.out.println("=".repeat(80));

        int[] longWorkload = burst(100, 15, 3);

        // 静态方案
        ExperimentResult staticR = runStaticExperiment(
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                "BURST100", "100步突发", longWorkload);

        // 自适应方案 — 扩展版，记录时序数据
        StabilityResult adaptiveS = runAdaptiveStabilityExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20),
                "BURST100", "100步突发", longWorkload);

        System.out.println("\n── 长期稳定性对比 ──");
        System.out.printf("%-24s | %8s | %8s | %8s | %8s | %10s | %12s | %12s%n",
                "方案", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s", "最终线程", "线程调整次数");
        System.out.println("-".repeat(110));
        System.out.printf("%-24s | %8d | %8d | %8.1f | %8d | %10.1f | %12d | %12s%n",
                staticR.poolName, staticR.completed, staticR.rejected,
                staticR.avgQueueDepth, staticR.maxQueueDepth, staticR.throughput,
                (int) staticR.avgActiveThreads, "N/A(固定)");
        System.out.printf("%-24s | %8d | %8d | %8.1f | %8d | %10.1f | %12d | %12d%n",
                adaptiveS.poolName, adaptiveS.completed, adaptiveS.rejected,
                adaptiveS.avgQueueDepth, adaptiveS.maxQueueDepth, adaptiveS.throughput,
                adaptiveS.finalThreads, adaptiveS.totalAdjustments);

        // 稳定性指标
        System.out.printf("\n  稳定性: 线程标准差=%.2f, 振荡次数=%d, 最大连续调整=%d%n",
                adaptiveS.threadStdDev, adaptiveS.oscillationCount, adaptiveS.maxConsecutiveAdjusts);
    }

    // ── 实验 7: 自适应响应速度 ──────────────────────────────────────

    @Test
    @DisplayName("实验7: 自适应响应速度 — 尖峰检测到完成调整的延迟")
    void experiment7_adaptationSpeed() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 7: 自适应响应速度 — 从检测到调整完成的延迟");
        System.out.println("=".repeat(80));

        // 设计: 前10步低负载(2任务/步), 第11步突然尖峰(20任务/步), 持续10步
        int[] workload = new int[20];
        for (int i = 0; i < 10; i++) workload[i] = 2;
        for (int i = 10; i < 20; i++) workload[i] = 20;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        int currentCore = 2;
        List<String> timeline = new ArrayList<>();

        long startMs = System.currentTimeMillis();
        long spikeTimeMs = -1;
        long firstDetectMs = -1;
        long firstAdjustMs = -1;

        for (int step = 0; step < workload.length; step++) {
            int taskCount = workload[step];

            if (step == 10) spikeTimeMs = System.currentTimeMillis();

            for (int t = 0; t < taskCount; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();

            // 检测尖峰
            if (qs > 10 && ac >= currentCore && firstDetectMs < 0) {
                firstDetectMs = System.currentTimeMillis();
            }

            // 调整
            if (qs > 5 && ac >= currentCore && currentCore < 8) {
                int newCore = Math.min(currentCore + 2, 8);
                executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                executor.setCorePoolSize(newCore);
                if (firstAdjustMs < 0) firstAdjustMs = System.currentTimeMillis();
                timeline.add("s" + step + ":↑" + currentCore + "→" + newCore + " q=" + qs);
                currentCore = newCore;
            }

            Thread.sleep(50);
        }

        // 等待完成
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        long detectDelayMs = firstDetectMs > 0 ? firstDetectMs - spikeTimeMs : -1;
        long adjustDelayMs = firstAdjustMs > 0 ? firstAdjustMs - spikeTimeMs : -1;

        System.out.println("调整时间线: " + String.join(" | ", timeline));
        System.out.printf("尖峰到达: +%dms (step 10)%n", spikeTimeMs - startMs);
        System.out.printf("首次检测: +%dms (延迟 %dms)%n",
                firstDetectMs - startMs, detectDelayMs);
        System.out.printf("首次调整: +%dms (延迟 %dms)%n",
                firstAdjustMs - startMs, adjustDelayMs);
        System.out.printf("完成=%d 拒绝=%d 最终core=%d 总耗时=%dms%n",
                completed.sum(), rejected.sum(), currentCore, endMs - startMs);
        System.out.printf("响应速度: 检测延迟 %dms, 调整延迟 %dms (含冷却)%n",
                detectDelayMs, adjustDelayMs);
    }

    // ── 实验 8: 下游阻塞模拟与恢复 ──────────────────────────────────

    @Test
    @DisplayName("实验8: 下游阻塞模拟 — 线程耗尽与恢复")
    void experiment8_downstreamBlockageRecovery() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 8: 下游阻塞模拟 — 固定池 vs 自适应池的恢复对比");
        System.out.println("=".repeat(80));

        // 场景: 20步, 第5-10步出现阻塞任务(200ms vs 正常10-50ms), 模拟下游RT突增
        int[] workload = steady(20, 8);

        // 阻塞任务
        Runnable blockingTask = () -> {
            try {
                Thread.sleep(150 + (long) (ThreadLocalRandom.current().nextDouble() * 100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        System.out.printf("%-24s | %8s | %8s | %8s | %10s | %10s | %8s%n",
                "方案", "完成", "超时等效", "阻塞期队列", "阻塞期吞吐", "恢复后吞吐", "恢复耗时");
        System.out.println("-".repeat(110));

        // 对比 3 种方案
        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态小池(4/4/20)", 4, 4, 20),
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
        }) {
            BlockageResult r = runBlockageExperiment(cfg, workload, blockingTask, 5, 10);
            System.out.printf("%-24s | %8d | %8d | %8.1f | %10.1f | %10.1f | %8d%n",
                    cfg.name, r.completed, r.timedOutEquivalent,
                    r.blockagePeriodAvgQueue, r.blockagePeriodThroughput,
                    r.recoveryThroughput, r.recoveryTimeMs);
        }

        BlockageResult adaptiveR = runAdaptiveBlockageExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20),
                workload, blockingTask, 5, 10);
        System.out.printf("%-24s | %8d | %8d | %8.1f | %10.1f | %10.1f | %8d%n",
                "闭环自适应(2→8/20)", adaptiveR.completed, adaptiveR.timedOutEquivalent,
                adaptiveR.blockagePeriodAvgQueue, adaptiveR.blockagePeriodThroughput,
                adaptiveR.recoveryThroughput, adaptiveR.recoveryTimeMs);
    }

    // ── 实验 9: 混合 CPU/IO 负载 ────────────────────────────────────

    @Test
    @DisplayName("实验9: 混合CPU/IO负载下各方案表现")
    void experiment9_mixedCpuIoWorkload() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 9: 混合 CPU/IO 负载 — 30步 BURST, 70%IO + 30%CPU");
        System.out.println("=".repeat(80));

        int[] workload = burst(30, 15, 3);
        double ioRatio = 0.7;

        // CPU任务: 短暂CPU计算模拟
        Runnable cpuTask = () -> {
            long sum = 0;
            long end = System.nanoTime() + (long) (10 + ThreadLocalRandom.current().nextDouble() * 30) * 1_000_000;
            while (System.nanoTime() < end) {
                sum = (sum + 1) % 100000;
            }
        };

        System.out.printf("%-24s | %8s | %8s | %8s | %10s | %12s | %12s%n",
                "方案", "完成", "拒绝", "平均队列", "吞吐/s", "CPU任务完成", "IO任务完成");
        System.out.println("-".repeat(110));

        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态保守(2/2/10)", 2, 2, 10),
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
                new PoolConfig("弹性缓存池(cached)", 0, Integer.MAX_VALUE, 0),
        }) {
            MixedResult r = runMixedExperiment(cfg, workload, ioRatio, cpuTask);
            System.out.printf("%-24s | %8d | %8d | %8.1f | %10.1f | %12d | %12d%n",
                    cfg.name, r.completed, r.rejected, r.avgQueueDepth,
                    r.throughput, r.cpuCompleted, r.ioCompleted);
        }

        MixedResult adaptiveR = runAdaptiveMixedExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20),
                workload, ioRatio, cpuTask);
        System.out.printf("%-24s | %8d | %8d | %8.1f | %10.1f | %12d | %12d%n",
                "闭环自适应(2→8/20)", adaptiveR.completed, adaptiveR.rejected,
                adaptiveR.avgQueueDepth, adaptiveR.throughput,
                adaptiveR.cpuCompleted, adaptiveR.ioCompleted);
    }

    // ── 实验 10: 振荡分析 ───────────────────────────────────────────

    @Test
    @DisplayName("实验10: 振荡分析 — 线程数时序与收敛性")
    void experiment10_oscillationAnalysis() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 10: 振荡分析 — 自适应线程数时序变化");
        System.out.println("=".repeat(80));

        // 交替高-低负载: 10步高峰(20任务) → 10步低峰(3任务) → 10步高峰 → 10步低峰
        int[] workload = new int[40];
        for (int i = 0; i < 10; i++) workload[i] = 20;
        for (int i = 10; i < 20; i++) workload[i] = 3;
        for (int i = 20; i < 30; i++) workload[i] = 20;
        for (int i = 30; i < 40; i++) workload[i] = 3;

        // 记录每步的线程数
        List<Integer> threadHistory = new ArrayList<>();
        List<Integer> queueHistory = new ArrayList<>();
        List<String> adjustmentLog = new ArrayList<>();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        int currentCore = 2;
        int lastAdjustStep = -10;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < workload.length; step++) {
            int taskCount = workload[step];

            for (int t = 0; t < taskCount; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();
            threadHistory.add(currentCore);
            queueHistory.add(qs);

            // 自适应逻辑 (冷却3步 vs 之前5步, 观察振荡差异)
            if (step - lastAdjustStep >= 3) {
                if (qs > 10 && ac >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 1 && currentCore < 8) {
                        int newCore = Math.min(currentCore + 2, 8);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        adjustmentLog.add("↑" + currentCore + "→" + newCore + "@s" + step);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else if (qs <= 2 && currentCore > 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 2 && currentCore > 2) {
                        int newCore = Math.max(currentCore - 1, 2);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        adjustmentLog.add("↓" + currentCore + "→" + newCore + "@s" + step);
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        // 振荡分析
        int directionChanges = 0;
        int lastDirection = 0;
        for (int i = 1; i < threadHistory.size(); i++) {
            int diff = threadHistory.get(i) - threadHistory.get(i - 1);
            if (diff != 0) {
                int dir = diff > 0 ? 1 : -1;
                if (lastDirection != 0 && dir != lastDirection) directionChanges++;
                lastDirection = dir;
            }
        }

        double avgThreads = threadHistory.stream().mapToInt(Integer::intValue).average().orElse(0);
        double threadVariance = threadHistory.stream()
                .mapToDouble(v -> Math.pow(v - avgThreads, 2)).average().orElse(0);
        double threadStdDev = Math.sqrt(threadVariance);

        System.out.println("调整日志: " + String.join(" | ", adjustmentLog));
        System.out.printf("总调整次数: %d, 方向变化次数: %d (振荡指标)%n",
                adjustmentLog.size(), directionChanges);
        System.out.printf("线程数: 均值=%.2f, 标准差=%.2f, 范围=[%d..%d]%n",
                avgThreads, threadStdDev,
                threadHistory.stream().mapToInt(Integer::intValue).min().orElse(0),
                threadHistory.stream().mapToInt(Integer::intValue).max().orElse(0));
        System.out.printf("完成=%d, 拒绝=%d, 最终线程=%d%n",
                completed.sum(), rejected.sum(), currentCore);

        // 打印线程数时序（每5步采样）
        System.out.print("线程数时序: ");
        for (int i = 0; i < threadHistory.size(); i += 5) {
            System.out.printf("[s%d:%d] ", i, threadHistory.get(i));
        }
        System.out.println();

        System.out.print("队列深度时序: ");
        for (int i = 0; i < queueHistory.size(); i += 5) {
            System.out.printf("[s%d:%d] ", i, queueHistory.get(i));
        }
        System.out.println();

        // 判断
        if (directionChanges <= 2) {
            System.out.println("→ 振荡控制良好 (方向变化≤2)");
        } else if (directionChanges <= 4) {
            System.out.println("→ 轻微振荡 (方向变化 " + directionChanges + ")");
        } else {
            System.out.println("→ 明显振荡, 需增强冷却或反振荡守卫");
        }
    }

    // ── 实验 11: 排队等待时间 vs 执行时间 ────────────────────────────

    @Test
    @DisplayName("实验11: 排队等待时间 vs 执行时间 — 区分池饱和与任务慢")
    void experiment11_queueWaitVsExecutionTime() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 11: 排队等待时间 vs 执行时间 — BURST 30步");
        System.out.println("=".repeat(80));

        int[] workload = burst(30, 15, 3);

        System.out.printf("%-24s | %8s | %8s | %8s | %8s | %6s%n",
                "方案", "排队p50", "排队p95", "执行p50", "执行p95", "排队占比");
        System.out.println("-".repeat(85));

        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态保守(2/2/10)", 2, 2, 10),
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
        }) {
            QueueWaitResult r = runQueueWaitExperiment(cfg, workload);
            double waitRatio = r.totalWaitMs > 0
                    ? r.totalWaitMs * 100.0 / (r.totalWaitMs + r.totalExecMs) : 0;
            System.out.printf("%-24s | %8.1f | %8.1f | %8.1f | %8.1f | %5.1f%%%n",
                    cfg.name, r.queueP50, r.queueP95, r.execP50, r.execP95, waitRatio);
        }

        QueueWaitResult adaptiveR = runAdaptiveQueueWaitExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20), workload);
        double adaptiveWaitRatio = adaptiveR.totalWaitMs > 0
                ? adaptiveR.totalWaitMs * 100.0 / (adaptiveR.totalWaitMs + adaptiveR.totalExecMs) : 0;
        System.out.printf("%-24s | %8.1f | %8.1f | %8.1f | %8.1f | %5.1f%%%n",
                "闭环自适应(2→8/20)", adaptiveR.queueP50, adaptiveR.queueP95,
                adaptiveR.execP50, adaptiveR.execP95, adaptiveWaitRatio);

        System.out.println("→ 排队占比高=池饱和, 执行占比高=任务慢。现网告警需区分两者");
    }

    // ── 实验 12: 持续过载退化行为 ────────────────────────────────────

    @Test
    @DisplayName("实验12: 持续过载退化 — 2倍容量持续100步")
    void experiment12_sustainedOverloadDegradation() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 12: 持续过载退化 — 4线程容量 vs 持续20任务/步(5倍过载)");
        System.out.println("=".repeat(80));

        // 持续高负载: 100步, 每步20任务 → 4线程消化不了的持续过载
        int[] overload = steady(100, 20);

        System.out.printf("%-28s | %8s | %8s | %8s | %8s | %10s | %8s | %8s%n",
                "方案", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s", "耗时ms", "退化趋势");
        System.out.println("-".repeat(120));

        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态Abort(4/4/20)", 4, 4, 20),
                new PoolConfig("静态CallerRuns(4/4/20)", 4, 4, 20),
                new PoolConfig("静态Abort(8/8/50)", 8, 8, 50),
        }) {
            OverloadResult r = runOverloadExperiment(cfg, overload,
                    cfg.name.contains("CallerRuns")
                            ? new ThreadPoolExecutor.CallerRunsPolicy()
                            : new ThreadPoolExecutor.AbortPolicy());
            // 分段检测退化趋势
            String trend = r.throughputFirstHalf > r.throughputSecondHalf * 1.1 ? "↓退化"
                    : r.throughputSecondHalf > r.throughputFirstHalf * 1.1 ? "↑恢复" : "→稳定";
            System.out.printf("%-28s | %8d | %8d | %8.1f | %8d | %10.1f | %8d | %8s%n",
                    cfg.name, r.completed, r.rejected, r.avgQueueDepth, r.maxQueueDepth,
                    r.throughput, r.durationMs, trend);
            System.out.printf("  前半段吞吐=%.1f/s  后半段吞吐=%.1f/s%n",
                    r.throughputFirstHalf, r.throughputSecondHalf);
        }

        OverloadResult adaptiveR = runAdaptiveOverloadExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20),
                overload, new ThreadPoolExecutor.AbortPolicy());
        String adaptiveTrend = adaptiveR.throughputFirstHalf > adaptiveR.throughputSecondHalf * 1.1
                ? "↓退化" : adaptiveR.throughputSecondHalf > adaptiveR.throughputFirstHalf * 1.1
                ? "↑恢复" : "→稳定";
        System.out.printf("%-28s | %8d | %8d | %8.1f | %8d | %10.1f | %8d | %8s%n",
                "闭环自适应(2→8/20)", adaptiveR.completed, adaptiveR.rejected,
                adaptiveR.avgQueueDepth, adaptiveR.maxQueueDepth,
                adaptiveR.throughput, adaptiveR.durationMs, adaptiveTrend);
        System.out.printf("  前半段吞吐=%.1f/s  后半段吞吐=%.1f/s%n",
                adaptiveR.throughputFirstHalf, adaptiveR.throughputSecondHalf);
    }

    // ── 实验 13: 内存开销对比 ────────────────────────────────────────

    @Test
    @DisplayName("实验13: 内存开销对比 — 峰值线程数×队列深度")
    void experiment13_memoryOverheadComparison() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 13: 内存开销对比 — RAMP递增负载(1→20)");
        System.out.println("=".repeat(80));

        int[] rampWorkload = ramp(30, 1, 20);

        System.out.printf("%-24s | %8s | %8s | %8s | %12s | %10s | %12s%n",
                "方案", "峰值线程", "峰值队列", "均值线程", "线程内存估", "队列引用估", "总内存估");
        System.out.println("-".repeat(110));

        for (PoolConfig cfg : new PoolConfig[]{
                new PoolConfig("静态保守(2/2/10)", 2, 2, 10),
                new PoolConfig("静态均衡(4/4/20)", 4, 4, 20),
                new PoolConfig("静态大池(8/8/50)", 8, 8, 50),
                new PoolConfig("静态超大(16/16/200)", 16, 16, 200),
                new PoolConfig("弹性缓存池(cached)", 0, Integer.MAX_VALUE, 0),
        }) {
            MemoryResult r = runMemoryExperiment(cfg, rampWorkload);
            // 估算: 线程栈1MB/线程, 队列引用+对象~1KB/条目
            long threadMemKB = r.peakThreads * 1024;  // 1MB = 1024KB
            long queueMemKB = r.peakQueue * 1;  // ~1KB/entry
            System.out.printf("%-24s | %8d | %8d | %8.1f | %10dKB | %8dKB | %10dKB%n",
                    cfg.name, r.peakThreads, r.peakQueue, r.avgThreads,
                    threadMemKB, queueMemKB, threadMemKB + queueMemKB);
        }

        MemoryResult adaptiveR = runAdaptiveMemoryExperiment(
                new PoolConfig("闭环自适应(2→8/20)", 2, 8, 20), rampWorkload);
        long aThreadMemKB = adaptiveR.peakThreads * 1024;
        long aQueueMemKB = adaptiveR.peakQueue * 1;
        System.out.printf("%-24s | %8d | %8d | %8.1f | %10dKB | %8dKB | %10dKB%n",
                "闭环自适应(2→8/20)", adaptiveR.peakThreads, adaptiveR.peakQueue,
                adaptiveR.avgThreads, aThreadMemKB, aQueueMemKB, aThreadMemKB + aQueueMemKB);

        System.out.println("→ 估算基准: 线程栈 ~1MB/线程, 队列条目 ~1KB/条");
    }

    // ── 实验 14: 拒绝策略对比 ────────────────────────────────────────

    @Test
    @DisplayName("实验14: 拒绝策略对比 — Abort vs CallerRuns vs DiscardOldest")
    void experiment14_rejectionPolicyComparison() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 14: 拒绝策略对比 — BURST 30步, 小池(2/2/5)故意触发拒绝");
        System.out.println("=".repeat(80));

        int[] workload = burst(30, 15, 3);

        System.out.printf("%-26s | %8s | %10s | %10s | %10s | %8s | %10s%n",
                "策略", "完成", "拒绝/调用者跑", "调用者耗时", "任务乱序", "吞吐/s", "队列峰值");
        System.out.println("-".repeat(120));

        for (RejectedExecutionHandler handler : new RejectedExecutionHandler[]{
                new ThreadPoolExecutor.AbortPolicy(),
                new ThreadPoolExecutor.CallerRunsPolicy(),
                new ThreadPoolExecutor.DiscardOldestPolicy(),
        }) {
            String handlerName = handler.getClass().getSimpleName();
            RejectionResult r = runRejectionExperiment(
                    new PoolConfig(handlerName, 2, 2, 5), workload, handler);
            System.out.printf("%-26s | %8d | %10d | %8.1fms | %10d | %8.1f | %10d%n",
                    handlerName, r.completed, r.callerExecuted,
                    r.avgCallerExecMs, r.discardedOldest, r.throughput, r.peakQueue);
        }
        System.out.println("→ AbortPolicy: 直接拒绝, 最安全但丢任务最多");
        System.out.println("→ CallerRunsPolicy: 调用线程执行, 自然限流但可能拖慢调用方");
        System.out.println("→ DiscardOldestPolicy: 丢弃最旧, 吞吐最高但丢任务且乱序");
    }

    // ── 实验 15: 队列类型对比 ────────────────────────────────────────

    @Test
    @DisplayName("实验15: 队列类型对比 — LinkedBlockingQueue vs ArrayBlockingQueue")
    void experiment15_queueTypeComparison() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 15: LinkedBlockingQueue vs ArrayBlockingQueue — RAMP 20步");
        System.out.println("=".repeat(80));

        int[] rampWorkload = ramp(20, 1, 20);

        System.out.printf("%-28s | %8s | %8s | %8s | %10s | %8s | %8s%n",
                "队列类型", "完成", "拒绝", "平均队列", "吞吐/s", "耗时ms", "p95ms");
        System.out.println("-".repeat(110));

        // LinkedBlockingQueue
        QueueTypeResult linkedR = runQueueTypeExperiment(
                new PoolConfig("LinkedBlocking(4/4/50)", 4, 4, 50),
                rampWorkload, true);
        System.out.printf("%-28s | %8d | %8d | %8.1f | %10.1f | %8d | %8.1f%n",
                "LinkedBlockingQueue", linkedR.completed, linkedR.rejected,
                linkedR.avgQueueDepth, linkedR.throughput, linkedR.durationMs, linkedR.p95);

        // ArrayBlockingQueue
        QueueTypeResult arrayR = runQueueTypeExperiment(
                new PoolConfig("ArrayBlocking(4/4/50)", 4, 4, 50),
                rampWorkload, false);
        System.out.printf("%-28s | %8d | %8d | %8.1f | %10.1f | %8d | %8.1f%n",
                "ArrayBlockingQueue", arrayR.completed, arrayR.rejected,
                arrayR.avgQueueDepth, arrayR.throughput, arrayR.durationMs, arrayR.p95);

        System.out.println("→ LinkedBlockingQueue: 链表结构, 入列出列分离锁, 内存不可预分配");
        System.out.println("→ ArrayBlockingQueue: 数组结构, 单锁, 内存预分配可控, GC友好");
    }

    // ── 实验 16: 配置参数敏感性 ──────────────────────────────────────

    @Test
    @DisplayName("实验16: 配置参数敏感性 — 冷却窗口/压力阈值/调整步长")
    void experiment16_parameterSensitivity() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 16: 配置参数敏感性 — BURST 30步, 自适应(2→8/20)");
        System.out.println("=".repeat(80));

        int[] workload = burst(30, 15, 3);

        // 维度1: 冷却窗口
        System.out.println("\n── 维度1: 冷却窗口对效果的影响 ──");
        System.out.printf("%-18s | %8s | %8s | %8s | %10s | %8s | %10s%n",
                "冷却(步)", "完成", "拒绝", "调整次数", "吞吐/s", "最终core", "振荡次数");
        System.out.println("-".repeat(95));
        for (int cooldown : new int[]{1, 3, 5, 10}) {
            SensitivityResult r = runSensitivityExperiment(2, 8, 20,
                    cooldown, 10, 2, workload);
            System.out.printf("cooldown=%-11d | %8d | %8d | %8d | %10.1f | %8d | %10d%n",
                    cooldown, r.completed, r.rejected, r.adjustmentCount,
                    r.throughput, r.finalCore, r.oscillationCount);
        }

        // 维度2: 压力阈值
        System.out.println("\n── 维度2: 压力阈值(队列>N触发)对效果的影响 ──");
        System.out.printf("%-18s | %8s | %8s | %8s | %10s | %8s | %10s%n",
                "阈值", "完成", "拒绝", "调整次数", "吞吐/s", "最终core", "振荡次数");
        System.out.println("-".repeat(95));
        for (int threshold : new int[]{3, 5, 10, 15}) {
            SensitivityResult r = runSensitivityExperiment(2, 8, 20,
                    5, threshold, 2, workload);
            System.out.printf("queue>%-12d | %8d | %8d | %8d | %10.1f | %8d | %10d%n",
                    threshold, r.completed, r.rejected, r.adjustmentCount,
                    r.throughput, r.finalCore, r.oscillationCount);
        }

        // 维度3: 调整步长
        System.out.println("\n── 维度3: 调整步长对效果的影响 ──");
        System.out.printf("%-18s | %8s | %8s | %8s | %10s | %8s | %10s%n",
                "步长(线程)", "完成", "拒绝", "调整次数", "吞吐/s", "最终core", "振荡次数");
        System.out.println("-".repeat(95));
        for (int stepSize : new int[]{1, 2, 4}) {
            SensitivityResult r = runSensitivityExperiment(2, 8, 20,
                    5, 10, stepSize, workload);
            System.out.printf("step=%-13d | %8d | %8d | %8d | %10.1f | %8d | %10d%n",
                    stepSize, r.completed, r.rejected, r.adjustmentCount,
                    r.throughput, r.finalCore, r.oscillationCount);
        }

        System.out.println("\n→ 安全配置范围建议见输出");
    }

    @Test
    @DisplayName("实验4: 线程数对吞吐效率 (RAMP递增负载)")
    void experiment4_threadCountImpact() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("实验 4: RAMP递增负载(1→20)下, 线程数对吞吐效率的影响");
        System.out.println("=".repeat(80));

        int[] rampWorkload = ramp(20, 1, 20);

        System.out.printf("%-14s | %8s | %8s | %8s | %8s | %10s | %10s%n",
                "线程数", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s", "吞吐/线程");
        System.out.println("-".repeat(80));

        for (int threads : new int[]{1, 2, 4, 8, 16}) {
            ExperimentResult r = runStaticExperiment(
                    new PoolConfig("固定" + threads + "线程", threads, threads, 500),
                    "RAMP20", "1→20递增", rampWorkload);
            double tpp = r.throughput / threads;
            System.out.printf("core=max=%-8d | %8d | %8d | %8.1f | %8d | %10.1f | %10.1f%n",
                    threads, r.completed, r.rejected,
                    r.avgQueueDepth, r.maxQueueDepth, r.throughput, tpp);
        }
    }

    // ── 基础设施 ──────────────────────────────────────────────────────

    record PoolConfig(String name, int core, int max, int queue) {}

    record ExperimentResult(
            String poolName, String workloadName, String workloadDesc,
            int totalTasks, long completed, long rejected,
            double avgQueueDepth, int maxQueueDepth,
            long durationMs, double throughput,
            double avgActiveThreads, int finalPoolSize
    ) {}

    record LatencyResult(
            String poolName, long completed, long rejected,
            double p50, double p95, double p99, double max, double avg,
            double throughput
    ) {}

    record StabilityResult(
            String poolName, long completed, long rejected,
            double avgQueueDepth, int maxQueueDepth,
            double throughput, int finalThreads, int totalAdjustments,
            double threadStdDev, int oscillationCount, int maxConsecutiveAdjusts
    ) {}

    record BlockageResult(
            String poolName, long completed, long timedOutEquivalent,
            double blockagePeriodAvgQueue, double blockagePeriodThroughput,
            double recoveryThroughput, long recoveryTimeMs
    ) {}

    record MixedResult(
            String poolName, long completed, long rejected,
            double avgQueueDepth, double throughput,
            long cpuCompleted, long ioCompleted
    ) {}

    record QueueWaitResult(
            String poolName, long completed, long rejected,
            double queueP50, double queueP95, double execP50, double execP95,
            long totalWaitMs, long totalExecMs
    ) {}

    record OverloadResult(
            String poolName, long completed, long rejected,
            double avgQueueDepth, int maxQueueDepth,
            double throughput, double throughputFirstHalf, double throughputSecondHalf,
            long durationMs
    ) {}

    record MemoryResult(
            String poolName, int peakThreads, int peakQueue, double avgThreads
    ) {}

    record RejectionResult(
            String poolName, long completed, long callerExecuted,
            double avgCallerExecMs, long discardedOldest,
            double throughput, int peakQueue
    ) {}

    record QueueTypeResult(
            String poolName, long completed, long rejected,
            double avgQueueDepth, double throughput, long durationMs, double p95
    ) {}

    record SensitivityResult(
            String poolName, long completed, long rejected,
            int adjustmentCount, double throughput, int finalCore, int oscillationCount
    ) {}

    private ExperimentResult runStaticExperiment(
            PoolConfig config, String wlName, String wlDesc, int[] taskCounts)
            throws Exception {

        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        AtomicLong maxQueueObserved = new AtomicLong(0);
        LongAdder activeSum = new LongAdder();
        int sampleCount = 0;
        int totalSubmitted = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            int taskCount = taskCounts[step];
            totalSubmitted += taskCount;

            for (int t = 0; t < taskCount; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            // 每步后采样队列和活跃线程
            queueSum.add(executor.getQueue().size());
            maxQueueObserved.updateAndGet(v -> Math.max(v, executor.getQueue().size()));
            activeSum.add(executor.getActiveCount());
            sampleCount++;

            // 短间隔后继续下一步（模拟真实请求间隔 ~50ms）
            Thread.sleep(50);
        }

        // 等待所有任务完成（最多 15 秒）
        long waitDeadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < waitDeadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            // 期间继续采样
            if (System.currentTimeMillis() % 200 < 50) {
                queueSum.add(executor.getQueue().size());
                maxQueueObserved.updateAndGet(v -> Math.max(v, executor.getQueue().size()));
                activeSum.add(executor.getActiveCount());
                sampleCount++;
            }
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        long durationMs = endMs - startMs;

        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        int maxQueue = (int) maxQueueObserved.get();
        double throughput = durationMs > 0 ? (double) completed.sum() / (durationMs / 1000.0) : 0;
        double avgActive = sampleCount > 0 ? (double) activeSum.sum() / sampleCount : 0;

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new ExperimentResult(
                config.name, wlName, wlDesc, totalSubmitted,
                completed.sum(), rejected.sum(),
                avgQueue, maxQueue, durationMs, throughput,
                avgActive, config.max);
    }

    private ExperimentResult runAdaptiveExperiment(
            PoolConfig config, String wlName, String wlDesc, int[] taskCounts)
            throws Exception {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        AtomicLong maxQueueObserved = new AtomicLong(0);
        LongAdder activeSum = new LongAdder();
        int sampleCount = 0;
        int totalSubmitted = 0;
        List<String> adjustmentLog = new ArrayList<>();

        int currentCore = 2;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;
        long lastAdjustStep = -10;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            int taskCount = taskCounts[step];
            totalSubmitted += taskCount;

            // 提交任务
            for (int t = 0; t < taskCount; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            // 采样
            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();
            queueSum.add(qs);
            maxQueueObserved.updateAndGet(v -> Math.max(v, qs));
            activeSum.add(ac);
            sampleCount++;

            // 闭环决策: 每 5 步评估一次, 冷却 5 步
            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && ac >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        adjustmentLog.add("↑" + currentCore + "→" + newCore + "@s" + step);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else if (qs <= 2 && ac <= currentCore / 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 3 && currentCore > 2) {
                        int newCore = Math.max(currentCore - 1, 2);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        adjustmentLog.add("↓" + currentCore + "→" + newCore + "@s" + step);
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                }
            }

            Thread.sleep(50);
        }

        // 等待剩余任务完成
        long waitDeadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < waitDeadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        long durationMs = endMs - startMs;

        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        int maxQueue = (int) maxQueueObserved.get();
        double throughput = durationMs > 0 ? (double) completed.sum() / (durationMs / 1000.0) : 0;
        double avgActive = sampleCount > 0 ? (double) activeSum.sum() / sampleCount : 0;

        System.out.println("  [闭环] " + String.join(" | ", adjustmentLog));
        System.out.printf("  [闭环] 最终core=%d 完成=%d 拒绝=%d 平均队列=%.1f 最大队列=%d 吞吐=%.1f/s 耗时=%dms%n",
                currentCore, completed.sum(), rejected.sum(), avgQueue, maxQueue, throughput, durationMs);

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new ExperimentResult(
                config.name, wlName, wlDesc, totalSubmitted,
                completed.sum(), rejected.sum(),
                avgQueue, maxQueue, durationMs, throughput,
                avgActive, currentCore);
    }

    // ── 实验5-10 运行器 ────────────────────────────────────────────────

    private LatencyResult runLatencyExperiment(PoolConfig config, int[] taskCounts)
            throws Exception {
        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> {
                        long t0 = System.nanoTime();
                        IO_TASK.run();
                        long t1 = System.nanoTime();
                        latencies.add((t1 - t0) / 1_000_000);  // ns → ms
                        completed.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int n = sorted.size();
        double p50 = n > 0 ? sorted.get(n / 2) : 0;
        double p95 = n > 0 ? sorted.get((int) (n * 0.95)) : 0;
        double p99 = n > 0 ? sorted.get((int) (n * 0.99)) : 0;
        double max = n > 0 ? sorted.get(n - 1) : 0;
        double avg = n > 0 ? sorted.stream().mapToLong(Long::longValue).average().orElse(0) : 0;

        return new LatencyResult(config.name, completed.sum(), rejected.sum(),
                p50, p95, p99, max, avg, throughput);
    }

    private LatencyResult runAdaptiveLatencyExperiment(PoolConfig config, int[] taskCounts)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        int currentCore = 2;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;
        int lastAdjustStep = -10;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> {
                        long t0 = System.nanoTime();
                        IO_TASK.run();
                        long t1 = System.nanoTime();
                        latencies.add((t1 - t0) / 1_000_000);
                        completed.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();

            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && ac >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else if (qs <= 2 && ac <= currentCore / 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 3 && currentCore > 2) {
                        int newCore = Math.max(currentCore - 1, 2);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int n = sorted.size();
        double p50 = n > 0 ? sorted.get(n / 2) : 0;
        double p95 = n > 0 ? sorted.get((int) (n * 0.95)) : 0;
        double p99 = n > 0 ? sorted.get((int) (n * 0.99)) : 0;
        double max = n > 0 ? sorted.get(n - 1) : 0;
        double avg = n > 0 ? sorted.stream().mapToLong(Long::longValue).average().orElse(0) : 0;

        System.out.printf("  [闭环延迟] p50=%.1f p95=%.1f p99=%.1f avg=%.1f max=%.1f%n",
                p50, p95, p99, avg, max);
        return new LatencyResult(config.name, completed.sum(), rejected.sum(),
                p50, p95, p99, max, avg, throughput);
    }

    private StabilityResult runAdaptiveStabilityExperiment(
            PoolConfig config, String wlName, String wlDesc, int[] taskCounts)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        AtomicLong maxQueueObserved = new AtomicLong(0);
        int sampleCount = 0;

        List<Integer> threadHistory = new ArrayList<>();
        List<Integer> adjustmentDirectionHistory = new ArrayList<>();
        int currentCore = 2;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;
        int lastAdjustStep = -10;
        int totalAdjustments = 0;
        int maxConsecutiveAdjusts = 0;
        int currentConsecutiveAdjusts = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            queueSum.add(qs);
            maxQueueObserved.updateAndGet(v -> Math.max(v, qs));
            threadHistory.add(currentCore);
            sampleCount++;

            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && executor.getActiveCount() >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        adjustmentDirectionHistory.add(1);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                        totalAdjustments++;
                        currentConsecutiveAdjusts++;
                        if (currentConsecutiveAdjusts > maxConsecutiveAdjusts)
                            maxConsecutiveAdjusts = currentConsecutiveAdjusts;
                    }
                } else if (qs <= 2 && executor.getActiveCount() <= currentCore / 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 3 && currentCore > 2) {
                        int newCore = Math.max(currentCore - 1, 2);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        adjustmentDirectionHistory.add(-1);
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                        totalAdjustments++;
                        currentConsecutiveAdjusts++;
                        if (currentConsecutiveAdjusts > maxConsecutiveAdjusts)
                            maxConsecutiveAdjusts = currentConsecutiveAdjusts;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                    currentConsecutiveAdjusts = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        // 振荡分析
        double avgThreads = threadHistory.stream().mapToInt(Integer::intValue).average().orElse(0);
        double threadVariance = threadHistory.stream()
                .mapToDouble(v -> Math.pow(v - avgThreads, 2)).average().orElse(0);
        double threadStdDev = Math.sqrt(threadVariance);

        int oscillationCount = 0;
        int lastDir = 0;
        for (int dir : adjustmentDirectionHistory) {
            if (lastDir != 0 && dir != lastDir) oscillationCount++;
            lastDir = dir;
        }

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new StabilityResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, (int) maxQueueObserved.get(), throughput,
                currentCore, totalAdjustments, threadStdDev,
                oscillationCount, maxConsecutiveAdjusts);
    }

    private BlockageResult runBlockageExperiment(
            PoolConfig config, int[] taskCounts, Runnable blockingTask,
            int blockageStartStep, int blockageEndStep) throws Exception {
        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder timedOutEquivalent = new LongAdder();
        LongAdder queueSumBlockage = new LongAdder();
        LongAdder completedBlockage = new LongAdder();
        LongAdder completedRecovery = new LongAdder();
        int blockageSamples = 0, recoverySamples = 0;
        long blockageStartMs = 0, blockageEndMs = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            boolean isBlockage = step >= blockageStartStep && step <= blockageEndStep;

            if (step == blockageStartStep) blockageStartMs = System.currentTimeMillis();
            if (step == blockageEndStep + 1) blockageEndMs = System.currentTimeMillis();

            for (int t = 0; t < taskCounts[step]; t++) {
                Runnable task = isBlockage ? blockingTask : IO_TASK;
                try {
                    executor.submit(() -> { task.run(); completed.increment();
                        if (isBlockage) completedBlockage.increment();
                        else completedRecovery.increment();
                    });
                } catch (RejectedExecutionException e) {
                    // 被拒绝的任务视为等效超时
                    timedOutEquivalent.increment();
                }
            }

            int qs = executor.getQueue().size();
            if (isBlockage) {
                queueSumBlockage.add(qs);
                blockageSamples++;
            } else if (step > blockageEndStep) {
                recoverySamples++;
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        if (blockageEndMs == 0) blockageEndMs = System.currentTimeMillis();

        long endMs = System.currentTimeMillis();
        long blockageDurationMs = Math.max(1, blockageEndMs - blockageStartMs);
        long recoveryDurationMs = Math.max(1, endMs - blockageEndMs);

        double blockageThroughput = blockageDurationMs > 0
                ? (double) completedBlockage.sum() / (blockageDurationMs / 1000.0) : 0;
        double recoveryThroughput = recoveryDurationMs > 0
                ? (double) completedRecovery.sum() / (recoveryDurationMs / 1000.0) : 0;
        double blockageQueue = blockageSamples > 0
                ? (double) queueSumBlockage.sum() / blockageSamples : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new BlockageResult(config.name, completed.sum(), timedOutEquivalent.sum(),
                blockageQueue, blockageThroughput, recoveryThroughput, recoveryDurationMs);
    }

    private BlockageResult runAdaptiveBlockageExperiment(
            PoolConfig config, int[] taskCounts, Runnable blockingTask,
            int blockageStartStep, int blockageEndStep) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder timedOutEquivalent = new LongAdder();
        LongAdder queueSumBlockage = new LongAdder();
        LongAdder completedBlockage = new LongAdder();
        LongAdder completedRecovery = new LongAdder();
        int blockageSamples = 0, recoverySamples = 0;
        long blockageStartMs = 0, blockageEndMs = 0;

        int currentCore = 2;
        int consecutivePressure = 0;
        int lastAdjustStep = -10;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            boolean isBlockage = step >= blockageStartStep && step <= blockageEndStep;

            if (step == blockageStartStep) blockageStartMs = System.currentTimeMillis();
            if (step == blockageEndStep + 1) blockageEndMs = System.currentTimeMillis();

            for (int t = 0; t < taskCounts[step]; t++) {
                Runnable task = isBlockage ? blockingTask : IO_TASK;
                try {
                    executor.submit(() -> { task.run(); completed.increment();
                        if (isBlockage) completedBlockage.increment();
                        else completedRecovery.increment();
                    });
                } catch (RejectedExecutionException e) {
                    timedOutEquivalent.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();
            if (isBlockage) {
                queueSumBlockage.add(qs);
                blockageSamples++;
            } else if (step > blockageEndStep) {
                recoverySamples++;
            }

            // 自适应逻辑: 阻塞期间检测队列堆积
            if (step - lastAdjustStep >= 3) {
                if (qs > 5 && ac >= currentCore && currentCore < config.max) {
                    consecutivePressure++;
                    if (consecutivePressure >= 2) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        if (blockageEndMs == 0) blockageEndMs = System.currentTimeMillis();

        long endMs = System.currentTimeMillis();
        long blockageDurationMs = Math.max(1, blockageEndMs - blockageStartMs);
        long recoveryDurationMs = Math.max(1, endMs - blockageEndMs);

        double blockageThroughput = blockageDurationMs > 0
                ? (double) completedBlockage.sum() / (blockageDurationMs / 1000.0) : 0;
        double recoveryThroughput = recoveryDurationMs > 0
                ? (double) completedRecovery.sum() / (recoveryDurationMs / 1000.0) : 0;
        double blockageQueue = blockageSamples > 0
                ? (double) queueSumBlockage.sum() / blockageSamples : 0;

        System.out.printf("  [自适应阻塞] 最终core=%d 阻塞期队列=%.1f 阻塞期吞吐=%.1f 恢复期吞吐=%.1f%n",
                currentCore, blockageQueue, blockageThroughput, recoveryThroughput);

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new BlockageResult(config.name, completed.sum(), timedOutEquivalent.sum(),
                blockageQueue, blockageThroughput, recoveryThroughput, recoveryDurationMs);
    }

    private MixedResult runMixedExperiment(
            PoolConfig config, int[] taskCounts, double ioRatio, Runnable cpuTask)
            throws Exception {
        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        LongAdder cpuCompleted = new LongAdder();
        LongAdder ioCompleted = new LongAdder();
        int sampleCount = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                boolean isIo = ThreadLocalRandom.current().nextDouble() < ioRatio;
                Runnable task = isIo ? IO_TASK : cpuTask;
                try {
                    executor.submit(() -> { task.run(); completed.increment();
                        if (isIo) ioCompleted.increment();
                        else cpuCompleted.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            queueSum.add(executor.getQueue().size());
            sampleCount++;
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new MixedResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, throughput, cpuCompleted.sum(), ioCompleted.sum());
    }

    private MixedResult runAdaptiveMixedExperiment(
            PoolConfig config, int[] taskCounts, double ioRatio, Runnable cpuTask)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        LongAdder cpuCompleted = new LongAdder();
        LongAdder ioCompleted = new LongAdder();
        int sampleCount = 0;

        int currentCore = 2;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;
        int lastAdjustStep = -10;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                boolean isIo = ThreadLocalRandom.current().nextDouble() < ioRatio;
                Runnable task = isIo ? IO_TASK : cpuTask;
                try {
                    executor.submit(() -> { task.run(); completed.increment();
                        if (isIo) ioCompleted.increment();
                        else cpuCompleted.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();
            queueSum.add(qs);
            sampleCount++;

            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && ac >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else if (qs <= 2 && ac <= currentCore / 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 3 && currentCore > 2) {
                        int newCore = Math.max(currentCore - 1, 2);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new MixedResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, throughput, cpuCompleted.sum(), ioCompleted.sum());
    }

    // ── 实验11-16 运行器 ───────────────────────────────────────────────

    private QueueWaitResult runQueueWaitExperiment(PoolConfig config, int[] taskCounts)
            throws Exception {
        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        List<Long> queueWaits = Collections.synchronizedList(new ArrayList<>());
        List<Long> execTimes = Collections.synchronizedList(new ArrayList<>());
        LongAdder totalWait = new LongAdder();
        LongAdder totalExec = new LongAdder();
        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                long submitNs = System.nanoTime();
                try {
                    executor.submit(() -> {
                        long startNs = System.nanoTime();
                        long queueWaitMs = (startNs - submitNs) / 1_000_000;
                        IO_TASK.run();
                        long endNs = System.nanoTime();
                        long execMs = (endNs - startNs) / 1_000_000;
                        queueWaits.add(queueWaitMs);
                        execTimes.add(execMs);
                        totalWait.add(queueWaitMs);
                        totalExec.add(execMs);
                        completed.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        List<Long> qSorted = new ArrayList<>(queueWaits);
        List<Long> eSorted = new ArrayList<>(execTimes);
        Collections.sort(qSorted);
        Collections.sort(eSorted);
        int n = qSorted.size();

        return new QueueWaitResult(config.name, completed.sum(), rejected.sum(),
                n > 0 ? qSorted.get(n / 2) : 0,
                n > 0 ? qSorted.get((int) (n * 0.95)) : 0,
                n > 0 ? eSorted.get(n / 2) : 0,
                n > 0 ? eSorted.get((int) (n * 0.95)) : 0,
                totalWait.sum(), totalExec.sum());
    }

    private QueueWaitResult runAdaptiveQueueWaitExperiment(
            PoolConfig config, int[] taskCounts) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        List<Long> queueWaits = Collections.synchronizedList(new ArrayList<>());
        List<Long> execTimes = Collections.synchronizedList(new ArrayList<>());
        LongAdder totalWait = new LongAdder();
        LongAdder totalExec = new LongAdder();
        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        int currentCore = 2;
        int consecutivePressure = 0;
        int lastAdjustStep = -10;

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                long submitNs = System.nanoTime();
                try {
                    executor.submit(() -> {
                        long startNs = System.nanoTime();
                        long queueWaitMs = (startNs - submitNs) / 1_000_000;
                        IO_TASK.run();
                        long endNs = System.nanoTime();
                        long execMs = (endNs - startNs) / 1_000_000;
                        queueWaits.add(queueWaitMs);
                        execTimes.add(execMs);
                        totalWait.add(queueWaitMs);
                        totalExec.add(execMs);
                        completed.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && executor.getActiveCount() >= currentCore) {
                    consecutivePressure++;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else { consecutivePressure = 0; }
            }
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }
        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        List<Long> qSorted = new ArrayList<>(queueWaits);
        List<Long> eSorted = new ArrayList<>(execTimes);
        Collections.sort(qSorted);
        Collections.sort(eSorted);
        int n = qSorted.size();

        return new QueueWaitResult(config.name, completed.sum(), rejected.sum(),
                n > 0 ? qSorted.get(n / 2) : 0,
                n > 0 ? qSorted.get((int) (n * 0.95)) : 0,
                n > 0 ? eSorted.get(n / 2) : 0,
                n > 0 ? eSorted.get((int) (n * 0.95)) : 0,
                totalWait.sum(), totalExec.sum());
    }

    private OverloadResult runOverloadExperiment(
            PoolConfig config, int[] taskCounts, RejectedExecutionHandler handler)
            throws Exception {
        BlockingQueue<Runnable> queue = config.queue > 100000
                ? new LinkedBlockingQueue<>(Integer.MAX_VALUE)
                : new LinkedBlockingQueue<>(config.queue);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                config.core, config.max, 60L, TimeUnit.SECONDS, queue, handler);
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        AtomicLong maxQueueObserved = new AtomicLong(0);
        LongAdder completedFirstHalf = new LongAdder();
        LongAdder completedSecondHalf = new LongAdder();
        int midpoint = taskCounts.length / 2;
        int sampleCount = 0;

        long startMs = System.currentTimeMillis();
        long midMs = 0;

        for (int step = 0; step < taskCounts.length; step++) {
            if (step == midpoint) midMs = System.currentTimeMillis();

            final int currentStep = step;
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment();
                        if (currentStep < midpoint) completedFirstHalf.increment();
                        else completedSecondHalf.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            queueSum.add(executor.getQueue().size());
            maxQueueObserved.updateAndGet(v -> Math.max(v, executor.getQueue().size()));
            sampleCount++;
            Thread.sleep(50);
        }

        if (midMs == 0) midMs = startMs;

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        long firstHalfMs = Math.max(1, midMs - startMs);
        long secondHalfMs = Math.max(1, endMs - midMs);

        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;
        double tpFirstHalf = (double) completedFirstHalf.sum() / (firstHalfMs / 1000.0);
        double tpSecondHalf = (double) completedSecondHalf.sum() / (secondHalfMs / 1000.0);

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new OverloadResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, (int) maxQueueObserved.get(), throughput,
                tpFirstHalf, tpSecondHalf, endMs - startMs);
    }

    private OverloadResult runAdaptiveOverloadExperiment(
            PoolConfig config, int[] taskCounts, RejectedExecutionHandler handler)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue), handler);

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        AtomicLong maxQueueObserved = new AtomicLong(0);
        LongAdder completedFirstHalf = new LongAdder();
        LongAdder completedSecondHalf = new LongAdder();
        int midpoint = taskCounts.length / 2;
        int sampleCount = 0;

        int currentCore = 2;
        int consecutivePressure = 0;
        int lastAdjustStep = -10;

        long startMs = System.currentTimeMillis();
        long midMs = 0;

        for (int step = 0; step < taskCounts.length; step++) {
            if (step == midpoint) midMs = System.currentTimeMillis();

            final int currentStep = step;
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment();
                        if (currentStep < midpoint) completedFirstHalf.increment();
                        else completedSecondHalf.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            queueSum.add(qs);
            maxQueueObserved.updateAndGet(v -> Math.max(v, qs));
            sampleCount++;

            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && executor.getActiveCount() >= currentCore) {
                    consecutivePressure++;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else { consecutivePressure = 0; }
            }

            Thread.sleep(50);
        }

        if (midMs == 0) midMs = startMs;

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        long firstHalfMs = Math.max(1, midMs - startMs);
        long secondHalfMs = Math.max(1, endMs - midMs);

        double avgQueue = sampleCount > 0 ? (double) queueSum.sum() / sampleCount : 0;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new OverloadResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, (int) maxQueueObserved.get(), throughput,
                (double) completedFirstHalf.sum() / (firstHalfMs / 1000.0),
                (double) completedSecondHalf.sum() / (secondHalfMs / 1000.0),
                endMs - startMs);
    }

    private MemoryResult runMemoryExperiment(PoolConfig config, int[] taskCounts)
            throws Exception {
        ThreadPoolExecutor executor = (config.queue == 0)
                ? new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy())
                : new ThreadPoolExecutor(config.core, config.max,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.queue > 100000 ? Integer.MAX_VALUE : config.queue),
                    new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder threadSum = new LongAdder();
        AtomicLong peakThreads = new AtomicLong(0);
        AtomicLong peakQueue = new AtomicLong(0);
        int sampleCount = 0;

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int poolSize = executor.getPoolSize();
            int qs = executor.getQueue().size();
            threadSum.add(poolSize);
            peakThreads.updateAndGet(v -> Math.max(v, poolSize));
            peakQueue.updateAndGet(v -> Math.max(v, qs));
            sampleCount++;
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        double avgThreads = sampleCount > 0 ? (double) threadSum.sum() / sampleCount : 0;
        return new MemoryResult(config.name, (int) peakThreads.get(),
                (int) peakQueue.get(), avgThreads);
    }

    private MemoryResult runAdaptiveMemoryExperiment(
            PoolConfig config, int[] taskCounts) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder threadSum = new LongAdder();
        AtomicLong peakThreads = new AtomicLong(0);
        AtomicLong peakQueue = new AtomicLong(0);
        int sampleCount = 0;

        int currentCore = 2;
        int consecutivePressure = 0;
        int lastAdjustStep = -10;

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int poolSize = executor.getPoolSize();
            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();
            threadSum.add(poolSize);
            peakThreads.updateAndGet(v -> Math.max(v, poolSize));
            peakQueue.updateAndGet(v -> Math.max(v, qs));
            sampleCount++;

            if (step - lastAdjustStep >= 5) {
                if (qs > config.queue / 2 && ac >= currentCore) {
                    consecutivePressure++;
                    if (consecutivePressure >= 2 && currentCore < config.max) {
                        int newCore = Math.min(currentCore + 2, config.max);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else { consecutivePressure = 0; }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        double avgThreads = sampleCount > 0 ? (double) threadSum.sum() / sampleCount : 0;
        return new MemoryResult(config.name, (int) peakThreads.get(),
                (int) peakQueue.get(), avgThreads);
    }

    private RejectionResult runRejectionExperiment(
            PoolConfig config, int[] taskCounts, RejectedExecutionHandler handler)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                config.core, config.max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queue), handler);

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder callerExecuted = new LongAdder();
        LongAdder callerExecNs = new LongAdder();
        AtomicLong peakQueue = new AtomicLong(0);
        LongAdder discardedOldest = new LongAdder();

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    // CallerRunsPolicy: when queue is full, the task runs in the calling thread.
                    // We detect this by checking if the thread executing the task is NOT a pool thread.
                    final String callerThreadName = Thread.currentThread().getName();
                    executor.submit(() -> {
                        IO_TASK.run();
                        completed.increment();
                        if (Thread.currentThread().getName().equals(callerThreadName)) {
                            callerExecuted.increment();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    if (handler instanceof ThreadPoolExecutor.AbortPolicy) {
                        rejected.increment();
                    } else if (handler instanceof ThreadPoolExecutor.DiscardOldestPolicy) {
                        discardedOldest.increment();
                    }
                }
            }
            peakQueue.updateAndGet(v -> Math.max(v, executor.getQueue().size()));
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new RejectionResult(config.name, completed.sum(), callerExecuted.sum(),
                callerExecuted.sum() > 0 ? (double) callerExecNs.sum() / callerExecuted.sum() / 1_000_000 : 0,
                discardedOldest.sum(), throughput, (int) peakQueue.get());
    }

    private QueueTypeResult runQueueTypeExperiment(
            PoolConfig config, int[] taskCounts, boolean useLinked) throws Exception {
        BlockingQueue<Runnable> queue = useLinked
                ? new LinkedBlockingQueue<>(config.queue)
                : new ArrayBlockingQueue<>(config.queue);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                config.core, config.max, 60L, TimeUnit.SECONDS, queue,
                new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        LongAdder queueSum = new LongAdder();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> {
                        long t0 = System.nanoTime();
                        IO_TASK.run();
                        latencies.add((System.nanoTime() - t0) / 1_000_000);
                        completed.increment();
                    });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }
            queueSum.add(executor.getQueue().size());
            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double avgQueue = (double) queueSum.sum() / taskCounts.length;
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        Collections.sort(latencies);
        double p95 = latencies.size() > 0
                ? latencies.get((int) (latencies.size() * 0.95)) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new QueueTypeResult(config.name, completed.sum(), rejected.sum(),
                avgQueue, throughput, endMs - startMs, p95);
    }

    private SensitivityResult runSensitivityExperiment(
            int coreMin, int coreMax, int queueCap,
            int cooldownSteps, int pressureThreshold, int stepSize,
            int[] taskCounts) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreMin, coreMax, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCap),
                new ThreadPoolExecutor.AbortPolicy());

        LongAdder completed = new LongAdder();
        LongAdder rejected = new LongAdder();
        int currentCore = coreMin;
        int consecutivePressure = 0;
        int consecutiveRelief = 0;
        int lastAdjustStep = -cooldownSteps;
        int adjustCount = 0;
        int lastDirection = 0;
        int oscillationCount = 0;

        long startMs = System.currentTimeMillis();

        for (int step = 0; step < taskCounts.length; step++) {
            for (int t = 0; t < taskCounts[step]; t++) {
                try {
                    executor.submit(() -> { IO_TASK.run(); completed.increment(); });
                } catch (RejectedExecutionException e) {
                    rejected.increment();
                }
            }

            int qs = executor.getQueue().size();
            int ac = executor.getActiveCount();

            if (step - lastAdjustStep >= cooldownSteps) {
                if (qs > pressureThreshold && ac >= currentCore) {
                    consecutivePressure++;
                    consecutiveRelief = 0;
                    if (consecutivePressure >= 2 && currentCore < coreMax) {
                        int newCore = Math.min(currentCore + stepSize, coreMax);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize()));
                        executor.setCorePoolSize(newCore);
                        adjustCount++;
                        int dir = 1;
                        if (lastDirection != 0 && dir != lastDirection) oscillationCount++;
                        lastDirection = dir;
                        currentCore = newCore;
                        consecutivePressure = 0;
                        lastAdjustStep = step;
                    }
                } else if (qs <= 2 && ac <= currentCore / 2) {
                    consecutiveRelief++;
                    consecutivePressure = 0;
                    if (consecutiveRelief >= 3 && currentCore > coreMin) {
                        int newCore = Math.max(currentCore - Math.max(1, stepSize / 2), coreMin);
                        executor.setCorePoolSize(newCore);
                        executor.setMaximumPoolSize(Math.max(newCore, executor.getActiveCount()));
                        adjustCount++;
                        int dir = -1;
                        if (lastDirection != 0 && dir != lastDirection) oscillationCount++;
                        lastDirection = dir;
                        currentCore = newCore;
                        consecutiveRelief = 0;
                        lastAdjustStep = step;
                    }
                } else {
                    consecutivePressure = 0;
                    consecutiveRelief = 0;
                }
            }

            Thread.sleep(50);
        }

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueue().isEmpty() && executor.getActiveCount() == 0) break;
            Thread.sleep(20);
        }

        long endMs = System.currentTimeMillis();
        double throughput = (endMs - startMs) > 0
                ? (double) completed.sum() / ((endMs - startMs) / 1000.0) : 0;

        executor.shutdown();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (!executor.isTerminated()) executor.shutdownNow();

        return new SensitivityResult("sensitivity", completed.sum(), rejected.sum(),
                adjustCount, throughput, currentCore, oscillationCount);
    }

    // ── 负载生成器 ────────────────────────────────────────────────────

    private static int[] steady(int steps, int perStep) {
        int[] a = new int[steps];
        for (int i = 0; i < steps; i++) a[i] = perStep;
        return a;
    }

    private static int[] ramp(int steps, int start, int end) {
        int[] a = new int[steps];
        for (int i = 0; i < steps; i++) {
            a[i] = start + (end - start) * i / Math.max(1, steps - 1);
        }
        return a;
    }

    private static int[] burst(int steps, int spike, int normal) {
        int[] a = new int[steps];
        for (int i = 0; i < steps; i++) {
            a[i] = (i % 3 == 0) ? spike : normal;
        }
        return a;
    }

    // ── 输出 ──────────────────────────────────────────────────────────

    private void printStaticComparisonTable(
            List<ExperimentResult> results, PoolConfig[] pools, String[] workloadNames) {

        for (String wlName : workloadNames) {
            // 汇总该负载下各方案的 3 轮平均值
            System.out.println("\n── 负载: " + wlName + " ──");
            System.out.printf("%-24s | %8s | %8s | %8s | %8s | %10s | %8s%n",
                    "方案", "完成", "拒绝", "平均队列", "最大队列", "吞吐/s", "耗时ms");
            System.out.println("-".repeat(100));

            for (PoolConfig pool : pools) {
                List<ExperimentResult> rounds = results.stream()
                        .filter(r -> r.poolName.equals(pool.name) && r.workloadName.equals(wlName))
                        .toList();

                if (rounds.isEmpty()) continue;

                double avgCompleted = rounds.stream().mapToLong(ExperimentResult::completed).average().orElse(0);
                double avgRejected = rounds.stream().mapToLong(ExperimentResult::rejected).average().orElse(0);
                double avgQueue = rounds.stream().mapToDouble(ExperimentResult::avgQueueDepth).average().orElse(0);
                double avgMaxQueue = rounds.stream().mapToInt(ExperimentResult::maxQueueDepth).average().orElse(0);
                double avgThroughput = rounds.stream().mapToDouble(ExperimentResult::throughput).average().orElse(0);
                double avgDuration = rounds.stream().mapToLong(ExperimentResult::durationMs).average().orElse(0);

                System.out.printf("%-24s | %8.0f | %8.0f | %8.1f | %8.0f | %10.1f | %8.0f%n",
                        pool.name, avgCompleted, avgRejected, avgQueue, avgMaxQueue,
                        avgThroughput, avgDuration);
            }

            // 最佳方案
            ExperimentResult best = results.stream()
                    .filter(r -> r.workloadName.equals(wlName))
                    .min((a, b) -> {
                        if (a.rejected != b.rejected) return Long.compare(a.rejected, b.rejected);
                        return Double.compare(b.throughput, a.throughput);
                    }).orElse(null);
            ExperimentResult worst = results.stream()
                    .filter(r -> r.workloadName.equals(wlName))
                    .max((a, b) -> {
                        if (a.rejected != b.rejected) return Long.compare(a.rejected, b.rejected);
                        return Double.compare(b.throughput, a.throughput);
                    }).orElse(null);

            if (best != null && worst != null) {
                System.out.printf("  → 最佳: %s (拒绝%.0f, 吞吐%.1f/s) | 最差: %s (拒绝%.0f, 吞吐%.1f/s)%n",
                        best.poolName, (double) best.rejected, best.throughput,
                        worst.poolName, (double) worst.rejected, worst.throughput);
            }
        }
    }
}
