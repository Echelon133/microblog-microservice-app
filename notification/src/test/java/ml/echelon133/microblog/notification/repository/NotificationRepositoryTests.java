package ml.echelon133.microblog.notification.repository;

import ml.echelon133.microblog.shared.notification.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of NotificationRepository")
public class NotificationRepositoryTests {

    @Autowired
    private NotificationRepository notificationRepository;

    private static class TestNotification {

        private static class Builder {
            private UUID targetedUser = UUID.randomUUID();
            private UUID notificationSource = UUID.randomUUID();
            private Notification.Type type = Notification.Type.MENTION;
            private boolean read = false;

            private Builder() {}

            public Builder targetedUser(UUID targetedUser) {
                this.targetedUser = targetedUser;
                return this;
            }

            public Builder notificationSource(UUID notificationSource) {
                this.notificationSource = notificationSource;
                return this;
            }

            public Builder type(Notification.Type type) {
                this.type = type;
                return this;
            }

            public Builder read(boolean read) {
                this.read = read;
                return this;
            }

            public Notification build() {
                var notification = new Notification(this.targetedUser, this.notificationSource, this.type);
                notification.setRead(this.read);
                return notification;
            }
        }

        public static Builder builder() {
            return new Builder();
        }
    }

    @Test
    @DisplayName("Derived countByUserToNotifyAndReadFalse returns zero when there are only read notifications")
    public void countByUserToNotifyAndReadFalse_OnlyReadNotifications_ReturnsZero() {
        var targetedUser = UUID.randomUUID();
        var numberOfNotifications = 10;

        // given
        for (int i = 0 ; i < numberOfNotifications; i++) {
            notificationRepository.save(TestNotification.builder().targetedUser(targetedUser).read(true).build());
        }

        // when
        var countUnread = notificationRepository.countByUserToNotifyAndReadFalse(targetedUser);

        // then
        assertEquals(0, countUnread);
    }

    @Test
    @DisplayName("Derived countByUserToNotifyAndReadFalse counts only unread notifications of a specified user")
    public void countByUserToNotifyAndReadFalse_UnreadNotificationsOfAnotherUser_ReturnsZero() {
        var targetedUser = UUID.randomUUID();
        var anotherUser = UUID.randomUUID();
        var numberOfNotifications = 10;

        // given
        // create notifications for targetedUser
        for (int i = 0 ; i < numberOfNotifications; i++) {
            notificationRepository.save(TestNotification.builder().targetedUser(targetedUser).build());
        }

        // when
        // but check notifications for anotherUser, who does not have any notifications
        var countUnread = notificationRepository.countByUserToNotifyAndReadFalse(anotherUser);

        // then
        assertEquals(0, countUnread);
    }

    @Test
    @DisplayName("Derived countByUserToNotifyAndReadFalse correctly counts unread notifications of a user")
    public void countByUserToNotifyAndReadFalse_MixedReadAndUnreadNotifications_OnlyCountsUnread() {
        var targetedUser = UUID.randomUUID();
        var numberOfReadNotifications = 10;
        var numberOfUnreadNotifications = 25;

        // given
        for (int i = 0 ; i < numberOfReadNotifications; i++) {
            notificationRepository.save(TestNotification.builder().targetedUser(targetedUser).read(true).build());
        }
        for (int i = 0 ; i < numberOfUnreadNotifications; i++) {
            notificationRepository.save(TestNotification.builder().targetedUser(targetedUser).build());
        }

        // when
        var countUnread = notificationRepository.countByUserToNotifyAndReadFalse(targetedUser);

        // then
        assertEquals(numberOfUnreadNotifications, countUnread);
    }
}
