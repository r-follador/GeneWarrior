package com.genewarrior.genewarrior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.ToolJobDto;
import com.genewarrior.genewarrior.persistence.ToolJobEntity;
import com.genewarrior.genewarrior.persistence.ToolJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {
    private final ToolJobRepository repository;
    private final ObjectMapper objectMapper;

    public JobService(ToolJobRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ToolJobDto createQueued(String tool) {
        ToolJobEntity entity = new ToolJobEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTool(tool);
        entity.setStatus("QUEUED");
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public ToolJobDto get(String id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ApiNotFoundException("Job not found"));
    }

    public ToolJobDto toDto(ToolJobEntity entity) {
        return new ToolJobDto(entity.getId(), entity.getTool(), entity.getStatus(), entity.getCreatedAt(), entity.getFinishedAt(), parseResult(entity.getResult()), entity.getError());
    }

    private Object parseResult(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(result, Object.class);
        } catch (JsonProcessingException e) {
            return result;
        }
    }
}
