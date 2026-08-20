package connecta.post.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.post.messaging.AzurePostEventPublisher;
import connecta.post.messaging.NoOpPostEventPublisher;
import connecta.post.messaging.PostEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    @Bean(destroyMethod = "close")
    PostEventPublisher postEventPublisher(ServiceBusProperties properties, ObjectMapper objectMapper) {
        if (!properties.isConfigured()) {
            return new NoOpPostEventPublisher();
        }
        try {
            ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                    .connectionString(properties.connectionString())
                    .sender()
                    .topicName(properties.topic())
                    .buildClient();
            return new AzurePostEventPublisher(sender, objectMapper);
        } catch (RuntimeException ex) {
            log.warn("Could not create Azure Service Bus sender; events disabled. cause={}", ex.toString());
            return new NoOpPostEventPublisher();
        }
    }
}
