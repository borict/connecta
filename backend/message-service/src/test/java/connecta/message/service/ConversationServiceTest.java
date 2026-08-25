package connecta.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.message.client.UserSummaryDto;
import connecta.message.domain.Conversation;
import connecta.message.domain.ConversationParticipant;
import connecta.message.domain.ConversationRead;
import connecta.message.domain.DirectPair;
import connecta.message.domain.Message;
import connecta.message.domain.Role;
import connecta.message.dto.ConversationResponse;
import connecta.message.dto.PageResponse;
import connecta.message.repository.ConversationParticipantRepository;
import connecta.message.repository.ConversationReadRepository;
import connecta.message.repository.ConversationRepository;
import connecta.message.repository.DirectPairRepository;
import connecta.message.repository.MessageRepository;
import connecta.message.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private DirectPairRepository directPairRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationReadRepository readRepository;

    @Mock
    private UserLookupService userLookup;

    private ConversationService conversationService;
    private UUID currentUserId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                participantRepository,
                directPairRepository,
                messageRepository,
                readRepository,
                userLookup
        );
        currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        authenticate(currentUserId);
        stubResponseLookups();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrCreate_self_returnsBadRequest() {
        assertThatThrownBy(() -> conversationService.getOrCreate(currentUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Cannot start a conversation with yourself");
                });
        verify(directPairRepository, never()).save(any());
        verify(userLookup, never()).requireActiveUser(any());
    }

    @Test
    void getOrCreate_createsConversation() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());
        when(userLookup.requireActiveUser(otherUserId)).thenReturn(user(otherUserId));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any(ConversationParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(directPairRepository.save(any(DirectPair.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationService.ConversationResult result = conversationService.getOrCreate(otherUserId);

        assertThat(result.created()).isTrue();
        assertThat(result.conversation().otherUserId()).isEqualTo(otherUserId);
        ArgumentCaptor<DirectPair> pairCaptor = ArgumentCaptor.forClass(DirectPair.class);
        verify(directPairRepository).save(pairCaptor.capture());
        assertThat(pairCaptor.getValue().getUserAId()).isEqualTo(currentUserId);
        assertThat(pairCaptor.getValue().getUserBId()).isEqualTo(otherUserId);
        verify(userLookup).requireActiveUser(otherUserId);
    }

    @Test
    void getOrCreate_existing_isIdempotent() {
        UUID conversationId = UUID.randomUUID();
        DirectPair existing = new DirectPair(currentUserId, otherUserId, conversationId);
        when(directPairRepository.findById(any())).thenReturn(Optional.of(existing));

        ConversationService.ConversationResult result = conversationService.getOrCreate(otherUserId);

        assertThat(result.created()).isFalse();
        assertThat(result.conversation().conversationId()).isEqualTo(conversationId);
        verify(userLookup, never()).requireActiveUser(any());
        verify(directPairRepository, never()).save(any());
    }

    @Test
    void getOrCreate_userNotFound_returnsNotFound() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());
        when(userLookup.requireActiveUser(otherUserId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        assertThatThrownBy(() -> conversationService.getOrCreate(otherUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(directPairRepository, never()).save(any());
    }

    @Test
    void getOrCreate_userServiceDown_returnsServiceUnavailable() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());
        when(userLookup.requireActiveUser(otherUserId))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User Service unavailable"));

        assertThatThrownBy(() -> conversationService.getOrCreate(otherUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(statusEx.getReason()).isEqualTo("User Service unavailable");
                });
        verify(directPairRepository, never()).save(any());
    }

    @Test
    void getOrCreate_uniqueViolation_returnsExisting() {
        UUID conversationId = UUID.randomUUID();
        DirectPair existing = new DirectPair(currentUserId, otherUserId, conversationId);
        when(directPairRepository.findById(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userLookup.requireActiveUser(otherUserId)).thenReturn(user(otherUserId));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any(ConversationParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(directPairRepository.save(any(DirectPair.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ConversationService.ConversationResult result = conversationService.getOrCreate(otherUserId);

        assertThat(result.created()).isFalse();
        assertThat(result.conversation().conversationId()).isEqualTo(conversationId);
    }

    @Test
    void getWithUser_missing_returnsNotFound() {
        when(directPairRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getWithUser(otherUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Conversation not found");
                });
    }

    @Test
    void getWithUser_self_returnsBadRequest() {
        assertThatThrownBy(() -> conversationService.getWithUser(currentUserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void list_emptyPage() {
        when(conversationRepository.findByParticipantUserIdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<ConversationResponse> page = conversationService.list(0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void list_enrichesOtherUserAndUnreadCount() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId);
        when(conversationRepository.findByParticipantUserIdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(conversation), PageRequest.of(0, 20), 1));
        when(participantRepository.findByConversationIdIn(List.of(conversationId))).thenReturn(List.of(
                new ConversationParticipant(conversationId, currentUserId),
                new ConversationParticipant(conversationId, otherUserId)
        ));
        when(userLookup.summariesByIds(any())).thenReturn(Map.of(otherUserId, user(otherUserId)));
        Message last = new Message(UUID.randomUUID(), conversationId, otherUserId, "Hi");
        when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                .thenReturn(Optional.of(last));
        when(readRepository.findByConversationIdAndUserId(conversationId, currentUserId))
                .thenReturn(Optional.of(new ConversationRead(
                        conversationId,
                        currentUserId,
                        Instant.parse("2026-08-25T10:00:00Z")
                )));
        when(messageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                conversationId,
                currentUserId,
                Instant.parse("2026-08-25T10:00:00Z")
        )).thenReturn(2L);

        PageResponse<ConversationResponse> page = conversationService.list(0, 20);

        assertThat(page.content()).hasSize(1);
        ConversationResponse row = page.content().getFirst();
        assertThat(row.conversationId()).isEqualTo(conversationId);
        assertThat(row.otherUserId()).isEqualTo(otherUserId);
        assertThat(row.otherUsername()).isEqualTo("ana");
        assertThat(row.otherDisplayName()).isEqualTo("Ana");
        assertThat(row.lastMessage().content()).isEqualTo("Hi");
        assertThat(row.unreadCount()).isEqualTo(2L);
    }

    @Test
    void list_userServiceDown_leavesOtherUsernameNull() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId);
        when(conversationRepository.findByParticipantUserIdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(conversation), PageRequest.of(0, 20), 1));
        when(participantRepository.findByConversationIdIn(List.of(conversationId))).thenReturn(List.of(
                new ConversationParticipant(conversationId, otherUserId)
        ));
        when(userLookup.summariesByIds(any())).thenReturn(Map.of());

        PageResponse<ConversationResponse> page = conversationService.list(-1, 999);

        ConversationResponse row = page.content().getFirst();
        assertThat(row.otherUserId()).isEqualTo(otherUserId);
        assertThat(row.otherUsername()).isNull();
        assertThat(row.otherDisplayName()).isNull();
        assertThat(row.unreadCount()).isZero();
    }

    private void stubResponseLookups() {
        lenient().when(conversationRepository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.of(new Conversation(id));
        });
        lenient().when(participantRepository.findByConversationIdAndUserIdNot(any(), any()))
                .thenReturn(Optional.of(new ConversationParticipant(UUID.randomUUID(), otherUserId)));
        lenient().when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        lenient().when(readRepository.findByConversationIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(messageRepository.countByConversationIdAndSenderIdNot(any(), any())).thenReturn(0L);
        lenient().when(userLookup.summariesByIds(any())).thenReturn(Map.of());
    }

    private static UserSummaryDto user(UUID userId) {
        return new UserSummaryDto(userId, "ana", "Ana", "http://pic", false);
    }

    private static void authenticate(UUID userId) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }
}
