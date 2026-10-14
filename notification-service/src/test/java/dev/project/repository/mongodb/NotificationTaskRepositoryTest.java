package dev.project.repository.mongodb;


import dev.project.repository.mongodb.entity.NotificationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(NotificationTaskRepository.class)
@Testcontainers
public class NotificationTaskRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    public NotificationTaskRepository notificationTaskRepository;

    @Autowired
    public MongoTemplate mongoTemplate;


    @Test
    public void shouldClaimPendingTask() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var claimed = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(claimed.isPresent());

        var task = claimed.orElseThrow();

        assertEquals("notification-1", task.getId());
        assertEquals("PROCESSING", task.getStatus());
        assertEquals(1, task.getAttempts());
        assertEquals(now.plusSeconds(60), task.getLockedUntil());
        assertNotNull(task.getLockToken());

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("PROCESSING", saved.getStatus());
        assertEquals(1, saved.getAttempts());
        assertEquals(now.plusSeconds(60), saved.getLockedUntil());
        assertEquals(task.getLockToken(), saved.getLockToken());

    }


    @Test
    public void shouldNotClaimTaskWithActiveLock() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var secondClaim = notificationTaskRepository.claimNext(now.plusSeconds(10), Duration.ofMinutes(1));

        assertTrue(secondClaim.isEmpty());

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("PROCESSING", saved.getStatus());
        assertEquals(1, saved.getAttempts());
        assertEquals(now.plusSeconds(60), saved.getLockedUntil());
        assertEquals(firstClaim.orElseThrow().getLockToken(), saved.getLockToken());

    }

    @Test
    public void shouldReclaimTaskAfterLockExpires() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var secondClaim = notificationTaskRepository.claimNext(now.plusSeconds(61), Duration.ofMinutes(1));

        assertTrue(secondClaim.isPresent());

        var secondTask = secondClaim.orElseThrow();

        assertEquals("notification-1", secondTask.getId());
        assertEquals("PROCESSING", secondTask.getStatus());
        assertEquals(2, secondTask.getAttempts());
        assertEquals(now.plusSeconds(121), secondTask.getLockedUntil());
        assertNotNull(secondTask.getLockToken());
        assertNotEquals(firstClaim.orElseThrow().getLockToken(), secondTask.getLockToken());

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("PROCESSING", saved.getStatus());
        assertEquals(2, saved.getAttempts());
        assertEquals(now.plusSeconds(121), saved.getLockedUntil());
        assertNotNull(saved.getLockToken());
        assertEquals(secondTask.getLockToken(), saved.getLockToken());

    }

    @Test
    public void shouldRejectMarkSentWithOldLockToken() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var secondClaim = notificationTaskRepository.claimNext(now.plusSeconds(61), Duration.ofMinutes(1));

        assertTrue(secondClaim.isPresent());

        var marked = notificationTaskRepository.markSent("notification-1", firstClaim.get().getLockToken(), now.plusSeconds(62));

        assertFalse(marked);

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("PROCESSING", saved.getStatus());
        assertEquals(secondClaim.orElseThrow().getLockToken(), saved.getLockToken());
        assertNull(saved.getSentAt());
    }

    @Test
    public void shouldMarkTaskSentWithCurrentLockToken() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var task = firstClaim.orElseThrow();

        var sentAt = now.plusSeconds(10);

        var marked = notificationTaskRepository.markSent("notification-1", task.getLockToken(), sentAt);

        assertTrue(marked);

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("SENT", saved.getStatus());
        assertEquals(sentAt, saved.getSentAt());
        assertEquals(1, saved.getAttempts());
        assertNull(saved.getLockedUntil());
        assertNull(saved.getLockToken());
        assertNull(saved.getNextAttemptAt());


    }

    @Test
    public void shouldMarkTaskFailedWithCurrentLock() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var task = firstClaim.orElseThrow();

        var retryAt = now.plusSeconds(40);

        var marked = notificationTaskRepository.markFailed("notification-1", task.getLockToken(), "SMTP unavailable", retryAt);

        assertTrue(marked);

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("FAILED", saved.getStatus());
        assertEquals("SMTP unavailable", saved.getErrorMessage());
        assertEquals(retryAt, saved.getNextAttemptAt());
        assertEquals(1, saved.getAttempts());
        assertNull(saved.getLockedUntil());
        assertNull(saved.getLockToken());
        assertNull(saved.getSentAt());
    }


    @Test
    public void shouldRetryFailedTaskOnlyWhenDue() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");
        var retryAt = now.plusSeconds(40);

        var notification = NotificationDocument.builder()
                .id("notification-1")
                .status("FAILED")
                .attempts(1)
                .createdAt(now.minusSeconds(60))
                .nextAttemptAt(retryAt)
                .errorMessage("SMTP unavailable")
                .build();

        mongoTemplate.insert(notification);

        var claim = notificationTaskRepository.claimNext(retryAt.minusSeconds(1), Duration.ofMinutes(1));

        assertTrue(claim.isEmpty());

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("FAILED", saved.getStatus());
        assertEquals(1, saved.getAttempts());

        var secondClaim = notificationTaskRepository.claimNext(retryAt, Duration.ofMinutes(1));

        assertTrue(secondClaim.isPresent());

        var secondTask = secondClaim.orElseThrow();

        assertEquals("notification-1", secondTask.getId());
        assertEquals("PROCESSING", secondTask.getStatus());
        assertEquals(2, secondTask.getAttempts());
        assertEquals(retryAt.plusSeconds(60), secondTask.getLockedUntil());
        assertNotNull(secondTask.getLockToken());

    }

    @Test
    public void shouldRejectMarkFailedWithOldLockToken() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        insertPendingTask(now);

        var firstClaim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(firstClaim.isPresent());

        var secondClaim = notificationTaskRepository.claimNext(now.plusSeconds(61), Duration.ofMinutes(1));

        assertTrue(secondClaim.isPresent());

        var marked = notificationTaskRepository.markFailed("notification-1", firstClaim.get().getLockToken(), "SMTP unavailable", now.plusSeconds(90));

        assertFalse(marked);

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("PROCESSING", saved.getStatus());
        assertEquals(secondClaim.orElseThrow().getLockToken(), saved.getLockToken());
        assertEquals(now.plusSeconds(121), saved.getLockedUntil());
        assertEquals(2, saved.getAttempts());
        assertNull(saved.getErrorMessage());
    }


    @Test
    public void shouldNotClaimSentTask() {
        Instant now = Instant.parse("2026-10-13T10:00:00Z");

        var notification = NotificationDocument.builder()
                .id("notification-1")
                .status("SENT")
                .attempts(1)
                .createdAt(now.minusSeconds(60))
                .sentAt(now.minusSeconds(10))
                .nextAttemptAt(now.minusSeconds(1))
                .build();

        mongoTemplate.insert(notification);

        var claim = notificationTaskRepository.claimNext(now, Duration.ofMinutes(1));

        assertTrue(claim.isEmpty());

        var saved = mongoTemplate.findById("notification-1", NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("notification-1", saved.getId());
        assertEquals("SENT", saved.getStatus());
        assertEquals(1, saved.getAttempts());
        assertEquals(now.minusSeconds(60), saved.getCreatedAt());
        assertEquals(now.minusSeconds(10), saved.getSentAt());
        assertEquals(now.minusSeconds(1), saved.getNextAttemptAt());
        assertNull(saved.getErrorMessage());
        assertNull(saved.getLockToken());
        assertNull(saved.getLockedUntil());
    }

    private void insertPendingTask(Instant now) {
        var notification = NotificationDocument.builder()
                .id("notification-1")
                .status("PENDING")
                .attempts(0)
                .createdAt(now.minusSeconds(60))
                .nextAttemptAt(now.minusSeconds(1))
                .build();

        mongoTemplate.insert(notification);
    }


    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), NotificationDocument.class);
    }
}
