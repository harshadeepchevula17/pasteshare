package com.pasteshare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pasteshare.repository.PasteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "pasteshare.public-base-url=https://paste.example",
                "pasteshare.allowed-origin=https://paste.example"
        })
class ActuatorHealthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @MockitoBean
    private PasteRepository pasteRepository;

    @Test
    void healthEndpointReturnsOnlyPublicStatus() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                JsonNode.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().path("status").asText());
        assertNull(response.getBody().get("components"));
    }

    @Test
    void readinessEndpointReturnsUp() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health/readiness",
                JsonNode.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().path("status").asText());
    }
}
