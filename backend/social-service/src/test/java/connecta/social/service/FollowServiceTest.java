package connecta.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.social.client.UserSummaryDto;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowId;
import connecta.social.domain.FollowStatus;
import connecta.social.domain.Role;
import connecta.social.dto.FollowResponse;
import connecta.social.dto.PageResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserLookupService userLookup;

    private FollowService followService;
    private UUID followerId;
    private UUID followeeId;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, userLookup);
        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(followerId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void follow_self_returnsBadRequest() {
        assertThatThrownBy(() -> followService.follow(followerId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Cannot follow yourself");
                });
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_publicUser_createsAccepted() {
        when(followRepository.findById(any())).thenReturn(Optional.empty());
        when(userLookup.requireActiveUser(followeeId)).thenReturn(user(followeeId, false));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowService.FollowResult result = followService.follow(followeeId);

        assertThat(result.created()).isTrue();
        assertThat(result.follow().status()).isEqualTo(FollowStatus.ACCEPTED);
        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(followerId);
        assertThat(captor.getValue().getFolloweeId()).isEqualTo(followeeId);
        assertThat(captor.getValue().getStatus()).isEqualTo(FollowStatus.ACCEPTED);
    }

    @Test
    void follow_privateUser_createsPending() {
        when(followRepository.findById(any())).thenReturn(Optional.empty());
        when(userLookup.requireActiveUser(followeeId)).thenReturn(user(followeeId, true));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowResponse response = followService.follow(followeeId).follow();

        assertThat(response.status()).isEqualTo(FollowStatus.PENDING);
    }

    @Test
    void follow_alreadyExists_isIdempotent() {
        Follow existing = new Follow(followerId, followeeId, FollowStatus.ACCEPTED);
        when(followRepository.findById(any())).thenReturn(Optional.of(existing));

        FollowService.FollowResult result = followService.follow(followeeId);

        assertThat(result.created()).isFalse();
        assertThat(result.follow().status()).isEqualTo(FollowStatus.ACCEPTED);
        verify(userLookup, never()).requireActiveUser(any());
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_uniqueViolation_returnsExisting() {
        Follow existing = new Follow(followerId, followeeId, FollowStatus.PENDING);
        when(followRepository.findById(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userLookup.requireActiveUser(followeeId)).thenReturn(user(followeeId, true));
        when(followRepository.save(any(Follow.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        FollowService.FollowResult result = followService.follow(followeeId);

        assertThat(result.created()).isFalse();
        assertThat(result.follow().status()).isEqualTo(FollowStatus.PENDING);
    }

    @Test
    void unfollow_existing_deletes() {
        Follow existing = new Follow(followerId, followeeId, FollowStatus.ACCEPTED);
        when(followRepository.findById(any())).thenReturn(Optional.of(existing));

        followService.unfollow(followeeId);

        verify(followRepository).delete(existing);
    }

    @Test
    void unfollow_missing_isIdempotent() {
        when(followRepository.findById(any())).thenReturn(Optional.empty());

        followService.unfollow(followeeId);

        verify(followRepository, never()).delete(any());
    }

    @Test
    void accept_pending_setsAccepted() {
        authenticate(followeeId);
        Follow pending = new Follow(followerId, followeeId, FollowStatus.PENDING);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.of(pending));
        when(followRepository.save(pending)).thenReturn(pending);

        FollowResponse response = followService.accept(followerId);

        assertThat(response.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(pending.getStatus()).isEqualTo(FollowStatus.ACCEPTED);
        verify(followRepository).save(pending);
    }

    @Test
    void accept_alreadyAccepted_isIdempotent() {
        authenticate(followeeId);
        Follow accepted = new Follow(followerId, followeeId, FollowStatus.ACCEPTED);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.of(accepted));

        FollowResponse response = followService.accept(followerId);

        assertThat(response.status()).isEqualTo(FollowStatus.ACCEPTED);
        verify(followRepository, never()).save(any());
    }

    @Test
    void accept_missing_returnsNotFound() {
        authenticate(followeeId);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.accept(followerId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Follow request not found");
                });
    }

    @Test
    void reject_pending_deletes() {
        authenticate(followeeId);
        Follow pending = new Follow(followerId, followeeId, FollowStatus.PENDING);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.of(pending));

        followService.reject(followerId);

        verify(followRepository).delete(pending);
    }

    @Test
    void reject_missing_isIdempotent() {
        authenticate(followeeId);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.empty());

        followService.reject(followerId);

        verify(followRepository, never()).delete(any());
    }

    @Test
    void reject_accepted_returnsBadRequest() {
        authenticate(followeeId);
        Follow accepted = new Follow(followerId, followeeId, FollowStatus.ACCEPTED);
        when(followRepository.findById(eq(new FollowId(followerId, followeeId)))).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> followService.reject(followerId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("Follow is not pending");
                });
        verify(followRepository, never()).delete(any());
    }

    @Test
    void incomingRequests_returnsPendingOnly() {
        authenticate(followeeId);
        Follow pending = new Follow(followerId, followeeId, FollowStatus.PENDING);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(followRepository.findByFolloweeIdAndStatusOrderByCreatedAtDesc(
                followeeId,
                FollowStatus.PENDING,
                pageRequest
        )).thenReturn(new PageImpl<>(List.of(pending), pageRequest, 1));

        PageResponse<FollowResponse> page = followService.incomingRequests(0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().followerId()).isEqualTo(followerId);
        assertThat(page.content().getFirst().status()).isEqualTo(FollowStatus.PENDING);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    private void authenticate(UUID userId) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private static UserSummaryDto user(UUID id, boolean isPrivate) {
        return new UserSummaryDto(id, "ana", "Ana", null, isPrivate);
    }
}
