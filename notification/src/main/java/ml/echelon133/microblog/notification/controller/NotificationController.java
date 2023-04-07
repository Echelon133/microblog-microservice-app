package ml.echelon133.microblog.notification.controller;

import ml.echelon133.microblog.notification.service.NotificationService;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public Page<NotificationDto> getNotifications(@PageableDefault(size = 20) Pageable pageable,
                                                  @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));
        return notificationService.findAllNotificationsOfUser(id, pageable);
    }

    @GetMapping("/unread-counter")
    public Map<String, Integer> getUnreadCounter(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));
        return Map.of("unread", notificationService.countUnreadOfUser(id));
    }

    @PostMapping("/{notificationId}/read")
    public Map<String, Integer> readSingleNotification(@PathVariable UUID notificationId,
                                                       @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) throws Exception {

        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));
        return Map.of("read", notificationService.readSingleNotification(id, notificationId));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> readAllNotifications(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));
        return Map.of("read", notificationService.readAllNotificationsOfUser(id));
    }
}
