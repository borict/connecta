package connecta.message.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMessageEventPublisher implements MessageEventPublisher, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AzureMessageEventPublisher.class);

    private final ServiceBusSenderClient sender;
    private final ObjectMapper objectMapper;

    public AzureMessageEventPublisher(ServiceBusSenderClient sender, ObjectMapper objectMapper) {
        this.sender = sender;
        this.objectMapper = objectMapper;
        log.info("Azure Service Bus publisher enabled for message events");
    }

    @Override
    public void publishMessageSent(MessageSentEvent event) {
        send(event.eventType(), event);
    }

    private void send(String eventType, Object event) {
        try {
            String body = objectMapper.writeValueAsString(event);
            sender.sendMessage(new ServiceBusMessage(body)
                    .setContentType("application/json")
                    .setSubject(eventType));
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("Failed to publish {}; primary flow continues. cause={}", eventType, ex.toString());
        }
    }

    @Override
    public void close() {
        sender.close();
    }
}
