package com.example.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc // Configures MockMvc for simulating API calls
@ActiveProfiles("test") 
class TaskControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc; // Used to simulate HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // Utility for converting objects to JSON

    // Helper class for creating a Task payload (Includes getters/setters to eliminate warnings)
    @SuppressWarnings("unused") // Suppress warnings for methods used externally by Jackson
    private static class TaskPayload {
        private String title;
        private Boolean completed;
        
        public TaskPayload() {} 

        public TaskPayload(String title, Boolean completed) {
            this.title = title;
            this.completed = completed;
        }

        public String getTitle() {
            return title;
        }

        public Boolean getCompleted() {
            return completed;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }

        public void setCompleted(Boolean completed) {
            this.completed = completed;
        }
    }

    @Test
    void shouldCreateAndListTasks() throws Exception {
        // 1. Arrange: Create a new task object
        TaskPayload newTask = new TaskPayload("Verify CORS Fix", false);
        String taskJson = objectMapper.writeValueAsString(newTask);

        // 2. Act & Assert: Send POST request to create the task
        mockMvc.perform(post("/api/tasks") // Assuming your endpoint is /api/tasks
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Verify CORS Fix")))
                .andExpect(jsonPath("$.completed", is(false)));

        // 3. Act & Assert: Send GET request to retrieve all tasks (verifies Read logic)
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1)))); // Should have at least the one we created
    }
}