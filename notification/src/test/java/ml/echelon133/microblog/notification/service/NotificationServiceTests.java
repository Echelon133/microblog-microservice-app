package ml.echelon133.microblog.notification.service;

import ml.echelon133.microblog.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
