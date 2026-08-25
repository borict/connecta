package connecta.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.message.domain.Conversation;
import connecta.message.domain.ConversationRead;
import connecta.message.domain.DirectPair;
import connecta.message.domain.Message;
import connecta.message.domain.Role;
import connecta.message.dto.CreateMessageRequest;
import connecta.message.dto.MessageResponse;
import connecta.message.dto.PageResponse;
import connecta.message.repository.ConversationReadRepository;
import connecta.message.repository.ConversationRepository;
import connecta.message.repository.DirectPairRepository;
import connecta.message.repository.MessageRepository;
import connecta.message.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private DirectPairRepository directPairRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationReadRepository readRepository;

    private MessageService messageService;
    private UUID currentUserId;
    private UUID otherUserId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                directPairRepository,
                conversationRepository,
                messageRepository,
                readRepository
        );
        currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        conversationId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(currentUserId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void send_persistsTrimmedContentAndTouchesConversation() {
        stubExistingConversation();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(new Conversation(conversationId)));

        MessageResponse response = messageService.send(otherUserId, new CreateMessageRequest("  Hey!  "));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();
        assertThat(saved.getConversationId()).isEqualTo(conversationId);
        assertThat(saved.getSenderId()).isEqualTo(currentUserId);
        assertThat(saved.getContent()).isEqualTo("Hey!");
        assertThat(response.content()).isEqualTo("Hey!");
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void send_self_returnsBadRequest() {
        assertThatThrownBy(() -> messageService.send(currentUserId, new CreateMessageRequest("Hey")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Cannot start a conversation with yourself");
                });
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_missingConversation_returnsNotFound() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.send(otherUserId, new CreateMessageRequest("Hey")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Conversation not found");
                });
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_blankContent_returnsBadRequest() {
        stubExistingConversation();

        assertThatThrownBy(() -> messageService.send(otherUserId, new CreateMessageRequest("   ")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Content must not be blank");
                });
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_tooLongContent_returnsBadRequest() {
        stubExistingConversation();
        String tooLong = "a".repeat(MessageService.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> messageService.send(otherUserId, new CreateMessageRequest(tooLong)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Content must be at most 2000 characters");
                });
        verify(messageRepository, never()).save(any());
    }

    @Test
    void list_returnsNewestFirstPage() {
        stubExistingConversation();
        Message newest = new Message(UUID.randomUUID(), conversationId, otherUserId, "latest");
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(
                eq(conversationId),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new PageImpl<>(List.of(newest), PageRequest.of(0, 20), 1));

        PageResponse<MessageResponse> page = messageService.list(otherUserId, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().content()).isEqualTo("latest");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void list_clampsPageAndSize() {
        stubExistingConversation();
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversationId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        messageService.list(otherUserId, -2, 999);

        verify(messageRepository).findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, 50)
        );
    }

    @Test
    void list_missingConversation_returnsNotFound() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.list(otherUserId, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void markRead_createsCursorWhenMissing() {
        stubExistingConversation();
        when(readRepository.findByConversationIdAndUserId(conversationId, currentUserId))
                .thenReturn(Optional.empty());

        messageService.markRead(otherUserId);

        ArgumentCaptor<ConversationRead> captor = ArgumentCaptor.forClass(ConversationRead.class);
        verify(readRepository).save(captor.capture());
        assertThat(captor.getValue().getConversationId()).isEqualTo(conversationId);
        assertThat(captor.getValue().getUserId()).isEqualTo(currentUserId);
        assertThat(captor.getValue().getLastReadAt()).isNotNull();
    }

    @Test
    void markRead_updatesExistingCursor() {
        stubExistingConversation();
        ConversationRead existing = new ConversationRead(
                conversationId,
                currentUserId,
                Instant.parse("2026-08-25T10:00:00Z")
        );
        when(readRepository.findByConversationIdAndUserId(conversationId, currentUserId))
                .thenReturn(Optional.of(existing));

        messageService.markRead(otherUserId);

        assertThat(existing.getLastReadAt()).isAfter(Instant.parse("2026-08-25T10:00:00Z"));
        verify(readRepository).save(existing);
    }

    @Test
    void markRead_missingConversation_returnsNotFound() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.markRead(otherUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(readRepository, never()).save(any());
    }

    private void stubExistingConversation() {
        when(directPairRepository.findById(any()))
                .thenReturn(Optional.of(new DirectPair(currentUserId, otherUserId, conversationId)));
    }
}
