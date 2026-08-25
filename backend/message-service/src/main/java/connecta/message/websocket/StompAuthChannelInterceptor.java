package connecta.message.websocket;

import connecta.message.repository.ConversationParticipantRepository;
import connecta.message.security.AuthenticatedUser;
import connecta.message.security.JwtService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ConversationParticipantRepository participantRepository;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            ConversationParticipantRepository participantRepository
    ) {
        this.jwtService = jwtService;
        this.participantRepository = participantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            AuthenticatedUser user = authenticate(accessor);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities()
            );
            accessor.setUser(authentication);
        }

        if (accessor.getUser() instanceof Authentication authentication) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }
        if (StompCommand.SEND.equals(accessor.getCommand()) && currentUser(accessor) == null) {
            throw new MessageDeliveryException("Authentication required");
        }
        return message;
    }

    private AuthenticatedUser authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Authentication required");
        }
        String token = header.substring(7).trim();
        return jwtService.parseAuthenticatedUser(token)
                .orElseThrow(() -> new MessageDeliveryException("Invalid or expired token"));
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        AuthenticatedUser user = currentUser(accessor);
        if (user == null) {
            throw new MessageDeliveryException("Authentication required");
        }
        UUID conversationId = ChatDestinations.conversationIdFromTopic(accessor.getDestination())
                .orElseThrow(() -> new MessageDeliveryException("Subscription not allowed"));
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, user.id())) {
            throw new MessageDeliveryException("Conversation not found");
        }
    }

    private static AuthenticatedUser currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }
}
