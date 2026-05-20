package com.example.expensechallenge;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that stub the Treasury API via WireMock.
 *
 * <p>A single WireMock server is started for the whole test run. Because all
 * subclasses inherit the same {@link DynamicPropertySource} method (and
 * therefore produce the same context customizer key), Spring's context cache
 * keeps one {@code ApplicationContext} alive for every subclass instead of
 * spinning up a new one per test class.
 *
 * <p>Subclasses must call {@code wireMock.resetAll()} in a {@code @BeforeEach}
 * method so stubs don't leak between test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractWireMockIntegrationTest {

    protected static final WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @DynamicPropertySource
    static void overrideWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("treasury.base-url", wireMock::baseUrl);
    }
}
