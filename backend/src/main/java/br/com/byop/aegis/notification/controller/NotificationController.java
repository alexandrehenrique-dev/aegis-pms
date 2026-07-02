package br.com.byop.aegis.notification.controller;

import br.com.byop.aegis.notification.contract.CreateNotificationRequest;
import br.com.byop.aegis.notification.dto.NotificationResponse;
import br.com.byop.aegis.notification.dto.NotificationWithStatus;
import br.com.byop.aegis.notification.service.NotificationService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public NotificationController(NotificationService notificationService,
                                  AuthenticatedUserProvider authenticatedUserProvider) {
        this.notificationService = notificationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/notifications/mine")
    public List<NotificationWithStatus> listMine(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return notificationService.listMine(caller);
    }

    @GetMapping("/api/v1/notifications/mine/pending-modal")
    public ResponseEntity<NotificationWithStatus> getPendingModal(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        NotificationWithStatus pending = notificationService.getPendingModal(caller);
        return pending == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pending);
    }

    @PostMapping("/api/v1/notifications/{notificationId}/mark-shown")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markShown(@PathVariable("notificationId") UUID notificationId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        notificationService.markShown(caller, notificationId);
    }

    @PostMapping("/api/v1/notifications/{notificationId}/mark-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable("notificationId") UUID notificationId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        notificationService.markRead(caller, notificationId);
    }

    @PostMapping("/api/v1/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(@Valid @RequestBody CreateNotificationRequest request,
                                                   Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return notificationService.create(caller, request);
    }

    @GetMapping("/api/v1/notifications")
    public List<NotificationResponse> listNotifications(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return notificationService.listAll(caller);
    }
}
