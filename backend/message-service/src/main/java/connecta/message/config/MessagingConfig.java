package connecta.message.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.message.messaging.AzureMessageEventPublisher;
import connecta.message.messaging.MessageEventPublisher;
import connecta.message.messaging.NoOpMessageEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    @Bean(destroyMethod = "close")
    MessageEventPublisher messageEventPublisher(ServiceBusProperties properties, ObjectMapper objectMapper) {
        if (!properties.isConfigured()) {
            return new NoOpMessageEventPublisher();
        }
        try {
            ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                    .connectionString(properties.connectionString())
                    .sender()
                    .topicName(properties.topic())
                    .buildClient();
            return new AzureMessageEventPublisher(sender, objectMapper);
        } catch (RuntimeException ex) {
            log.warn("Could not create Azure Service Bus sender; events disabled. cause={}", ex.toString());
            return new NoOpMessageEventPublisher();
        }
    }
}
