package org.alfresco.testcontainers;

import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * The Alfresco Community Docker container which exposes by default port 8080.
 */
public class AlfrescoContainer<SELF extends AlfrescoContainer<SELF>> extends GenericContainer<SELF> {

    // Alfresco Community Docker base image
    private static final DockerImageName DEFAULT_ALFRESCO_IMAGE_NAME = DockerImageName.parse("alfresco/alfresco-content-repository-community");

    // PostgreSQL Docker base image
    private static final DockerImageName DEFAULT_POSTGRESQL_IMAGE_NAME = DockerImageName.parse("postgres:15.6");

    // ActiveMQ Docker base image
    private static final DockerImageName DEFAULT_ACTIVEMQ_IMAGE_NAME = DockerImageName.parse("apache/activemq-classic:5.18.3");

    // Default Java tool options for Alfresco
    protected static final String DEFAULT_JAVA_TOOL_OPTIONS =
            """
            -Dencryption.keystore.type=JCEKS
            -Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding
            -Dencryption.keyAlgorithm=DESede
            -Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore
            -Dmetadata-keystore.password=mp6yc0UD9e
            -Dmetadata-keystore.aliases=metadata
            -Dmetadata-keystore.metadata.password=oKIWzVdEdA
            -Dmetadata-keystore.metadata.algorithm=DESede
            """.replace("\n", " ").trim();

    // Default Java options for Alfresco
    protected static final String DEFAULT_JAVA_OPTS =
            """
            -Ddb.driver=org.postgresql.Driver
            -Ddb.username=alfresco
            -Ddb.password=alfresco
            -Ddb.url=jdbc:postgresql://postgres:5432/alfresco
            -Dindex.subsystem.name=noindex
            -Dlocal.transform.service.enabled=false
            -Drepo.event2.enabled=false
            -Dmessaging.subsystem.autoStart=false
            -Dcsrf.filter.enabled=false
            """.replace("\n", " ").trim();

    // Alfresco Community requires at least one PostgreSQL container, so a shared network is necessary.
    private final Network network;

    // PostgreSQL Container to be used as DB by Alfresco Community
    private PostgreSQLContainer<?> postgresContainer;

    // ActiveMQ Container to be used as Messaging Engine by Alfresco Community
    private ActiveMQContainer activeMQContainer;

    /**
     * Create an Alfresco Community Container by passing the full docker image name.
     *
     * @param version Alfresco docker image version as a {@link String}, like:
     *     7.4.1
     *     23.2.1
     */
    public AlfrescoContainer(String version) {
        this(DockerImageName.parse(DEFAULT_ALFRESCO_IMAGE_NAME.getUnversionedPart() + ":" + version));
    }

