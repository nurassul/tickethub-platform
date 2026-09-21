package dev.project.repository.mongodb;

import dev.project.repository.mongodb.entity.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
}
