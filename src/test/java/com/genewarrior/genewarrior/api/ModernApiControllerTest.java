package com.genewarrior.genewarrior.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.genewarrior.genewarrior.api.dto.ImportSequencesRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genewarrior-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "genewarrior.tools.tcoffee.path=",
        "genewarrior.tools.primer3.path="
})
@AutoConfigureMockMvc
class ModernApiControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void importsSequencesAsJson() throws Exception {
        mockMvc.perform(post("/api/sequences/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportSequencesRequest(">alpha\nACGT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequences[0].name", is("alpha")))
                .andExpect(jsonPath("$.sequences[0].kind", is("DNA")))
                .andExpect(jsonPath("$.sequences[0].sequence", is("ACGT")));
    }

    @Test
    void reportsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/sequences/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.text").exists());
    }

    @Test
    void returnsMissingJobAs404() throws Exception {
        mockMvc.perform(get("/api/jobs/not-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Job not found")));
    }

    @Test
    void createsConsensusAsAnOrdinaryPersistableSequence() throws Exception {
        var project = workspace();
        var alignment = project.get("alignments").get(0);
        var request = objectMapper.createObjectNode().put("name", alignment.get("name").asText()).put("kind", "DNA");
        request.set("sequences", alignment.get("sequences"));
        var response = mockMvc.perform(post("/api/alignments/consensus").contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pairwise [Consensus]")))
                .andExpect(jsonPath("$.sequence", is("ACGT")))
                .andExpect(jsonPath("$.kind", is("DNA")))
                .andExpect(jsonPath("$.marks").isEmpty())
                .andReturn().getResponse().getContentAsString();
        var sequence = objectMapper.readTree(response);
        project.withArray("sequences").add(sequence);
        project.withArray("entryOrder").insert(1, sequence.get("id").asText());
        String key = saveWorkspace(project);
        mockMvc.perform(get("/api/projects/" + key)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sequences[2].sequence", is("ACGT")))
                .andExpect(jsonPath("$.entryOrder[1]", is(sequence.get("id").asText())));
    }

    @Test
    void validatesConsensusRequestsAndLimits() throws Exception {
        for (String invalid : java.util.List.of("{}",
                """
                        {"name":"Alignment","kind":"DNA","sequences":[]}
                        """,
                """
                        {"name":"Alignment","kind":"DNA","sequences":[null]}
                        """,
                """
                        {"name":"Alignment","kind":"DNA","sequences":[{"name":"Row","value":"AX"}]}
                        """,
                """
                        {"name":"Alignment","kind":"DNA","sequences":[{"name":"Row","value":"--"}]}
                        """)) {
            mockMvc.perform(post("/api/alignments/consensus").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        }
        var request = objectMapper.createObjectNode().put("name", "Alignment").put("kind", "DNA");
        request.putArray("sequences").addObject().put("name", "Row").put("value", "A".repeat(100_001));
        mockMvc.perform(post("/api/alignments/consensus").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isPayloadTooLarge());
    }

    private ObjectNode workspace() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {"name":"Saved workspace","sequences":[
                  {"id":"dna","name":"Renamed DNA","kind":"DNA","sequence":"ACGT",
                   "marks":[{"start":0,"end":2,"color":"#f4c95d","type":"HIGHLIGHT"},
                            {"start":1,"end":4,"color":"#5a88b2","type":"ANNOTATION","annotation":"Gene","direction":"REVERSE"}]},
                  {"id":"protein","name":"Protein","kind":"PROTEIN","sequence":"MK","marks":[]}
                ],"alignments":[
                  {"id":"alignment","name":"Pairwise","kind":"DNA","score":10,
                   "sequences":[{"name":"First","value":"ACGT"},{"name":"Second","value":"A-GT"}],
                   "consensus":"* **","marks":[
                     {"start":0,"end":3,"color":"#4e9d86","type":"ANNOTATION","annotation":"Domain","direction":"FORWARD"}]}
                ],"entryOrder":["alignment","protein","dna"]}
                """);
    }

    private String saveWorkspace(ObjectNode project) throws Exception {
        String body = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("project", project))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("shareKey").asText();
    }

    @Test
    void savesAndLoadsCompleteWorkspaceAsIndependentSnapshots() throws Exception {
        ObjectNode project = objectMapper.valueToTree(objectMapper.treeToValue(workspace(),
                com.genewarrior.genewarrior.api.dto.ProjectDto.class));
        String firstKey = saveWorkspace(project);
        String body = mockMvc.perform(get("/api/projects/" + firstKey))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var loaded = objectMapper.readTree(body);
        assertEquals(project.get("sequences"), loaded.get("sequences"));
        assertEquals(project.get("alignments"), loaded.get("alignments"));
        assertEquals(project.get("entryOrder"), loaded.get("entryOrder"));
        assertNotNull(loaded.get("createdAt").textValue());
        assertNotNull(loaded.get("updatedAt").textValue());
        project.put("name", "Edited copy");
        String secondKey = saveWorkspace(project);
        assertNotEquals(firstKey, secondKey);
        mockMvc.perform(get("/api/projects/" + firstKey))
                .andExpect(jsonPath("$.name", is("Saved workspace")));
        mockMvc.perform(get("/api/projects/" + secondKey))
                .andExpect(jsonPath("$.name", is("Edited copy")));
    }

    @Test
    void savesEmptyWorkspaceAndReturnsMissingProjectAs404() throws Exception {
        ObjectNode empty = objectMapper.createObjectNode().put("name", "Empty");
        empty.putArray("sequences");
        empty.putArray("alignments");
        empty.putArray("entryOrder");
        String key = saveWorkspace(empty);
        mockMvc.perform(get("/api/projects/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryOrder").isEmpty())
                .andExpect(jsonPath("$.alignments").isEmpty());
        mockMvc.perform(get("/api/projects/not-here")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidWorkspaceStructureAndMarks() throws Exception {
        for (String invalid : java.util.List.of("duplicate", "order", "bounds", "color", "rows", "alignment-highlight")) {
            ObjectNode project = workspace();
            switch (invalid) {
                case "duplicate" -> ((ObjectNode) project.at("/alignments/0")).put("id", "dna");
                case "order" -> project.withArray("entryOrder").remove(0);
                case "bounds" -> ((ObjectNode) project.at("/alignments/0/marks/0")).put("end", 100);
                case "color" -> ((ObjectNode) project.at("/sequences/0/marks/0")).put("color", "url(evil)");
                case "rows" -> ((ObjectNode) project.at("/alignments/0/sequences/1")).put("value", "A");
                case "alignment-highlight" ->
                        ((ObjectNode) project.at("/alignments/0/marks/0")).put("type", "HIGHLIGHT");
            }
            mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("project", project))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsOversizedWorkspaceWith413() throws Exception {
        ObjectNode project = workspace();
        ((ObjectNode) project.at("/sequences/0")).put("sequence", "A".repeat(5_000_001));
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("project", project))))
                .andExpect(status().isPayloadTooLarge());
    }

    private ObjectNode logoRequest() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {"sourceAlignmentId":"alignment","name":"DNA logo","kind":"DNA","mode":"FREQUENCY",
                 "sequences":[{"name":"A","value":"AC-N"},{"name":"B","value":"AG-N"}]}
                """);
    }

    @Test
    void calculatesLogoSynchronouslyAndSharesItWithoutAnExternalTool() throws Exception {
        var response = mockMvc.perform(post("/api/logos").contentType(MediaType.APPLICATION_JSON).content(logoRequest().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method", is("uniform-uncorrected-v1")))
                .andExpect(jsonPath("$.columns[0].information", is(2.0)))
                .andExpect(jsonPath("$.columns[2].gaps", is(2)))
                .andReturn().getResponse().getContentAsString();
        var logo = objectMapper.readTree(response);
        ObjectNode project = workspace();
        project.putArray("logos").add(logo);
        project.withArray("entryOrder").add(logo.get("id").asText());
        String key = saveWorkspace(project);
        var loaded = mockMvc.perform(get("/api/projects/" + key)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(logo, objectMapper.readTree(loaded).at("/logos/0"));
        ((ObjectNode) project.at("/logos/0/columns/0/symbols/0")).put("frequency", .5);
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("project", project))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesLogoRequestsAndLimits() throws Exception {
        for (String field : java.util.List.of("kind", "mode", "sequences", "sourceAlignmentId", "name")) {
            var request = logoRequest();
            request.remove(field);
            mockMvc.perform(post("/api/logos").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                    .andExpect(status().isBadRequest());
        }
        var request = logoRequest();
        ((ObjectNode) request.at("/sequences/0")).put("value", "AC!");
        mockMvc.perform(post("/api/logos").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
        ((ObjectNode) request.at("/sequences/0")).put("value", "A".repeat(2001));
        mockMvc.perform(post("/api/logos").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isPayloadTooLarge());
    }
}
