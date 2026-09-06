package dev.project.booking.integration.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.integration.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsListener {

    private static final String FAILED_TYPE = "payment.failed";
    private static final String SUCCEEDED_TYPE = "payment.succeeded";


    private final ObjectMapper mapper;
    private final PaymentEventHandler paymentEventHandler;


    @KafkaListener(topics = "${app.kafka.topics.payment-events}")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        PaymentEvent event = mapper.readValue(
                record.value(),
                PaymentEvent.class
        );

        validate(event);
        paymentEventHandler.handleEvent(event);

        log.debug(
                "Payment event parsed: id={}, type={}, bookingId={}, status={}",
                event.eventId(),
                event.eventType(),
                event.payload().bookingId(),
                event.payload().status()
        );
    }


    private void validate(PaymentEvent paymentEvent) {
        if (paymentEvent.eventVersion() != 1) {
            throw new IllegalArgumentException("Failed! 'eventVersion' is not equal to 1");
        }

        if (!(FAILED_TYPE.equals(paymentEvent.eventType()) || SUCCEEDED_TYPE.equals(paymentEvent.eventType()))) {
            throw new IllegalArgumentException("Incorrect 'eventType': " + paymentEvent.eventType());
        }

        if (paymentEvent.payload() == null) {
            throw new IllegalArgumentException("payload must be not null");
        }

        if (!Objects.equals(paymentEvent.correlationId(), paymentEvent.payload().bookingId())) {
            throw new IllegalArgumentException("'correlationId' is not equal to 'bookingId'");
        }


        if (SUCCEEDED_TYPE.equals(paymentEvent.eventType())
                && (!("SUCCEEDED".equals(paymentEvent.payload().status())) || paymentEvent.payload().paidAt() == null)

        ) {
            throw new IllegalArgumentException("payment.succeeded must have SUCCEEDED status");
        }

        if (FAILED_TYPE.equals(paymentEvent.eventType())
                && (!("FAILED".equals(paymentEvent.payload().status())) || (paymentEvent.payload().failureReason() == null|| paymentEvent.payload().failureReason().isBlank()))
        ) {
            throw new IllegalArgumentException("payment.failed must have FAILED status");
        }

    }

}
