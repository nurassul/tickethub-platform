package domain

import "github.com/google/uuid"

type OutboxEvent struct {
	ID         uuid.UUID
	Topic      string
	MessageKey string
	EventType  string
	Payload    []byte
}
