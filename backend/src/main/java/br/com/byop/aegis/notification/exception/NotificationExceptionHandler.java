package br.com.byop.aegis.notification.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import br.com.byop.aegis.notification.controller.NotificationController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NotificationController.class)
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleNotificationNotFound() {
        return new CoreErrorResponse("NOTIFICATION_NOT_FOUND");
    }

    @ExceptionHandler(NotificationTargetUserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleNotificationTargetUserNotFound() {
        return new CoreErrorResponse("NOTIFICATION_TARGET_USER_NOT_FOUND");
    }

    @ExceptionHandler(OnboardingNotificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleOnboardingNotificationNotFound() {
        return new CoreErrorResponse("ONBOARDING_NOTIFICATION_NOT_FOUND");
    }

    @ExceptionHandler(InvalidNotificationTargetException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidNotificationTarget() {
        return new CoreErrorResponse("INVALID_NOTIFICATION_TARGET");
    }

    @ExceptionHandler(InsufficientNotificationRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleInsufficientNotificationRole() {
        return new CoreErrorResponse("NOTIFICATION_ADMIN_FORBIDDEN");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleIllegalArgument() {
        return new CoreErrorResponse("INVALID_NOTIFICATION_REQUEST");
    }
}
