package com.pasteshare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.pasteshare.dto.CreatePasteResponse;
import com.pasteshare.entity.Paste;
import com.pasteshare.exception.PasteGoneException;
import com.pasteshare.repository.PasteRepository;
import com.pasteshare.service.PasteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PasteConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("pasteshare.public-base-url", () -> "http://localhost:5173");
        registry.add("pasteshare.allowed-origin", () -> "http://localhost:5173");
    }

    @Autowired
    private PasteService pasteService;

    @Autowired
    private PasteRepository pasteRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndRetrieveApiReturnsDocumentedContract() throws Exception {
        String response = mockMvc.perform(post("/api/v1/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"api text","maxViews":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.shareUrl").value(containsString("/p/")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/pastes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.content").value("api text"))
                .andExpect(jsonPath("$.viewsRemaining").value(1));
    }

    @Test
    void readinessHealthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void invalidCreateRequestReturnsConsistentBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   ","maxViews":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("content")));
    }

    @Test
    void pastExpirationIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"expired immediately","expiresAt":"2000-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Expiration")));
    }

    @Test
    void missingPasteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/pastes/{id}", "missing-paste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void exhaustedViewLimitReturnsGone() throws Exception {
        String response = mockMvc.perform(post("/api/v1/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"one view only","maxViews":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/pastes/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/pastes/{id}", id))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    void concurrentRetrievalsNeverExceedViewLimit() throws Exception {
        int maxViews = 7;
        int requestCount = 32;
        CreatePasteResponse created = pasteService.createPaste("concurrent text", null, maxViews);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successfulViews = new AtomicInteger();
        List<Future<?>> requests = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start in time.");
                    }
                    try {
                        pasteService.getPaste(created.id());
                        successfulViews.incrementAndGet();
                    } catch (PasteGoneException expected) {
                        // Requests after the limit is reached are expected to fail.
                    }
                    return null;
                }));
            }

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent requests did not become ready.");
            }
            start.countDown();
            for (Future<?> request : requests) {
                request.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Paste persisted = pasteRepository.findById(created.id()).orElseThrow();
        assertEquals(maxViews, successfulViews.get());
        assertEquals(maxViews, persisted.getViewCount());
        assertThrows(PasteGoneException.class, () -> pasteService.getPaste(created.id()));
    }
}
