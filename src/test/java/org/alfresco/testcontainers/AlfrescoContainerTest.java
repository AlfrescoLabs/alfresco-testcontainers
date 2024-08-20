package org.alfresco.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for AlfrescoContainer class.
 * These tests ensure that the AlfrescoContainer is correctly created, started, stopped,
 * and that it includes the expected configurations.
 */
class AlfrescoContainerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoContainerTest.class);

    /**
     * Retrieves the Alfresco version by making a REST API call to the Alfresco repository.
     *
     * @param alfrescoContainer the AlfrescoContainer instance
     * @return the Alfresco version as a String
     * @throws Exception if there is an error making the HTTP request
     */
    private String getAlfrescoVersion(AlfrescoContainer<?> alfrescoContainer) throws Exception {
        String serverUrl = "http://" + alfrescoContainer.getHost() + ":" + alfrescoContainer.getMappedPort(8080) + "/alfresco/service/api/server";
        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            Scanner scanner = new Scanner(url.openStream());
            StringBuilder inline = new StringBuilder();
            while (scanner.hasNext()) {
                inline.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject jsonResponse = new JSONObject(inline.toString());
            JSONObject data = jsonResponse.getJSONObject("data");
            return data.getString("version").split(" ")[0];
        }
    }

    /**
     * Test the creation of a default AlfrescoContainer for version 7.x.
     * Ensures that the container starts and stops without throwing any exceptions.
     */
    @DisplayName("Create default AlfrescoContainer for version 7.x")
    @Test
    void testAlfresco7() {
        LOGGER.info("Starting testAlfresco7");

        AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("7.4.1");
        assertDoesNotThrow(() -> {
            LOGGER.info("Starting AlfrescoContainer for version 7.x");
            alfrescoContainer.start();
        });

        LOGGER.info("Verifying Alfresco version 7.4.1");
        String version = assertDoesNotThrow(() -> getAlfrescoVersion(alfrescoContainer), "Failed to retrieve Alfresco version");
        Assertions.assertEquals("7.4.1", version, "Alfresco version should be 7.4.1");

        LOGGER.info("Verifying port mapping for AlfrescoContainer version 7.x");
        Assertions.assertNotNull(alfrescoContainer.getMappedPort(8080), "Port 8080 should be mapped");

        assertDoesNotThrow(() -> {
            LOGGER.info("Stopping AlfrescoContainer for version 7.x");
            alfrescoContainer.stop();
        });

        LOGGER.info("Completed testAlfresco7 successfully");
    }

    /**
     * Test the creation of a default AlfrescoContainer for version 23.x.
     * Verifies that the container starts, maps the expected port, and stops without throwing any exceptions.
     */
    @DisplayName("Create default AlfrescoContainer for version 23.x")
    @Test
    void testAlfresco23() {
        LOGGER.info("Starting testAlfresco23");

        AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1");
        assertDoesNotThrow(() -> {
            LOGGER.info("Starting AlfrescoContainer for version 23.x");
            alfrescoContainer.start();
        });

        LOGGER.info("Verifying Alfresco version 23.2.1");
        String version = assertDoesNotThrow(() -> getAlfrescoVersion(alfrescoContainer), "Failed to retrieve Alfresco version");
        // Note that Alfresco version 23.2.1 has a bug where it incorrectly returns version 23.2.0
        Assertions.assertEquals("23.2.0", version, "Alfresco version should be 23.2.1");

        LOGGER.info("Verifying port mapping for AlfrescoContainer version 23.x");
        Assertions.assertNotNull(alfrescoContainer.getMappedPort(8080), "Port 8080 should be mapped");

        assertDoesNotThrow(() -> {
            LOGGER.info("Stopping AlfrescoContainer for version 23.x");
            alfrescoContainer.stop();
        });

        LOGGER.info("Completed testAlfresco23 successfully");
    }

    /**
     * Test the creation of a default AlfrescoContainer for version 23.x with ActiveMQ enabled.
     * Ensures that the container starts, ActiveMQ is enabled, the expected port is mapped, and the container stops without throwing any exceptions.
     */
    @DisplayName("Create default AlfrescoContainer with ActiveMQ")
    @Test
    void testAlfrescoWithActiveMQ() {
        LOGGER.info("Starting testAlfrescoWithActiveMQ");

        AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1").withMessagingEnabled();
        assertDoesNotThrow(() -> {
            LOGGER.info("Starting AlfrescoContainer with ActiveMQ enabled");
            alfrescoContainer.start();
        });

        LOGGER.info("Verifying ActiveMQ configuration");
        Assertions.assertNotNull(alfrescoContainer.getActivemqContainer(), "ActiveMQ container should not be null");
        Assertions.assertNotNull(alfrescoContainer.getActivemqContainer().getMappedPort(61616), "ActiveMQ port 61616 should be mapped");

        assertDoesNotThrow(() -> {
            LOGGER.info("Stopping AlfrescoContainer with ActiveMQ enabled");
            alfrescoContainer.stop();
        });

        LOGGER.info("Completed testAlfrescoWithActiveMQ successfully");
    }

    /**
     * Test the AlfrescoContainer to ensure it includes the expected custom configurations.
     * Verifies network setup and environment variable configurations.
     */
    @DisplayName("Verify AlfrescoContainer includes expected configuration")
    @Test
    void testCustomConfiguration() {
        LOGGER.info("Starting testCustomConfiguration");

        AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1");
        assertDoesNotThrow(() -> {
            LOGGER.info("Starting AlfrescoContainer for custom configuration verification");
            alfrescoContainer.start();
        });

        LOGGER.info("Verifying network configuration");
        Assertions.assertNotNull(alfrescoContainer.getNetwork(), "Network should be correctly set");

        LOGGER.info("Verifying JAVA_TOOL_OPTIONS environment variable");
        Assertions.assertEquals(AlfrescoContainer.DEFAULT_JAVA_TOOL_OPTIONS, alfrescoContainer.getEnvMap().get("JAVA_TOOL_OPTIONS"), "JAVA_TOOL_OPTIONS should match expected value");

        LOGGER.info("Verifying JAVA_OPTS environment variable");
        Assertions.assertEquals(AlfrescoContainer.DEFAULT_JAVA_OPTS, alfrescoContainer.getEnvMap().get("JAVA_OPTS"), "JAVA_OPTS should match expected value");

        assertDoesNotThrow(() -> {
            LOGGER.info("Stopping AlfrescoContainer after custom configuration verification");
            alfrescoContainer.stop();
        });

        LOGGER.info("Completed testCustomConfiguration successfully");
    }
}
