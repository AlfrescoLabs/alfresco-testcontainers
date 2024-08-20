alfresco-testcontainers
======================

A [Testcontainers](https://www.testcontainers.org/) integration for [Alfresco](https://github.com/alfresco).

## Overview

The `alfresco-testcontainers` provides integration of [Alfresco](https://github.com/alfresco) Docker containers with [Testcontainers](https://www.testcontainers.org/).

## Usage

```xml
<dependency>
    <groupId>org.opensearch</groupId>
    <artifactId>alfresco-testcontainers</artifactId>
    <version>0.8.0</version>
    <scope>test</scope>
</dependency>
```

The `opensearch-testcontainers` can be used with [JUnit 4](https://junit.org/junit4/) or [JUnit 5](https://junit.org/junit5/).

## JUnit 4 Integration

Follow the [JUnit 4 Quickstart](https://www.testcontainers.org/quickstart/junit_4_quickstart/) to customize the container to your needs.

```java
@Rule
public AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1");

```

## JUnit 5 Integration

Follow the [JUnit 5 Quickstart](https://www.testcontainers.org/quickstart/junit_5_quickstart/) to activate `@Testcontainers` extension and to customize the container to your needs.

```java
@Container
public AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1");

```

## Usage Examples

By default, Alfresco Docker containers run with ActiveMQ disabled, but it can be enabled using method `withMessagingEnabled`.

```java
AlfrescoContainer<?> alfrescoContainer = new AlfrescoContainer<>("23.2.1").withMessagingEnabled();
```

The `AlfrescoContainer` provides access to internal containers used by Alfresco, like ActiveMQ and PostgreSQL.

```java
alfrescoContainer.getNetwork()
alfrescoContainer.getPostgreSQLContainer()
alfrescoContainer.getActivemqContainer() 
```

### Unavailable configurations

As of now, the AlfrescoContainer does not natively support the following containers and features:

* Search Service
* Transform Service
* UI components (such as Share and ACA)