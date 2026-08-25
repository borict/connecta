package connecta.message.controller;

import static org.mockito.Mockito.verify;

import connecta.message.dto.ChatSendRequest;
import connecta.message.service.MessageService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void send_delegatesToService() {
        UUID conversationId = UUID.randomUUID();

        chatController.send(new ChatSendRequest(conversationId, "hi"));

        verify(messageService).sendInConversation(conversationId, "hi");
    }

    @Test
    void send_nullRequest_delegatesNulls() {
        chatController.send(null);

        verify(messageService).sendInConversation(null, null);
    }
}
