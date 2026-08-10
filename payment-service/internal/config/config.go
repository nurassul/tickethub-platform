package config

import "os"

type Config struct {
	DatabaseURL     string
	HTTPPort        string
	MockCheckoutURL string
	GRPCPort        string
}

func Load() Config {
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

	return Config{
		HTTPPort:        port,
		DatabaseURL:     db,
		GRPCPort:        grpcPort,
		MockCheckoutURL: mockUrl,
	}
}
