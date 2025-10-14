package com.example.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // Use application-test.properties
class TaskManagerApplicationTests {

    @Test
    void contextLoads() {
        // This will load the Spring context with H2 in-memory DB
    }

}
