package br.com.byop.aegis.notification.mapper;

import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import br.com.byop.aegis.notification.dto.NotificationResponse;
import br.com.byop.aegis.notification.dto.NotificationWithStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", source = "notification.id")
    @Mapping(target = "type", source = "notification.type")
    @Mapping(target = "title", source = "notification.title")
    @Mapping(target = "bodyMarkdown", source = "notification.bodyMarkdown")
    @Mapping(target = "presentationMode", source = "notification.presentationMode")
    @Mapping(target = "createdAt", source = "notification.createdAt")
    NotificationWithStatus toWithStatus(UserNotificationStatus status);
}
