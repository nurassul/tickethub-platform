package dev.project.statistic.integration.kafka.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, ((consumerRecord, e) -> new TopicPartition(consumerRecord.topic() + ".dlt", consumerRecord.partition()))
        );

        deadLetterPublishingRecoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterPublishingRecoverer,
                new FixedBackOff(1000L, 2L)
        );

        errorHandler.addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class);

        errorHandler.setRetryListeners(
                new RetryListener() {
                    @Override
                    public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                        log.warn(
                                "Kafka event processing failed: topic={}, partition={}, offset={}, attempt={}, error={}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                deliveryAttempt,
                                ex.getClass().getSimpleName()
                        );
                    }

                    @Override
                    public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                        log.error(
                                "Kafka event moved to DLT: topic={}, dltTopic={}, partition={}, offset={}, error={}",
                                record.topic(),
                                record.topic() + ".dlt",
                                record.partition(),
                                record.offset(),
                                ex.getClass().getSimpleName()
                        );
                    }

                    @Override
                    public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
                        log.error(
                                "Kafka event failed while moving to DLT: topic={}, dltTopic={}, partition={}, offset={}, error={}",
                                record.topic(),
                                record.topic() + ".dlt",
                                record.partition(),
                                record.offset(),
                                failure.getClass().getSimpleName(),
                                failure
                        );
                    }
                }
        );

        factory.setCommonErrorHandler(errorHandler);



        return factory;
    }

}
