package connecta.social.messaging;

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
class AzureFollowEventPublisherTest {

    @Mock
    private ServiceBusSenderClient sender;

    private AzureFollowEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AzureFollowEventPublisher(sender, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void publishUserFollowed_sendsJsonMessage() {
        UserFollowedEvent event = UserFollowedEvent.of(UUID.randomUUID(), UUID.randomUUID());

        publisher.publishUserFollowed(event);

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo(UserFollowedEvent.TYPE);
        assertThat(captor.getValue().getBody().toString()).contains("USER_FOLLOWED");
    }

    @Test
    void publishUserFollowed_senderFailure_doesNotThrow() {
        doThrow(new RuntimeException("timeout")).when(sender).sendMessage(any());

        assertThatCode(() -> publisher.publishUserFollowed(
                UserFollowedEvent.of(UUID.randomUUID(), UUID.randomUUID())
        )).doesNotThrowAnyException();
    }
}
