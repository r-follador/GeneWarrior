package com.genewarrior.genewarrior.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.ImportSequencesRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
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
        "genewarrior.tools.muscle.path=",
        "genewarrior.tools.primer3.path=",
        "genewarrior.tools.weblogo.path="
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
}
