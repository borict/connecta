package connecta.message.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureMessageEventPublisherTest {

    @Mock
    private ServiceBusSenderClient sender;

    private AzureMessageEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AzureMessageEventPublisher(sender, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void publishMessageSent_sendsJsonMessage() {
        MessageSentEvent event = MessageSentEvent.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        publisher.publishMessageSent(event);

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo(MessageSentEvent.TYPE);
        assertThat(captor.getValue().getBody().toString()).contains("MESSAGE_SENT");
    }

    @Test
    void publishMessageSent_senderFailure_doesNotThrow() {
        doThrow(new RuntimeException("timeout")).when(sender).sendMessage(any());

        assertThatCode(() -> publisher.publishMessageSent(
                MessageSentEvent.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )).doesNotThrowAnyException();
    }
}
