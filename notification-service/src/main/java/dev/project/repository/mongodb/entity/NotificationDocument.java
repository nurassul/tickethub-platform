package dev.project.repository.mongodb.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class NotificationDocument {

    @Id
    private String id;
    private String type;
    private String recipientEmail;
    private String subject;
    private String bookingId;
    private List<String> ticketIds;
    private String status;
    private String errorMessage;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant sentAt;

}
