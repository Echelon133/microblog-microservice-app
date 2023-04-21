package ml.echelon133.microblog.notification.controller;

import ml.echelon133.microblog.notification.exception.NotificationNotFoundException;
import ml.echelon133.microblog.notification.exception.NotificationReadingForbiddenException;
import ml.echelon133.microblog.notification.service.NotificationService;
import ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.customBearerToken;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of NotificationController")
public class NotificationControllerTests {

    private MockMvc mvc;

    @InjectMocks
    private NotificationExceptionHandler notificationExceptionHandler;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders
                .standaloneSetup(notificationController)
                .setControllerAdvice(notificationExceptionHandler)
                .setCustomArgumentResolvers(
                        // this is required to resolve @AuthenticationPrincipal in controller methods
                        new AuthenticationPrincipalArgumentResolver(),
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("getUnreadCounter returns ok when user has unread notifications")
    public void getUnreadCounter_UserHasUnreadNotifications_ReturnsOk() throws Exception {
        var userId = UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID);
        var unreadCounter = 100;
        when(notificationService.countUnreadOfUser(userId)).thenReturn(unreadCounter);

        mvc.perform(
                        get("/api/notifications/unread-counter")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("unread", unreadCounter)));
    }

    @Test
    @DisplayName("getNotifications sets default page size to 20 and returns ok when there are notifications")
    public void getNotifications_PageSizeNotProvided_SetsDefaultSizeAndReturnsOk() throws Exception {
        var userId = UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID);
        var expectedPageSize = 20;
        var dto = new NotificationDto(UUID.randomUUID(), new Date(), UUID.randomUUID(), Notification.Type.MENTION, false);

        when(notificationService.findAllNotificationsOfUser(
                eq(userId),
                argThat(a -> a.getPageSize() == expectedPageSize)
        )).thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(expectedPageSize), 1));

        mvc.perform(
                        get("/api/notifications")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(expectedPageSize)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].notificationId", is(dto.getNotificationId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].notificationSource", is(dto.getNotificationSource().toString())))
                .andExpect(jsonPath("$.content[0].type", is(dto.getType().toString())))
                .andExpect(jsonPath("$.content[0].read", is(dto.isRead())));
    }

    @Test
    @DisplayName("readSingleNotification shows error when notification does not exist")
    public void readSingleNotification_NotificationNotFound_ReturnsExpectedError() throws Exception {
        var notificationId = UUID.randomUUID();

        when(notificationService.readSingleNotification(
                ArgumentMatchers.any(),
                eq(notificationId)
        )).thenThrow(new NotificationNotFoundException(notificationId));

        mvc.perform(
                        post("/api/notifications/" + notificationId + "/read")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(
                        String.format("notification %s could not be found", notificationId))
                ));
    }

    @Test
    @DisplayName("readSingleNotification shows error when user tries to read notification of someone else")
    public void readSingleNotification_UserReadsNotificationOfAnotherUser_ReturnsExpectedError() throws Exception {
        var userId = UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID);
        var notificationId = UUID.randomUUID();

        when(notificationService.readSingleNotification(userId, notificationId))
                .thenThrow(new NotificationReadingForbiddenException());

        mvc.perform(
                        post("/api/notifications/" + notificationId + "/read")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("users can only read their own notifications")));
    }

    @Test
    @DisplayName("readSingleNotification returns ok when notification is read")
    public void readSingleNotification_UserReadsNotification_ReturnsOk() throws Exception {
        var userId = UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID);
        var notificationId = UUID.randomUUID();

        when(notificationService.readSingleNotification(userId, notificationId)).thenReturn(1);

        mvc.perform(
                        post("/api/notifications/" + notificationId + "/read")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("read", 1)));
    }

    @Test
    @DisplayName("readAllNotifications returns ok when all notification are read")
    public void readAllNotifications_UserReadsAllNotifications_ReturnsOk() throws Exception {
        var userId = UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID);

        when(notificationService.readAllNotificationsOfUser(userId)).thenReturn(15);

        mvc.perform(
                        post("/api/notifications/read-all")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("read", 15)));
    }
}
