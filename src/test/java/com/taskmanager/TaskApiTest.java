package com.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests covering the user journey, validation and operational endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class TaskApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createThenFetchTask() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Write report\",\"description\":\"viva\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Write report")));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        mvc.perform(get("/api/tasks/{id}", "1' OR '1'='1")).andExpect(status().isBadRequest());
    }

    @Test
    void exposesProbesAndMetrics() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests")));
    }
}
