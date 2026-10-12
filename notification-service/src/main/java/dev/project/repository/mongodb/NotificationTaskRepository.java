package dev.project.repository.mongodb;


import dev.project.repository.mongodb.entity.NotificationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationTaskRepository {

    private final MongoTemplate mongoTemplate;


    public Optional<NotificationDocument> claimNext(Instant now, Duration lockDuration) {
        Query query = new Query();
        Criteria readyToSend = Criteria
                .where("status")
                .in("PENDING", "FAILED")
                .and("nextAttemptAt")
                .lte(now);

        Criteria expiredLock = Criteria
                .where("status")
                .is("PROCESSING")
                .and("lockedUntil")
                .lte(now);

        Criteria availableTask = new Criteria();
        availableTask.orOperator(readyToSend, expiredLock);

        query.addCriteria(availableTask).with(Sort.by(Sort.Direction.ASC, "createdAt"));

        Update update = new Update();
        update
                .set("status", "PROCESSING")
                .set("lockedUntil", now.plus(lockDuration))
                .set("lockToken", UUID.randomUUID().toString())
                .inc("attempts", 1);


        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.returnNew(true);

       NotificationDocument claimed = mongoTemplate.findAndModify(query, update, findAndModifyOptions, NotificationDocument.class);

       return Optional.ofNullable(claimed);

    }


    public boolean markSent(String id, String lockToken, Instant sentAt) {
        Query query = new Query();

        Criteria criteria = Criteria
                .where("_id")
                .is(id)
                .and("status")
                .is("PROCESSING")
                .and("lockToken")
                .is(lockToken);

        query.addCriteria(criteria);

        Update update = new Update();
        update
                .set("status", "SENT")
                .set("sentAt", sentAt)
                .unset("lockedUntil")
                .unset("lockToken")
                .unset("errorMessage")
                .unset("nextAttemptAt");



        var res = mongoTemplate.updateFirst(query, update, NotificationDocument.class);

        return res.getMatchedCount() == 1;
    }


    public boolean markFailed(String id, String lockToken, String errorMessage, Instant nextAttemptAt) {
        Query query = new Query();

        Criteria criteria = Criteria
                .where("_id")
                .is(id)
                .and("status")
                .is("PROCESSING")
                .and("lockToken")
                .is(lockToken);

        query.addCriteria(criteria);

        Update update = new Update();
        update
                .set("status", "FAILED")
                .set("errorMessage", errorMessage)
                .set("nextAttemptAt", nextAttemptAt)
                .unset("lockedUntil")
                .unset("lockToken");



        var res = mongoTemplate.updateFirst(query, update, NotificationDocument.class);

        return res.getMatchedCount() == 1;
    }








}
