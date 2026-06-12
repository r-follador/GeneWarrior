package com.genewarrior.genewarrior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.ProjectDto;
import com.genewarrior.genewarrior.api.dto.SaveProjectResponse;
import com.genewarrior.genewarrior.persistence.SharedProjectEntity;
import com.genewarrior.genewarrior.persistence.SharedProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class ProjectSharingService {
    private static final int MAX_PROJECT_BYTES = 5_000_000;

    private final SharedProjectRepository repository;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public ProjectSharingService(SharedProjectRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SaveProjectResponse save(ProjectDto project) {
        String payload = toJson(project);
        if (payload.getBytes().length > MAX_PROJECT_BYTES) {
            throw new IllegalArgumentException("Project payload exceeds 5 MB");
        }
        Instant now = Instant.now();
        SharedProjectEntity entity = new SharedProjectEntity();
        entity.setShareKey(newShareKey());
        entity.setName(project.name());
        entity.setPayload(payload);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        repository.save(entity);
        return new SaveProjectResponse(entity.getId().toString(), entity.getShareKey());
    }

    @Transactional
    public ProjectDto load(String shareKey) {
        SharedProjectEntity entity = repository.findByShareKey(shareKey)
                .orElseThrow(() -> new ApiNotFoundException("Project not found"));
        entity.setLastAccessedAt(Instant.now());
        return fromJson(entity.getPayload());
    }

    private String newShareKey() {
        for (int i = 0; i < 20; i++) {
            String shareKey = new BigInteger(80, random).toString(32);
            if (!repository.existsByShareKey(shareKey)) {
                return shareKey;
            }
        }
        throw new IllegalStateException("Could not generate a unique share key");
    }

    private String toJson(ProjectDto project) {
        try {
            return objectMapper.writeValueAsString(project);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Project payload is not serializable", e);
        }
    }

    private ProjectDto fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, ProjectDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored project payload is invalid", e);
        }
    }
}
