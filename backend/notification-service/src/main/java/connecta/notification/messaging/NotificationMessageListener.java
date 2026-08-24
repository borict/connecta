package connecta.notification.messaging;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationMessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageListener.class);

    private final NotificationEventHandler handler;

    public NotificationMessageListener(NotificationEventHandler handler) {
        this.handler = handler;
    }

    public void onMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        if (message == null) {
            log.warn("Abandoning notification event with no message payload");
            abandon(context);
            return;
        }

        String messageId = message.getMessageId();
        try {
            String body = message.getBody() == null ? null : message.getBody().toString();
            EventHandleResult result = handler.handle(message.getSubject(), body, messageId);
            switch (result) {
                case CREATED, IGNORED -> complete(context);
                case INVALID -> deadLetter(context, messageId, message.getSubject());
            }
        } catch (RuntimeException ex) {
            log.warn(
                    "Abandoning notification event after processing failure. messageId={} cause={}",
                    messageId,
                    ex.toString()
            );
            abandon(context);
        }
    }

    public void onError(ServiceBusErrorContext context) {
        Throwable exception = context.getException();
        log.warn(
                "Service Bus processor error. source={} entity={} cause={}",
                context.getErrorSource(),
                context.getEntityPath(),
                exception != null ? exception.toString() : "unknown"
        );
    }

    private static void complete(ServiceBusReceivedMessageContext context) {
        try {
            context.complete();
        } catch (RuntimeException ex) {
            log.warn("Failed to complete notification event. cause={}", ex.toString());
        }
    }

    private static void abandon(ServiceBusReceivedMessageContext context) {
        try {
            context.abandon();
        } catch (RuntimeException ex) {
            log.warn("Failed to abandon notification event. cause={}", ex.toString());
        }
    }

    private static void deadLetter(
            ServiceBusReceivedMessageContext context,
            String messageId,
            String subject
    ) {
        log.warn("Dead-lettering invalid notification event. messageId={} subject={}", messageId, subject);
        try {
            context.deadLetter(new DeadLetterOptions()
                    .setDeadLetterReason("InvalidPayload")
                    .setDeadLetterErrorDescription("Notification event could not be processed"));
        } catch (RuntimeException ex) {
            log.warn("Failed to dead-letter notification event. messageId={} cause={}", messageId, ex.toString());
        }
    }
}
