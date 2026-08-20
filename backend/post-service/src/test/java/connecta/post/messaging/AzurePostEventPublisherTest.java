package connecta.post.messaging;

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
class AzurePostEventPublisherTest {

    @Mock
    private ServiceBusSenderClient sender;

    private AzurePostEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AzurePostEventPublisher(sender, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void publishPostLiked_sendsJsonMessage() {
        PostLikedEvent event = PostLikedEvent.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        publisher.publishPostLiked(event);

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo(PostLikedEvent.TYPE);
        assertThat(captor.getValue().getBody().toString()).contains("POST_LIKED");
    }

    @Test
    void publishPostLiked_senderFailure_doesNotThrow() {
        doThrow(new RuntimeException("timeout")).when(sender).sendMessage(any());

        assertThatCode(() -> publisher.publishPostLiked(
                PostLikedEvent.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )).doesNotThrowAnyException();
    }
}
