package connecta.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connecta.azure.servicebus")
public record ServiceBusProperties(
        String connectionString,
        String topic,
        String subscription
) {
    public boolean isConfigured() {
        return connectionString != null && !connectionString.isBlank();
    }
}
