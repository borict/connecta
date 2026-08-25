package connecta.message.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.message.domain.Role;
import connecta.message.repository.ConversationParticipantRepository;
import connecta.message.security.AuthenticatedUser;
import connecta.message.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private MessageChannel channel;

    private StompAuthChannelInterceptor interceptor;
    private AuthenticatedUser user;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtService, participantRepository);
        user = new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "tamara",
                Role.USER
        );
        conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connect_validBearer_setsUser() {
        when(jwtService.parseAuthenticatedUser("good-token")).thenReturn(Optional.of(user));
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, accessor ->
                accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token"));

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor after = StompHeaderAccessor.wrap(result);
        assertThat(after.getUser()).isInstanceOf(Authentication.class);
        assertThat(((Authentication) after.getUser()).getPrincipal()).isEqualTo(user);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
    }

    @Test
    void connect_missingBearer_rejects() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, accessor -> {
        });

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Authentication required");
    }

    @Test
    void connect_invalidToken_rejects() {
        when(jwtService.parseAuthenticatedUser("bad")).thenReturn(Optional.empty());
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, accessor ->
                accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer bad"));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    void subscribe_participant_allows() {
        when(participantRepository.existsByConversationIdAndUserId(conversationId, user.id())).thenReturn(true);
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setDestination(ChatDestinations.conversationTopic(conversationId));
            accessor.setUser(authentication());
        });

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verify(participantRepository).existsByConversationIdAndUserId(conversationId, user.id());
    }

    @Test
    void subscribe_notParticipant_rejects() {
        when(participantRepository.existsByConversationIdAndUserId(conversationId, user.id())).thenReturn(false);
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setDestination(ChatDestinations.conversationTopic(conversationId));
            accessor.setUser(authentication());
        });

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    void subscribe_foreignDestination_rejects() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setDestination("/user/queue/notifications");
            accessor.setUser(authentication());
        });

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Subscription not allowed");
    }

    @Test
    void send_withoutUser_rejects() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, accessor ->
                accessor.setDestination("/app/chat.send"));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Authentication required");
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private static Message<byte[]> stompMessage(StompCommand command, Consumer<StompHeaderAccessor> customizer) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        customizer.accept(accessor);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
