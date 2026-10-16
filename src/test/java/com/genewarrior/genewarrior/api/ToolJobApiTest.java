package com.genewarrior.genewarrior.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:job-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "genewarrior.jobs.worker-enabled=false", "genewarrior.jobs.queue-capacity=2", "genewarrior.jobs.submissions-per-minute=2",
        "genewarrior.tools.tcoffee.path=/test/t_coffee"})
@AutoConfigureMockMvc
class ToolJobApiTest {
    @Autowired
    GuideTreeService guideTrees;
    @Autowired
    MockMvc mvc;
    @Autowired
    JobService jobs;
    @Autowired
    TcoffeeService tcoffee;
    @Autowired
    PrimerDesignService primer3;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate db;
    @MockitoBean
    ExternalToolRunner runner;
    String client;

    @BeforeEach
    void setup() {
        db.update("delete from tool_job");
        client = UUID.randomUUID().toString();
        when(runner.available(anyString())).thenReturn(true);
        when(runner.run(anyList(), anyString())).thenReturn(new ExternalToolRunner.ToolRunResult(0, "T-COFFEE Version_13.41.0.28bdc39", "", null));
        when(runner.run(anyList(), anyString(), anyMap(), anyString(), any())).thenReturn(
                new ExternalToolRunner.ToolRunResult(0, "", "", ">seq0002\nAC-T\n>seq0001\nACGT\n"));
        tcoffee.probe();
    }

    MultipleAlignmentRequest request() {
        return new MultipleAlignmentRequest(UUID.randomUUID(), List.of(
                new SequenceDto("one", "First", SequenceKind.DNA, "ACGT", List.of()),
                new SequenceDto("two", "Second", SequenceKind.DNA, "ACT", List.of())));
    }

