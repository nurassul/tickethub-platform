package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	DatabaseURL                string
	HTTPPort                   string
	MockCheckoutURL            string
	GRPCPort                   string
	KafkaBrokers               string
	KafkaPaymentEventsTopic    string
	KafkaBookingEventsTopic    string
	KafkaBookingEventsGroupID  string
	KafkaBookingEventsDLTTopic string
	OutboxRelayInterval        time.Duration
	OutboxRelayBatchSize       int
}

func Load() (Config, error) {
	port := os.Getenv("HTTP_PORT")
	if port == "" {
		port = "8085"
	}

	db := os.Getenv("DATABASE_URL")
	if db == "" {
		db = "postgres://postgres:postgres@localhost:5433/payment_db?sslmode=disable"
	}

	mockUrl := os.Getenv("MOCK_CHECKOUT_URL")
	if mockUrl == "" {
		mockUrl = "http://localhost:8085/api/v1/mock/payments"
	}

	grpcPort := os.Getenv("GRPC_PORT")
	if grpcPort == "" {
		grpcPort = "9090"
	}

	kafkaBrokers := os.Getenv("KAFKA_BROKERS")
	if kafkaBrokers == "" {
		kafkaBrokers = "localhost:9092"
	}

	kafkaPaymentEventsTopic := os.Getenv("KAFKA_PAYMENT_EVENTS_TOPIC")
	if kafkaPaymentEventsTopic == "" {
		kafkaPaymentEventsTopic = "tickethub.payment.events.v1"
	}

	kafkaBookingEventsTopic := os.Getenv("KAFKA_BOOKING_EVENTS_TOPIC")
	if kafkaBookingEventsTopic == "" {
		kafkaBookingEventsTopic = "tickethub.booking.events.v1"
	}

	kafkaBookingEventsGroupID := os.Getenv("KAFKA_BOOKING_EVENTS_GROUP_ID")
	if kafkaBookingEventsGroupID == "" {
		kafkaBookingEventsGroupID = "payment-booking-events-v1"
	}

	kafkaBookingEventsDLTTopic := os.Getenv("KAFKA_BOOKING_EVENTS_DLT_TOPIC")
	if kafkaBookingEventsDLTTopic == "" {
		kafkaBookingEventsGroupID = "tickethub.booking.events.v1.dlt"
	}

	outboxRelayInterval := 2 * time.Second

	if rawInterval := os.Getenv("OUTBOX_RELAY_INTERVAL"); rawInterval != "" {
		parsedInterval, err := time.ParseDuration(rawInterval)
		if err != nil {
			return Config{}, fmt.Errorf(
				"parse OUTBOX_RELAY_INTERVAL: %w",
				err,
			)
		}

		if parsedInterval <= 0 {
			return Config{}, fmt.Errorf(
				"OUTBOX_RELAY_INTERVAL must be positive",
			)
		}

		outboxRelayInterval = parsedInterval
	}

	outboxRelayBatchSize := 100

	if rawBatchSize := os.Getenv("OUTBOX_RELAY_BATCH_SIZE"); rawBatchSize != "" {
		parsedBatchSize, err := strconv.Atoi(rawBatchSize)
		if err != nil {
			return Config{}, fmt.Errorf(
				"parse OUTBOX_RELAY_BATCH_SIZE: %w",
				err,
			)
		}

		if parsedBatchSize <= 0 {
			return Config{}, fmt.Errorf(
				"OUTBOX_RELAY_BATCH_SIZE must be positive",
			)
		}

		outboxRelayBatchSize = parsedBatchSize
	}

	return Config{
		HTTPPort:                   port,
		DatabaseURL:                db,
		GRPCPort:                   grpcPort,
		MockCheckoutURL:            mockUrl,
		KafkaBrokers:               kafkaBrokers,
		KafkaPaymentEventsTopic:    kafkaPaymentEventsTopic,
		KafkaBookingEventsTopic:    kafkaBookingEventsTopic,
		KafkaBookingEventsGroupID:  kafkaBookingEventsGroupID,
		KafkaBookingEventsDLTTopic: kafkaBookingEventsDLTTopic,
		OutboxRelayInterval:        outboxRelayInterval,
		OutboxRelayBatchSize:       outboxRelayBatchSize,
	}, nil
}
