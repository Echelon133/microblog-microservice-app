package ml.echelon133.microblog.notification.service;

import ml.echelon133.microblog.notification.exception.NotificationNotFoundException;
import ml.echelon133.microblog.notification.exception.NotificationReadingForbiddenException;
import ml.echelon133.microblog.notification.repository.NotificationRepository;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of NotificationService")
public class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("countUnreadOfUser correctly calls repository")
    public void countUnreadOfUser_UserIdProvided_CorrectlyCallsRepository() {
        var userId = UUID.randomUUID();

        // given
        given(notificationRepository.countByUserToNotifyAndReadFalse(userId)).willReturn(100);

        // when
        var count = notificationService.countUnreadOfUser(userId);

        // then
        assertEquals(100, count);
    }

    @Test
    @DisplayName("findAllNotificationsOfUser correctly calls repository")
    public void findAllNotificationsOfUser_ArgumentsProvided_CorrectlyCallsRepository() {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(20);
        var dto = new NotificationDto(UUID.randomUUID(), new Date(), UUID.randomUUID(), Notification.Type.MENTION, false);

        // given
        given(notificationRepository.findNotificationsOfUser(userId, pageable)).willReturn(
                new PageImpl<>(List.of(dto), pageable, 1)
        );

        // when
        var page = notificationService.findAllNotificationsOfUser(userId, pageable);

        // then
        assertEquals(dto, page.getContent().get(0));
    }

    @Test
    @DisplayName("readSingleNotification throws a NotificationNotFoundException when notification does not exist")
    public void readSingleNotification_NotificationNotFound_ThrowsException() {
        var notificationId = UUID.randomUUID();

        // given
        given(notificationRepository.findById(any())).willReturn(Optional.empty());

        // when
        String message = assertThrows(NotificationNotFoundException.class, () ->
            notificationService.readSingleNotification(UUID.randomUUID(), notificationId)
        ).getMessage();

        // then
        assertEquals(String.format("Notification with id %s could not be found", notificationId), message);
    }

    @Test
    @DisplayName("readSingleNotification throws a NotificationReadingForbiddenException when notification does not belong to a user who is trying to read it")
    public void readSingleNotification_UserReadsNotificationOfAnotherUser_ThrowsException() {
        var notificationId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var notification = new Notification(UUID.randomUUID(), UUID.randomUUID(), Notification.Type.MENTION);

        // given
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        String message = assertThrows(NotificationReadingForbiddenException.class, () ->
                notificationService.readSingleNotification(userId, notificationId)
        ).getMessage();

        // then
        assertEquals(String.format("User with id '%s' cannot read a notification with id '%s'", userId, notificationId), message);
    }

    @Test
    @DisplayName("readSingleNotification calls the repository when user tries to read their own notification")
    public void readSingleNotification_UserReadsTheirNotification_CorrectlyCallsRepository() throws Exception {
        var notificationId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var notification = new Notification(userId, UUID.randomUUID(), Notification.Type.MENTION);

        // given
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));
        given(notificationRepository.readSingleNotification(notificationId)).willReturn(1);

        // when
        var readCount = notificationService.readSingleNotification(userId, notificationId);

        // then
        assertEquals(1, readCount);
    }

    @Test
    @DisplayName("readAllNotificationsOfUser calls the repository when user tries to read all of their notifications")
    public void readAllNotificationsOfUser_UserReadsAllNotifications_CorrectlyCallsRepository() {
        var userId = UUID.randomUUID();

        // given
        given(notificationRepository.readAllNotificationsOfUser(userId)).willReturn(20);

        // when
        var readCount = notificationService.readAllNotificationsOfUser(userId);

        // then
        assertEquals(20, readCount);
    }
}
