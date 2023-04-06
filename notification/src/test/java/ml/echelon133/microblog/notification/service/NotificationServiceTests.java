package ml.echelon133.microblog.notification.service;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
