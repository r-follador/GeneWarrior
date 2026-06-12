package com.genewarrior.genewarrior.config;

import com.genewarrior.genewarrior.service.ExternalToolRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ToolHealthCheck implements ApplicationRunner {
    private final ExternalToolRunner externalToolRunner;

    public ToolHealthCheck(ExternalToolRunner externalToolRunner) {
        this.externalToolRunner = externalToolRunner;
    }

    @Override
    public void run(ApplicationArguments args) {
        externalToolRunner.checkConfiguredTools();
    }
}
