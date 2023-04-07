package ml.echelon133.microblog.notification.repository;

import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
            private UUID userToNotify = UUID.randomUUID();
            private UUID notificationSource = UUID.randomUUID();
            private Notification.Type type = Notification.Type.MENTION;
            private boolean read = false;

            private Builder() {}

            public Builder userToNotify(UUID userToNotify) {
                this.userToNotify = userToNotify;
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
                var notification = new Notification(this.userToNotify, this.notificationSource, this.type);
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
        var userToNotify = UUID.randomUUID();
        var numberOfNotifications = 10;

        // given
        for (int i = 0 ; i < numberOfNotifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).read(true).build());
        }

        // when
        var countUnread = notificationRepository.countByUserToNotifyAndReadFalse(userToNotify);

        // then
        assertEquals(0, countUnread);
    }

    @Test
    @DisplayName("Derived countByUserToNotifyAndReadFalse counts only unread notifications of a specified user")
    public void countByUserToNotifyAndReadFalse_UnreadNotificationsOfAnotherUser_ReturnsZero() {
        var userToNotify = UUID.randomUUID();
        var anotherUser = UUID.randomUUID();
        var numberOfNotifications = 10;

        // given
        // create notifications for userToNotify
        for (int i = 0 ; i < numberOfNotifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).build());
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
        var userToNotify = UUID.randomUUID();
        var numberOfReadNotifications = 10;
        var numberOfUnreadNotifications = 25;

        // given
        for (int i = 0 ; i < numberOfReadNotifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).read(true).build());
        }
        for (int i = 0 ; i < numberOfUnreadNotifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).build());
        }

        // when
        var countUnread = notificationRepository.countByUserToNotifyAndReadFalse(userToNotify);

        // then
        assertEquals(numberOfUnreadNotifications, countUnread);
    }

    @Test
    @DisplayName("Custom findNotificationsOfUser returns an empty page for a user who does not have any notifications")
    public void findNotificationsOfUser_NoNotifications_ReturnsEmptyPage() {
        var userId = UUID.randomUUID();

        // when
        var page = notificationRepository.findNotificationsOfUser(userId, Pageable.unpaged());

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findNotificationsOfUser returns both read and unread notifications")
    public void findNotificationsOfUser_ReadAndUnreadNotifications_ReturnsBothTypes() {
        var userToNotify = UUID.randomUUID();

        // given
        notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).read(true).build());
        notificationRepository.save(TestNotification.builder().userToNotify(userToNotify).build());

        // when
        var page = notificationRepository.findNotificationsOfUser(userToNotify, Pageable.unpaged());

        // then
        assertEquals(2, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findNotificationsOfUser returns notifications of all types")
    public void findNotificationsOfUser_DifferentTypesOfNotifications_ReturnsAllTypes() {
        var userToNotify = UUID.randomUUID();

        // given
        notificationRepository.save(
                TestNotification.builder().userToNotify(userToNotify).type(Notification.Type.MENTION).build()
        );
        notificationRepository.save(
                TestNotification.builder().userToNotify(userToNotify).type(Notification.Type.RESPONSE).build()
        );
        notificationRepository.save(
                TestNotification.builder().userToNotify(userToNotify).type(Notification.Type.QUOTE).build()
        );
        notificationRepository.save(
                TestNotification.builder().userToNotify(userToNotify).type(Notification.Type.FOLLOW).build()
        );

        // when
        var page = notificationRepository.findNotificationsOfUser(userToNotify, Pageable.unpaged());

        // then
        assertEquals(4, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findNotificationsOfUser returns only notifications of a specified user")
    public void findNotificationsOfUser_MultipleUsersNotified_ReturnsOnlyNotificationOfSpecifiedUser() {
        var user1 = UUID.randomUUID();
        var numberOfUser1Notifications = 9;
        var user2 = UUID.randomUUID();
        var numberOfUser2Notifications = 5;

        // given
        for (int i = 0; i < numberOfUser1Notifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(user1).build());
        }
        for (int i = 0; i < numberOfUser2Notifications; i++) {
            notificationRepository.save(TestNotification.builder().userToNotify(user2).build());
        }

        // when
        var page = notificationRepository.findNotificationsOfUser(user1, Pageable.unpaged());

        // then
        assertEquals(numberOfUser1Notifications, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findNotificationsOfUser returns most recent notifications first")
    public void findNotificationsOfUser_NotificationsWithDifferentDates_ReturnsInCorrectOrder() {
        var user = UUID.randomUUID();
        var numberOfNotifications = 10;

        // given
        List<UUID> expectedNotificationIdsOrdering = new ArrayList<>();
        // create notifications, each next notification being one hour older than the previous one
        for (int i = 0; i < numberOfNotifications; i++) {
            var notif = notificationRepository.save(TestNotification.builder().userToNotify(user).build());
            // by default, jpa auditing sets the initial dateCreated, and overwriting it
            // requires a second save
            notif.setDateCreated(Date.from(Instant.now().minus(i, ChronoUnit.HOURS)));
            notificationRepository.save(notif);
            expectedNotificationIdsOrdering.add(notif.getId());
        }

        // when
        var page = notificationRepository.findNotificationsOfUser(user, Pageable.unpaged());

        // then
        assertEquals(numberOfNotifications, page.getTotalElements());
        var receivedIdsOrdering = page.getContent().stream().map(NotificationDto::getNotificationId).toList();
        for (int i = 0; i < numberOfNotifications; i++) {
            assertEquals(expectedNotificationIdsOrdering.get(i), receivedIdsOrdering.get(i));
        }
    }

    @Test
    @DisplayName("Custom readSingleNotification does not read a notification that does not exist")
    public void readSingleNotification_NotificationNotFound_ReadsZero() {
        // when
        var readCount = notificationRepository.readSingleNotification(UUID.randomUUID());

        // then
        assertEquals(0, readCount);
    }

    @Test
    @DisplayName("Custom readSingleNotification only reads a single notification with specified id")
    public void readSingleNotification_MultipleUnreadNotifications_ReadsOnlySpecifiedNotification() {
        // given
        var n1 = notificationRepository.save(TestNotification.builder().build());
        var n2 = notificationRepository.save(TestNotification.builder().build());
        var n3 = notificationRepository.save(TestNotification.builder().build());

        // when
        var readCount = notificationRepository.readSingleNotification(n1.getId());

        // then
        assertEquals(1, readCount);
        assertTrue(notificationRepository.findById(n1.getId()).get().isRead());
        assertFalse(notificationRepository.findById(n2.getId()).get().isRead());
        assertFalse(notificationRepository.findById(n3.getId()).get().isRead());
    }

    @Test
    @DisplayName("Custom readAllNotificationsOfUser does not read notifications of a user that does not exist")
    public void readAllNotificationsOfUser_UserNotFound_ReadsZero() {
        // when
        var readCount = notificationRepository.readAllNotificationsOfUser(UUID.randomUUID());

        // then
        assertEquals(0, readCount);
    }

    @Test
    @DisplayName("Custom readAllNotificationsOfUser only reads notifications of a single user")
    public void readAllNotificationsOfUser_MultipleUsersWithNotifications_ReadsOnlyNotificationsOfSingleUser() {
        var users = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // given
        // create 10 notifications for each user
        for (UUID user : users) {
            for (int i = 0; i < 10; i++) {
                notificationRepository.save(TestNotification.builder().userToNotify(user).build());
            }
        }

        // when
        var user1 = users.get(0);
        var readCountOfUser1 = notificationRepository.readAllNotificationsOfUser(user1);

        // then
        assertEquals(10, readCountOfUser1);
        var allNotifications = notificationRepository.findAll();
        for (Notification n : allNotifications) {
            if (n.getUserToNotify().equals(user1)) {
                assertTrue(n.isRead());
            } else {
                assertFalse(n.isRead());
            }
        }
    }
}
