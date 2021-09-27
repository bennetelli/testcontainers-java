package org.testcontainers.containers;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public class CosmosDBEmulatorContainerTest {

    @Rule
    // emulatorContainer {
    public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
        // Normally we would not use a 'latest' tag, but these images have a built in expiry
        DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
    );
    // }

    @Test
    public void testWithCosmosClient() throws Exception {
        // buildAndSaveNewKeyStore {
        Path keyStoreFile = Files.createTempFile("azure-cosmos-emulator", ".keystore");
        KeyStore keyStore = emulator.buildNewKeyStore();
        keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());
        // }
        // setSystemTrustStoreParameters {
        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", emulator.getEmulatorKey());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        // }
        // buildClient {
        CosmosAsyncClient client = new CosmosClientBuilder()
                .gatewayMode()
                .endpointDiscoveryEnabled(false)
                .endpoint(emulator.getEmulatorEndpoint())
                .key(emulator.getEmulatorKey())
                .buildAsyncClient();
        // }
        // testWithClientAgainstEmulatorContainer {
        CosmosDatabaseResponse databaseResponse =
                client.createDatabaseIfNotExists("Azure").block();
        Assertions.assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse =
                client.getDatabase("Azure").createContainerIfNotExists("ServiceContainer", "/name").block();
        Assertions.assertThat(containerResponse.getStatusCode()).isEqualTo(201);
        // }
    }
}
