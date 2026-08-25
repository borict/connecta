package connecta.message.controller;

import connecta.message.dto.ChatSendRequest;
import connecta.message.service.MessageService;
import connecta.message.websocket.ChatDestinations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final MessageService messageService;

    public ChatController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping(ChatDestinations.APP_SEND)
    public void send(ChatSendRequest request) {
        if (request == null) {
            messageService.sendInConversation(null, null);
            return;
        }
        messageService.sendInConversation(request.conversationId(), request.content());
    }
}
