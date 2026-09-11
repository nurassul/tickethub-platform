package kafka

import (
	"context"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/twmb/franz-go/pkg/kgo"
)

const (
	maxDeliveryAttempts = 3
	retryDelay          = time.Second
)

type BookingEventsConsumer struct {
	client      *kgo.Client
	dltProducer *Producer
	dltTopic    string
}

func (c *BookingEventsConsumer) Run(
	ctx context.Context,
	handler func(context.Context, *kgo.Record) error,
) error {
	for {
		if ctx.Err() != nil {
			return nil
		}

		fetches := c.client.PollFetches(ctx)

		if ctx.Err() != nil {
			return nil
		}

		if errs := fetches.Errors(); len(errs) > 0 {
			return fmt.Errorf("poll kafka records: %v", errs)
		}

		var handleErr error

		fetches.EachRecord(func(record *kgo.Record) {
			if handleErr != nil {
				return
			}

			handleErr = c.processRecord(ctx, record, handler)
			if handleErr != nil {
				return
			}
		})

		if handleErr != nil {
			return fmt.Errorf("handle booking Kafka event: %w", handleErr)
		}
	}
}

func (c *BookingEventsConsumer) sendToDLT(
	ctx context.Context,
	record *kgo.Record,
	cause error,
) error {
	err := c.dltProducer.PublishRaw(
		ctx,
		c.dltTopic,
		string(record.Key),
		record.Value,
	)
	if err != nil {
		return fmt.Errorf("send record to dlt: %w", err)
	}

	log.Printf(
		"booking Kafka event moved to DLT: sourceTopic=%s, dltTopic=%s, partition=%d, offset=%d, error=%v",
		record.Topic,
		c.dltTopic,
		record.Partition,
		record.Offset,
		cause,
	)

	return nil

}

func (c *BookingEventsConsumer) processRecord(
	ctx context.Context,
	record *kgo.Record,
	handler func(context.Context, *kgo.Record) error,
) error {
	var lastErr error

	for attempt := 1; attempt <= maxDeliveryAttempts; attempt++ {
		err := handler(ctx, record)
		if err == nil {
			if err := c.client.CommitRecords(ctx, record); err != nil {
				return fmt.Errorf(
					"commit booking Kafka record: %w",
					err,
				)
			}

			return nil
		}

		lastErr = err

		log.Printf(
			"booking Kafka event processing failed: topic=%s, partition=%d, offset=%d, attempt=%d/%d, error=%v",
			record.Topic,
			record.Partition,
			record.Offset,
			attempt,
			maxDeliveryAttempts,
			err,
		)

		if attempt == maxDeliveryAttempts {
			break
		}

		timer := time.NewTimer(retryDelay)

		select {
		case <-ctx.Done():
			timer.Stop()
			return ctx.Err()

		case <-timer.C:
		}
	}

	if err := c.sendToDLT(ctx, record, lastErr); err != nil {
		return fmt.Errorf("send booking record to DLT: %w", err)
	}

	if err := c.client.CommitRecords(ctx, record); err != nil {
		return fmt.Errorf(
			"commit booking Kafka record after DLT: %w",
			err,
		)
	}

	return nil
}

func NewBookingEventsConsumer(
	brokers string,
	topic string,
	groupID string,
	dltProducer *Producer,
	dltTopic string,
) (*BookingEventsConsumer, error) {
	client, err := kgo.NewClient(
		kgo.SeedBrokers(strings.Split(brokers, ",")...),
		kgo.ConsumeTopics(topic),
		kgo.ConsumerGroup(groupID),
		kgo.DisableAutoCommit(),
	)
	if err != nil {
		return nil, err
	}

	return &BookingEventsConsumer{
		client:      client,
		dltProducer: dltProducer,
		dltTopic:    dltTopic,
	}, nil
}

func (c *BookingEventsConsumer) Close() {
	c.client.Close()
}
