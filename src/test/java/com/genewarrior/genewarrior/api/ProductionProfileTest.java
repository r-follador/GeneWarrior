package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.DnaToolsService;
import com.genewarrior.genewarrior.MainController;
import com.genewarrior.genewarrior.persistence.ToolJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.on-not-found=ignore",
        "spring.datasource.url=jdbc:h2:mem:production-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "genewarrior.tools.tcoffee.path=",
        "genewarrior.tools.primer3.path="
})
@AutoConfigureMockMvc
@ActiveProfiles({"production", "legacy"})
class ProductionProfileTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ApplicationContext context;
    @Autowired
    ToolJobRepository jobs;

    @Test
    void servesOnlyModernFrontendAndPreservesNotFoundResponses() throws Exception {
        assertTrue(context.getBeansOfType(MainController.class).isEmpty());
        assertTrue(context.getBeansOfType(DnaToolsService.class).isEmpty());
        mvc.perform(get("/").param("project", "some-share-key"))
                .andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Production test frontend")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self';")))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        for (String path : new String[]{"/api/not-found", "/assets/missing.js", "/js/dnatools.js", "/share", "/primer",
                "/sharedProjects", "/sharedProjectEntities", "/toolJobEntities", "/projectEntities"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
        mvc.perform(post("/dnatoolsServlet").param("type", "loadExample")).andExpect(status().isNotFound());
        mvc.perform(post("/readProject").param("key", "test")).andExpect(status().isNotFound());
    }

    @Test
    void exposesOnlyMinimalHealth() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/configprops")).andExpect(status().isNotFound());
    }

    @Test
    void keepsModernApisWorkingButDoesNotAcceptUnexecutableJobs() throws Exception {
        mvc.perform(post("/api/sequences/import").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\">DNA\\nACGT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sequences[0].sequence").value("ACGT"));
        long count = jobs.count();
        for (String path : new String[]{"/api/alignments", "/api/primers"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("""
                            {"requestId":"c8bc5c45-4d78-4166-b77f-9ca8438f58d0","sequences":[
                              {"name":"First","kind":"DNA","sequence":"ACGT"},
                              {"name":"Second","kind":"DNA","sequence":"ACGT"}]}
                            """))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").exists())
                    .andExpect(content().string(not(containsString("QUEUED"))));
        }
        assertEquals(count, jobs.count());
    }
}
