package connecta.notification.service;

import connecta.notification.domain.Notification;
import connecta.notification.dto.NotificationResponse;
import connecta.notification.dto.PageResponse;
import connecta.notification.dto.UnreadCountResponse;
import connecta.notification.repository.NotificationRepository;
import connecta.notification.security.AuthenticatedUser;
import connecta.notification.security.SecurityUtils;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Page<Notification> result = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                currentUser.id(),
                pageRequest(page, size)
        );
        return PageResponse.from(result.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount() {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        return new UnreadCountResponse(notificationRepository.countByRecipientIdAndReadFalse(currentUser.id()));
    }

    @Transactional
    public NotificationResponse markRead(UUID id) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipientId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.isRead()) {
            notification.markRead();
            notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead() {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        notificationRepository.markAllRead(currentUser.id());
    }

    static PageRequest pageRequest(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize);
    }
}
