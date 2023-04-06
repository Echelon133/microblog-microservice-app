package ml.echelon133.microblog.notification.controller;

import ml.echelon133.microblog.notification.service.NotificationService;
import ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.customBearerToken;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of NotificationController")
public class NotificationControllerTests {

    private MockMvc mvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders
                .standaloneSetup(notificationController)
                .setCustomArgumentResolvers(
                        // this is required to resolve @AuthenticationPrincipal in controller methods
                        new AuthenticationPrincipalArgumentResolver()
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
}
