package com.genewarrior.genewarrior.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "genewarrior.jobs.worker-enabled", matchIfMissing = true)
public class ToolJobWorker {
    private final JobService jobs;
    private final TcoffeeService tcoffee;
    private final PrimerDesignService primer3;

    public ToolJobWorker(JobService jobs, TcoffeeService tcoffee, PrimerDesignService primer3) {
        this.jobs = jobs;
        this.tcoffee = tcoffee;
        this.primer3 = primer3;
    }

    @Scheduled(fixedDelayString = "${genewarrior.jobs.poll-delay:1000}", initialDelay = 1000)
    public void tick() {
        var claim = jobs.claim();
        if (claim == null) return;
        try {
            long[] nextHeartbeat = {0};
            java.util.function.BooleanSupplier cancelled = () -> {
                if (System.nanoTime() < nextHeartbeat[0]) return false;
                nextHeartbeat[0] = System.nanoTime() + 1_000_000_000;
                return !jobs.heartbeat(claim);
            };
            Object result = claim.tool().equals("PRIMER3")
                    ? primer3.design(claim.id(), claim.sequences().getFirst(), claim.settings(), cancelled)
                    : tcoffee.align(claim.id(), claim.sequences(), cancelled);
            jobs.finish(claim, result, null, null);
        } catch (ToolExecutionException e) {
            jobs.finish(claim, null, e.code(), e.getMessage());
        } catch (RuntimeException e) {
            jobs.finish(claim, null, "EXECUTION_FAILED", "Tool execution failed; retry explicitly");
        }
    }
}
