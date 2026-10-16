package com.genewarrior.genewarrior.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:primer-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "genewarrior.jobs.worker-enabled=false"})
@AutoConfigureMockMvc
class PrimerJobApiTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JobService jobs;
    @Autowired
    TcoffeeService tcoffee;
    @Autowired
    GuideTreeService guideTrees;
    @Autowired
    JdbcTemplate db;
    @MockitoBean
    PrimerDesignService primer3;

    @BeforeEach
    void setup() {
        db.update("delete from tool_job");
        when(primer3.capabilities()).thenReturn(new PrimerDesignService.Capabilities(true, "2.6.1", 20000));
        when(primer3.design(anyString(), any(), any(), any())).thenAnswer(call -> PrimerFixtures.result(call.getArgument(0), true));
    }

    PrimerDesignRequest request() {
        return new PrimerDesignRequest(UUID.randomUUID(), PrimerFixtures.source(), PrimerFixtures.settings(true));
    }

    @Test
    void persistsProbeInputExecutesAndSharesTypedResults() throws Exception {
        var request = request();
        mvc.perform(post("/api/primers").contentType("application/json").content(json.writeValueAsBytes(request)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.tool").value("PRIMER3"));
        assertEquals(request, json.readValue(db.queryForObject("select input from tool_job where id = ?", String.class, request.requestId().toString()), PrimerDesignRequest.class));
        new ToolJobWorker(jobs, tcoffee, primer3, guideTrees).tick();
        mvc.perform(get("/api/jobs/" + request.requestId())).andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.pairs[0].probe.strand").value("FORWARD"));
        var project = new ProjectDto(null, "Primer share", List.of(), List.of(), List.of(),
                List.of(PrimerFixtures.result(request.requestId().toString(), true)), List.of("primer3-" + request.requestId()), null, null);
        var response = mvc.perform(post("/api/projects").contentType("application/json").content(json.writeValueAsBytes(Map.of("project", project))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/projects/" + json.readTree(response).get("shareKey").asText())).andExpect(status().isOk())
                .andExpect(jsonPath("$.primerDesigns[0].pairs[0].right.start").value(151))
                .andExpect(jsonPath("$.primerDesigns[0].settings.includeProbe").value(true));
    }

    @Test
    void validatesLimitsAndSupportsCancellationAndIdempotency() throws Exception {
        mvc.perform(post("/api/primers").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/primers").servletPath("/api/primers").contentType("application/json").content(" ".repeat(262145)))
                .andExpect(status().isPayloadTooLarge());
        var request = request();
        jobs.submitPrimer(request, "primer-test");
        jobs.submitPrimer(request, "primer-test");
        assertEquals(1, db.queryForObject("select count(*) from tool_job", Integer.class));
        mvc.perform(delete("/api/jobs/" + request.requestId())).andExpect(jsonPath("$.status").value("CANCELLED"));
        new ToolJobWorker(jobs, tcoffee, primer3, guideTrees).tick();
        verify(primer3, never()).design(anyString(), any(), any(), any());
    }

    @Test
    void unavailableToolRejectsBeforeSaving() throws Exception {
        doThrow(new ExternalToolUnavailableException("Primer3 unavailable")).when(primer3).requireAvailable();
        mvc.perform(post("/api/primers").contentType("application/json").content(json.writeValueAsBytes(request())))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, db.queryForObject("select count(*) from tool_job", Integer.class));
    }
}
