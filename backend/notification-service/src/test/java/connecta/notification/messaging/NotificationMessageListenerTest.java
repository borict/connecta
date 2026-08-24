package connecta.notification.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationMessageListenerTest {

    @Mock
    private NotificationEventHandler handler;

    @Mock
    private ServiceBusReceivedMessageContext context;

    @Mock
    private ServiceBusReceivedMessage message;

    @Mock
    private ServiceBusErrorContext errorContext;

    private NotificationMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationMessageListener(handler);
    }

    @Test
    void created_completes() {
        stubMessage();
        when(handler.handle("POST_LIKED", "{\"ok\":true}", "msg-1")).thenReturn(EventHandleResult.CREATED);

        listener.onMessage(context);

        verify(context).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter();
        verify(context, never()).deadLetter(any(DeadLetterOptions.class));
    }

    @Test
    void ignored_completes() {
        stubMessage();
        when(handler.handle("POST_LIKED", "{\"ok\":true}", "msg-1")).thenReturn(EventHandleResult.IGNORED);

        listener.onMessage(context);

        verify(context).complete();
        verify(context, never()).deadLetter(any(DeadLetterOptions.class));
    }

    @Test
    void invalid_deadLetters() {
        stubMessage();
        when(handler.handle("POST_LIKED", "{\"ok\":true}", "msg-1")).thenReturn(EventHandleResult.INVALID);

        listener.onMessage(context);

        verify(context).deadLetter(any(DeadLetterOptions.class));
        verify(context, never()).complete();
        verify(context, never()).abandon();
    }

    @Test
    void handlerFailure_abandons() {
        stubMessage();
        when(handler.handle("POST_LIKED", "{\"ok\":true}", "msg-1"))
                .thenThrow(new RuntimeException("database down"));

        listener.onMessage(context);

        verify(context).abandon();
        verify(context, never()).complete();
        verify(context, never()).deadLetter(any(DeadLetterOptions.class));
    }

    @Test
    void missingMessage_abandons() {
        when(context.getMessage()).thenReturn(null);

        listener.onMessage(context);

        verify(context).abandon();
        verify(handler, never()).handle(any(), any(), any());
    }

    @Test
    void settlementFailure_doesNotThrow() {
        stubMessage();
        when(handler.handle("POST_LIKED", "{\"ok\":true}", "msg-1")).thenReturn(EventHandleResult.CREATED);
        doThrow(new RuntimeException("complete failed")).when(context).complete();

        listener.onMessage(context);

        verify(context).complete();
    }

    @Test
    void onError_doesNotThrow() {
        when(errorContext.getException()).thenReturn(new RuntimeException("link lost"));
        when(errorContext.getErrorSource()).thenReturn(null);
        when(errorContext.getEntityPath()).thenReturn("connecta-events/subscriptions/notification-service");

        listener.onError(errorContext);
    }

    private void stubMessage() {
        when(context.getMessage()).thenReturn(message);
        when(message.getSubject()).thenReturn("POST_LIKED");
        when(message.getBody()).thenReturn(BinaryData.fromString("{\"ok\":true}"));
        when(message.getMessageId()).thenReturn("msg-1");
    }
}
