package connecta.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.notification.domain.Notification;
import connecta.notification.domain.NotificationType;
import connecta.notification.domain.ResourceType;
import connecta.notification.domain.Role;
import connecta.notification.dto.NotificationResponse;
import connecta.notification.dto.PageResponse;
import connecta.notification.dto.UnreadCountResponse;
import connecta.notification.repository.NotificationRepository;
import connecta.notification.security.AuthenticatedUser;
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
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;
    private UUID recipientId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
        recipientId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(recipientId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_returnsCurrentUserPage() {
        Notification notification = likeNotification();
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), any()))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));

        PageResponse<NotificationResponse> page = notificationService.list(0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().id()).isEqualTo(notification.getId());
        assertThat(page.content().getFirst().actorId()).isEqualTo(actorId);
        assertThat(page.content().getFirst().read()).isFalse();
        assertThat(page.page()).isEqualTo(0);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1);
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void list_clampsPageAndSize() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        notificationService.list(-3, 999);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(NotificationService.MAX_PAGE_SIZE);
    }

    @Test
    void unreadCount_returnsUnreadForCurrentUser() {
        when(notificationRepository.countByRecipientIdAndReadFalse(recipientId)).thenReturn(4L);

        UnreadCountResponse response = notificationService.unreadCount();

        assertThat(response.unreadCount()).isEqualTo(4L);
    }

    @Test
    void markRead_marksOwnUnreadNotification() {
        Notification notification = likeNotification();
        when(notificationRepository.findByIdAndRecipientId(notification.getId(), recipientId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markRead(notification.getId());

        assertThat(response.read()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_alreadyRead_returnsOkWithoutSave() {
        Notification notification = likeNotification();
        notification.markRead();
        when(notificationRepository.findByIdAndRecipientId(notification.getId(), recipientId))
                .thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markRead(notification.getId());

        assertThat(response.read()).isTrue();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_missingOrForeign_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndRecipientId(id, recipientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Notification not found");
                });
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllRead_updatesCurrentUser() {
        notificationService.markAllRead();

        verify(notificationRepository).markAllRead(recipientId);
    }

    @Test
    void list_unauthenticated_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> notificationService.list(0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private Notification likeNotification() {
        return new Notification(
                UUID.randomUUID(),
                recipientId,
                actorId,
                NotificationType.LIKE,
                ResourceType.POST,
                UUID.randomUUID(),
                "Someone liked your post",
                "msg-1"
        );
    }
}
