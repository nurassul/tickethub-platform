package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/twmb/franz-go/pkg/kgo"
)

type Producer struct {
	client *kgo.Client
	topic  string
}

func (p *Producer) PublishRaw(
	ctx context.Context,
	topic string,
	messageKey string,
	payload []byte,
) error {
	result := p.client.ProduceSync(ctx, &kgo.Record{
		Topic: topic,
		Key:   []byte(messageKey),
		Value: payload,
	})

	if err := result.FirstErr(); err != nil {
		return fmt.Errorf("produce kafka record: %w", err)
	}

	return nil
}

func NewProducer(brokers string, topic string) (*Producer, error) {
	client, err := kgo.NewClient(
		kgo.SeedBrokers(strings.Split(brokers, ",")...),
	)
	if err != nil {
		return nil, err
	}

	return &Producer{
		client: client,
		topic:  topic,
	}, nil
}

func (p *Producer) Publish(ctx context.Context, event PaymentEvent) error {
	value, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("marshal payment event: %w", err)
	}

	result := p.client.ProduceSync(ctx, &kgo.Record{
		Topic: p.topic,
		Key:   []byte(event.CorrelationID.String()),
		Value: value,
	})

	if err := result.FirstErr(); err != nil {
		return fmt.Errorf("produce payment event: %w", err)
	}

	return nil
}

func (p *Producer) Close() {
	p.client.Close()
}
