package ml.echelon133.microblog.notification.controller;

import ml.echelon133.microblog.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-counter")
    public Map<String, Integer> getUnreadCounter(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));
        return Map.of("unread", notificationService.countUnreadOfUser(id));
    }
}
