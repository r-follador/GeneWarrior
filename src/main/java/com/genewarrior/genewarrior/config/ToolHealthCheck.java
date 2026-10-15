package com.genewarrior.genewarrior.config;

import com.genewarrior.genewarrior.service.ExternalToolRunner;
import com.genewarrior.genewarrior.service.TcoffeeService;
import com.genewarrior.genewarrior.service.PrimerDesignService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ToolHealthCheck implements ApplicationRunner {
    private final ExternalToolRunner externalToolRunner;
    private final TcoffeeService tcoffee;
    private final PrimerDesignService primer3;

    public ToolHealthCheck(ExternalToolRunner externalToolRunner, TcoffeeService tcoffee, PrimerDesignService primer3) {
        this.externalToolRunner = externalToolRunner;
        this.tcoffee = tcoffee;
        this.primer3 = primer3;
    }

    @Override
    public void run(ApplicationArguments args) {
        externalToolRunner.checkConfiguredTools();
        tcoffee.probe();
        primer3.probe();
    }
}