    @Test
    void persistsBeforeAcceptingAndExecutesQueuedWorkWithNewWorker() throws Exception {
        var request = request();
        mvc.perform(post("/api/alignments").contentType("application/json").content(json.writeValueAsBytes(request)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("QUEUED"));
        assertNotNull(db.queryForObject("select input from tool_job where id = ?", String.class, request.requestId().toString()));
        new ToolJobWorker(jobs, tcoffee, primer3, guideTrees).tick();
        mvc.perform(get("/api/jobs/" + request.requestId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.sequences[0].value").value("ACGT"))
                .andExpect(jsonPath("$.result.provenance.tool").value("T-Coffee"))
                .andExpect(jsonPath("$.result.score").isEmpty());
    }

    @Test
    void deduplicatesLostResponsesAndRejectsConflictingRequests() {
        var request = request();
        assertEquals(jobs.submit(request, client).id(), jobs.submit(request, client).id());
        assertEquals(1, db.queryForObject("select count(*) from tool_job", Integer.class));
        var conflict = new MultipleAlignmentRequest(request.requestId(), List.of(request.sequences().getFirst(), request.sequences().getFirst()));
        assertEquals(409, assertThrows(JobAdmissionException.class, () -> jobs.submit(conflict, client)).status());
    }

    @Test
    void guideTreeJobsPersistSnapshotsAndShareStructuredResults() throws Exception {
        var input = new TreeRequest(UUID.randomUUID(), "alignment", "Tree sample", SequenceKind.DNA,
                List.of(new AlignmentSequenceDto("Duplicate", "ACGT"), new AlignmentSequenceDto("Duplicate", "AC-T"), new AlignmentSequenceDto("Third", "A-GT")));
        when(runner.run(anyList(), anyString(), anyMap(), eq("guide.nwk"), any())).thenReturn(
                new ExternalToolRunner.ToolRunResult(0, "", "", "(seq0001:0.1,seq0002:0.2,seq0003:0.1);"));
        mvc.perform(post("/api/trees").contentType("application/json").content(json.writeValueAsBytes(input)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.tool").value("GUIDE_TREE"));
        assertEquals(input, json.readValue(db.queryForObject("select input from tool_job where id = ?", String.class, input.requestId().toString()), TreeRequest.class));
        new ToolJobWorker(jobs, tcoffee, primer3, guideTrees).tick();
        var job = jobs.get(input.requestId().toString());
        assertEquals("SUCCEEDED", job.status());
        var tree = json.convertValue(job.result(), GuideTreeDto.class);
        assertEquals(4, tree.nodes().size());
        var project = new ProjectDto(null, "Tree project", List.of(), List.of(), List.of(), List.of(), List.of(tree), List.of(tree.id()), null, null);
        var saved = mvc.perform(post("/api/projects").contentType("application/json").content(json.writeValueAsBytes(java.util.Map.of("project", project))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/projects/" + json.readTree(saved).get("shareKey").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trees[0].nodes[2].sourceIndex").value(1));
    }

    @Test
    void treeValidationCancellationAndAvailabilityUseExistingJobContracts() throws Exception {
        var input = new TreeRequest(UUID.randomUUID(), "alignment", "Tree", SequenceKind.DNA,
                List.of(new AlignmentSequenceDto("One", "ACGT"), new AlignmentSequenceDto("Two", "AC-T"), new AlignmentSequenceDto("Three", "A-GT")));
        mvc.perform(post("/api/trees").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        var pairwise = new TreeRequest(UUID.randomUUID(), input.sourceAlignmentId(), input.name(), input.kind(), input.sequences().subList(0, 2));
        mvc.perform(post("/api/trees").contentType("application/json").content(json.writeValueAsBytes(pairwise))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/trees").servletPath("/api/trees").contentType("application/json").content(" ".repeat(262145))).andExpect(status().isPayloadTooLarge());
        var job = jobs.submitTree(input, client);
        assertEquals(job.id(), jobs.submitTree(input, client).id());
        assertEquals("CANCELLED", jobs.cancel(job.id()).status());
        when(runner.available(anyString())).thenReturn(false);
        var next = new TreeRequest(UUID.randomUUID(), input.sourceAlignmentId(), input.name(), input.kind(), input.sequences());
        mvc.perform(post("/api/trees").contentType("application/json").content(json.writeValueAsBytes(next))).andExpect(status().isServiceUnavailable());
    }

    @Test
    void limitsQueueAndClientRate() {
        var first = jobs.submit(request(), client);
        jobs.submit(request(), client);
        assertEquals(503, assertThrows(JobAdmissionException.class, () -> jobs.submit(request(), "another")).status());
        jobs.cancel(first.id());
        assertEquals(429, assertThrows(JobAdmissionException.class, () -> jobs.submit(request(), client)).status());
    }

    @Test
    void cancellationWinsAgainstCompletionAndReservesWorkerUntilProcessStops() {
        var first = jobs.submit(request(), client);
        jobs.submit(request(), client);
        var claim = jobs.claim();
        assertNotNull(claim);
        jobs.cancel(claim.id());
        assertFalse(jobs.heartbeat(claim));
        assertNull(jobs.claim());
        jobs.finish(claim, null, "FAILED", "Late result");
        assertEquals("CANCELLED", jobs.get(claim.id()).status());
        assertNotNull(jobs.claim());
    }

    @Test
    void recoversExpiredLeaseWithoutReexecutingAndRetainsQueuedInputs() {
        jobs.submit(request(), client);
        var interrupted = jobs.claim();
        var queued = jobs.submit(request(), client);
        db.update("update tool_job set lease_until = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(1)), interrupted.id());
        var resumed = jobs.claim();
        assertEquals(queued.id(), resumed.id());
        assertEquals("FAILED", jobs.get(interrupted.id()).status());
        assertEquals("INTERRUPTED", jobs.get(interrupted.id()).errorCode());
        assertFalse(jobs.heartbeat(interrupted));
        assertEquals(2, resumed.sequences().size());
    }

    @Test
    void recordsToolFailureAndExpiresResults() {
        var first = jobs.submit(request(), client);
        when(runner.run(anyList(), anyString(), anyMap(), anyString(), any()))
                .thenThrow(new ToolExecutionException("TIMEOUT", "External tool timed out"));
        new ToolJobWorker(jobs, tcoffee, primer3, guideTrees).tick();
        assertEquals("FAILED", jobs.get(first.id()).status());
        assertEquals("TIMEOUT", jobs.get(first.id()).errorCode());
        db.update("update tool_job set expires_at = ?", Timestamp.from(Instant.now().minusSeconds(1)));
        assertThrows(ApiNotFoundException.class, () -> jobs.get(first.id()));
        jobs.claim();
        assertEquals(0, db.queryForObject("select count(*) from tool_job", Integer.class));
    }

    @Test
    void aLateCompletionCannotSucceedAfterItsLeaseExpires() {
        jobs.submit(request(), client);
        var claim = jobs.claim();
        db.update("update tool_job set lease_until = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(1)), claim.id());
        jobs.finish(claim, new AlignmentDto("late", "Late", SequenceKind.DNA,
                List.of(new AlignmentSequenceDto("One", "ACGT"), new AlignmentSequenceDto("Two", "AC-T")), "** *", null, List.of()), null, null);
        assertEquals("FAILED", jobs.get(claim.id()).status());
        assertEquals("INTERRUPTED", jobs.get(claim.id()).errorCode());
        assertNull(jobs.get(claim.id()).result());
    }

    @Test
    void validatesJsonPayloadLimitAndMissingCapabilities() throws Exception {
        mvc.perform(post("/api/alignments").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/alignments").servletPath("/api/alignments").contentType("application/json").content(" ".repeat(262145)))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(delete("/api/jobs/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tools/tcoffee")).andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        when(runner.available(anyString())).thenReturn(false);
        mvc.perform(post("/api/alignments").contentType("application/json").content(json.writeValueAsBytes(request())))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, db.queryForObject("select count(*) from tool_job", Integer.class));
    }
}
