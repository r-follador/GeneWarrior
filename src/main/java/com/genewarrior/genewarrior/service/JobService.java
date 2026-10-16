package com.genewarrior.genewarrior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.JobProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class JobService {
    private final JdbcTemplate db;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final TcoffeeService tcoffee;
    private final PrimerDesignService primer3;
    private final GuideTreeService guideTrees;
    private final JobProperties properties;
    private final Map<String, List<Instant>> submissions = new HashMap<>();

    public JobService(JdbcTemplate db, org.springframework.transaction.PlatformTransactionManager manager,
                      ObjectMapper json, TcoffeeService tcoffee, PrimerDesignService primer3, GuideTreeService guideTrees, JobProperties properties) {
        this.db = db;
        this.transactions = new TransactionTemplate(manager);
        this.json = json;
        this.tcoffee = tcoffee;
        this.properties = properties;
        this.primer3 = primer3;
        this.guideTrees = guideTrees;
    }

    public ToolJobDto submit(MultipleAlignmentRequest request, String client) {
        List<SequenceDto> sequences = tcoffee.validate(request.sequences());
        return submit(request.requestId().toString(), "TCOFFEE", encode(sequences), client, tcoffee::requireAvailable);
    }

    public ToolJobDto submitPrimer(PrimerDesignRequest request, String client) {
        var source = PrimerDesignService.validate(request.sequence(), request.settings());
        var snapshot = new PrimerDesignRequest(request.requestId(), source, request.settings());
        return submit(request.requestId().toString(), "PRIMER3", encode(snapshot), client, primer3::requireAvailable);
    }

    public ToolJobDto submitTree(TreeRequest request, String client) {
        guideTrees.validate(request);
        return submit(request.requestId().toString(), "GUIDE_TREE", encode(request), client, tcoffee::requireAvailable);
    }

    private ToolJobDto submit(String id, String tool, String input, String client, Runnable requireAvailable) {
        return transactions.execute(status -> {
            lock();
            var existing = db.query("select tool, input from tool_job where id = ?", (rs, row) -> List.of(rs.getString(1), rs.getString(2)), id);
            if (!existing.isEmpty()) {
                if (!existing.getFirst().equals(List.of(tool, input)))
                    throw new JobAdmissionException(409, "Request ID already used for different input");
                return get(id);
            }
            requireAvailable.run();
            int count = db.queryForObject("select count(*) from tool_job where status in ('QUEUED', 'RUNNING') or lease_until > ?", Integer.class, now());
            if (count >= properties.queueCapacity())
                throw new JobAdmissionException(503, "Tool queue is full; try again later");
            rateLimit(client);
            Instant created = Instant.now();
            db.update("insert into tool_job(id, tool, status, created_at, expires_at, input) values (?, ?, 'QUEUED', ?, ?, ?)",
                    id, tool, Timestamp.from(created), Timestamp.from(created.plus(properties.retention())), input);
            return get(id);
        });
    }

    private synchronized void rateLimit(String client) {
        Instant cutoff = Instant.now().minusSeconds(60);
        submissions.values().forEach(times -> times.removeIf(time -> time.isBefore(cutoff)));
        submissions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (!submissions.containsKey(client) && submissions.size() >= 4096)
            throw new JobAdmissionException(429, "Submission rate limit reached; try again in a minute");
        List<Instant> times = submissions.computeIfAbsent(client, key -> new ArrayList<>());
        if (times.size() >= properties.submissionsPerMinute())
            throw new JobAdmissionException(429, "Submission rate limit reached; try again in a minute");
        times.add(Instant.now());
    }

    public ToolJobDto get(String id) {
        return db.query("select * from tool_job where id = ? and expires_at > ?", this::dto, id, now())
                .stream().findFirst().orElseThrow(() -> new ApiNotFoundException("Job not found"));
    }

    public ToolJobDto cancel(String id) {
        return transactions.execute(status -> {
            get(id);
            db.update("update tool_job set status = 'CANCELLED', finished_at = ?, error_code = 'CANCELLED', error = 'Job was cancelled' where id = ? and status in ('QUEUED', 'RUNNING')", now(), id);
            return get(id);
        });
    }

    public Claim claim() {
        return transactions.execute(status -> {
            lock();
            db.update("update tool_job set status = 'FAILED', finished_at = ?, error_code = 'INTERRUPTED', error = 'Execution was interrupted; retry explicitly', lease_until = null, lease_token = null where status = 'RUNNING' and lease_until < ?", now(), now());
            db.update("update tool_job set lease_until = null, lease_token = null where status = 'CANCELLED' and lease_until < ?", now());
            db.update("delete from tool_job where expires_at < ? and lease_until is null", now());
            if (db.queryForObject("select count(*) from tool_job where lease_until is not null", Integer.class) != 0)
                return null;
            List<Claim> queued = db.query("select id, tool, input from tool_job where status = 'QUEUED' and ((tool in ('TCOFFEE', 'GUIDE_TREE') and ?) or (tool = 'PRIMER3' and ?)) order by created_at, id limit 1",
                    (rs, row) -> decodeClaim(rs), tcoffee.capabilities().available(), primer3.capabilities().available());
            if (queued.isEmpty()) return null;
            Claim claim = queued.getFirst();
            db.update("update tool_job set status = 'RUNNING', started_at = ?, lease_until = ?, lease_token = ? where id = ? and status = 'QUEUED'",
                    now(), Timestamp.from(Instant.now().plusSeconds(15)), claim.token(), claim.id());
            return claim;
        });
    }

    // Called at most once per second by the worker; a cancelled or lost lease stops the process.
    public boolean heartbeat(Claim claim) {
        return db.update("update tool_job set lease_until = ? where id = ? and lease_token = ? and status = 'RUNNING' and lease_until > ?",
                Timestamp.from(Instant.now().plusSeconds(15)), claim.id(), claim.token(), now()) == 1;
    }

    public void finish(Claim claim, Object result, String code, String error) {
        transactions.executeWithoutResult(status -> {
            db.update("update tool_job set status = ?, result = ?, error_code = ?, error = ?, finished_at = ? where id = ? and lease_token = ? and status = 'RUNNING' and lease_until > ?",
                    result != null ? "SUCCEEDED" : "FAILED", result == null ? null : encode(result), code, error, now(), claim.id(), claim.token(), now());
            db.update("update tool_job set status = 'FAILED', error_code = 'INTERRUPTED', error = 'Execution lease expired; retry explicitly', finished_at = ? where id = ? and lease_token = ? and status = 'RUNNING'",
                    now(), claim.id(), claim.token());
            db.update("update tool_job set lease_until = null, lease_token = null where id = ? and lease_token = ?", claim.id(), claim.token());
        });
    }

    private void lock() {
        db.queryForObject("select id from tool_job_gate where id = 1 for update", Integer.class);
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize job data", e);
        }
    }

    private List<SequenceDto> decodeInput(String value) {
        try {
            return List.of(json.readValue(value, SequenceDto[].class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid saved job input", e);
        }
    }

    private Claim decodeClaim(ResultSet rs) throws SQLException {
        String tool = rs.getString("tool"), input = rs.getString("input");
        String id = rs.getString("id"), token = UUID.randomUUID().toString();
        if (tool.equals("TCOFFEE")) return new Claim(id, token, decodeInput(input), null, tool, null);
        try {
            if (tool.equals("GUIDE_TREE"))
                return new Claim(id, token, List.of(), null, tool, json.readValue(input, TreeRequest.class));
            var request = json.readValue(input, PrimerDesignRequest.class);
            return new Claim(id, token, List.of(request.sequence()), request.settings(), tool, null);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid saved primer input", e);
        }
    }

    private ToolJobDto dto(ResultSet rs, int row) throws SQLException {
        Object result = null;
        try {
            if (rs.getString("result") != null) result = json.readValue(rs.getString("result"), Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid saved job result", e);
        }
        return new ToolJobDto(rs.getString("id"), rs.getString("tool"), rs.getString("status"),
                instant(rs, "created_at"), instant(rs, "finished_at"), result, rs.getString("error"),
                rs.getString("error_code"), instant(rs, "started_at"), instant(rs, "expires_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public record Claim(String id, String token, List<SequenceDto> sequences, PrimerSettings settings, String tool,
                        TreeRequest tree) {
    }
}