    /**
     * Create an Alfresco Community Container by passing the full docker image name.
     *
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like:
     *      DockerImageName.parse("alfresco/alfresco-content-repository-community:7.4.1")
     *      DockerImageName.parse("alfresco/alfresco-content-repository-community:23.2.1")
     */
    public AlfrescoContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_ALFRESCO_IMAGE_NAME);
        this.network = Network.newNetwork();
    }

    /**
     * Generates custom Java options for ActiveMQ, updating the messaging broker URL.
     *
     * @return A string containing the updated Java options.
     */
    private String getActivemqJavaOpts() {
        String brokerAlias = activeMQContainer.getNetworkAliases().get(0);
        String activemqBrokerUrl = String.format(
                "-Dmessaging.broker.url=\"failover:(nio://%s:61616)?timeout=3000&jms.useCompression=true\"", brokerAlias
        );
        return DEFAULT_JAVA_OPTS.replace(
                "-Drepo.event2.enabled=false -Dmessaging.subsystem.autoStart=false ", ""
        ) + " " + activemqBrokerUrl;
    }


    /**
     * Configures the Alfresco container with the necessary environment variables, network, and wait strategy.
     * This method is automatically called during the container initialization.
     */
    @Override
    protected void configure() {
        super.configure();

        if (postgresContainer == null) {
            createDefaultPostgreSQLContainer();
        }

        String javaOpts = activeMQContainer != null ? getActivemqJavaOpts() : DEFAULT_JAVA_OPTS;

        withEnv("JAVA_TOOL_OPTIONS", DEFAULT_JAVA_TOOL_OPTIONS);
        withEnv("JAVA_OPTS", javaOpts);
        withNetwork(network);
        withNetworkAliases("alfresco");
        withExposedPorts(8080);

        waitingFor(new HttpWaitStrategy()
                .forPort(8080)
                .forPath("/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-")
                .forStatusCodeMatching(response -> response == 200)
                .withStartupTimeout(Duration.ofMinutes(3)));
    }

    /**
     * Starts the Alfresco container and its dependencies, such as PostgreSQL and ActiveMQ.
     * <p>
     * The PostgreSQL container is started first, followed by the ActiveMQ container if present.
     * Finally, the Alfresco container is started. This ensures that all required services are up
     * and running before Alfresco attempts to start.
     * </p>
     */
    @Override
    public void start() {

        // Apply AlfrescoContainer configuration
        this.configure();

        // Start the PostgreSQL container first as it's essential for Alfresco to function
        postgresContainer.start();

        // Start the ActiveMQ container if it is present, enabling messaging services
        if (activeMQContainer != null) {
            activeMQContainer.start();
        }

        // Start the Alfresco container after the dependencies are up and running
        super.start();
    }

    /**
     * Stops the Alfresco container and its dependencies, such as PostgreSQL and ActiveMQ.
     * <p>
     * The Alfresco container is stopped first to ensure all processes are gracefully terminated.
     * The ActiveMQ container is stopped next if it was started, followed by the PostgreSQL container
     * to ensure data integrity and avoid potential data loss.
     * </p>
     */
    @Override
    public void stop() {
        // Stop the PostgreSQL container last as it is crucial for maintaining data integrity
        try {
            super.stop(); // Stop the Alfresco container first to ensure all processes are gracefully terminated
        } finally {
            // Ensure that the ActiveMQ container is stopped if it was started
            if (activeMQContainer != null) {
                activeMQContainer.stop();
            }

            // Stop the PostgreSQL container after other services to avoid data loss
            postgresContainer.stop();
        }
    }

    /**
     * Enable ActiveMQ Messaging service
     * @return this container instance
     */
    public SELF withMessagingEnabled() {
        createDefaultActivemqContainer();
        return self();
    }

    /**
     * Gets the network shared by the containers.
     *
     * @return The shared network.
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Sets the PostgreSQL container to be used by the Alfresco container.
     *
     * @param postgreSQLContainer The PostgreSQL container.
     */
    public void setPostgreSQLContainer(PostgreSQLContainer<?> postgreSQLContainer) {
        this.postgresContainer = postgreSQLContainer;
    }

    /**
     * Gets the PostgreSQL container being used.
     *
     * @return The PostgreSQL container.
     */
    public PostgreSQLContainer<?> getPostgreSQLContainer() {
        return this.postgresContainer;
    }

    /**
     * Creates and configures a default PostgreSQL container for Alfresco.
     */
    public void createDefaultPostgreSQLContainer() {
        this.postgresContainer = new PostgreSQLContainer<>(DEFAULT_POSTGRESQL_IMAGE_NAME)
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * Sets the ActiveMQ container to be used by the Alfresco container.
     *
     * @param activemqContainer The ActiveMQ container.
     */
    public void setActivemqContainer(ActiveMQContainer activemqContainer) {
        this.activeMQContainer = activemqContainer;
    }

    /**
     * Gets the ActiveMQ container being used.
     *
     * @return The ActiveMQ container.
     */
    public ActiveMQContainer getActivemqContainer() {
        return activeMQContainer;
    }

    /**
     * Creates and configures a default ActiveMQ container for Alfresco.
     */
    public void createDefaultActivemqContainer() {
        activeMQContainer = new ActiveMQContainer(DEFAULT_ACTIVEMQ_IMAGE_NAME)
                .withNetwork(network)
                .withNetworkAliases("activemq")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedPorts(61616, 8161);
    }

}
