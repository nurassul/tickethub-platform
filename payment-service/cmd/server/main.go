package main

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	paymentv1 "payment-service/gen/payment/v1"
	"payment-service/internal/kafka"
	"payment-service/internal/outbox"
	grpctransport "payment-service/internal/transport/grpc"
	"payment-service/migrations"
	"syscall"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib"

	"payment-service/internal/config"
	"payment-service/internal/database"
	"payment-service/internal/repository"
	"payment-service/internal/service"
	httptransport "payment-service/internal/transport/http"

	"github.com/gin-gonic/gin"
	"github.com/pressly/goose/v3"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	appCtx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	producer, err := kafka.NewProducer(
		cfg.KafkaBrokers,
		cfg.KafkaPaymentEventsTopic,
	)
	if err != nil {
		log.Fatalf("failed to create kafka producer: %v", err)
	}
	defer producer.Close()

	bookingEventsConsumer, err := kafka.NewBookingEventsConsumer(
		cfg.KafkaBrokers,
		cfg.KafkaBookingEventsTopic,
		cfg.KafkaBookingEventsGroupID,
		producer,
		cfg.KafkaBookingEventsDLTTopic,
	)
	if err != nil {
		log.Fatalf("failed to create kafka consumer: %v", err)
	}
	defer bookingEventsConsumer.Close()

	migrationDB, err := sql.Open("pgx", cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("failed to open database for migrations: %v", err)
	}
	goose.SetBaseFS(migrations.EmbedMigrations)
	if err := goose.SetDialect("postgres"); err != nil {
		log.Fatalf("failed to set goose dialect: %v", err)
	}

	log.Println("Running database migrations...")
	if err := goose.Up(migrationDB, "."); err != nil {
		log.Fatalf("failed to run migrations: %v", err)
	}
	migrationDB.Close()
	log.Println("Migrations applied successfully")

	r := gin.Default()
	pool, err := database.NewPostgresPool(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("failed to init postgres connection pool: %v", err)
	}
	defer pool.Close()

	paymentRepository := repository.NewPaymentRepository(pool)
	bookingExpiredRepository := repository.NewBookingExpiredEventRepository(pool)

	bookingExpiredHandler := kafka.NewBookingExpiredHandler(bookingExpiredRepository)
	consumerRestartDelay := 5 * time.Second
	go func() {
		for {
			if appCtx.Err() != nil {
				return
			}

			err := bookingEventsConsumer.Run(appCtx, bookingExpiredHandler.Handle)
			if err != nil && appCtx.Err() == nil {
				log.Printf("booking events consumer failed: %v", err)
				log.Printf(
					"booking events consumer will retry in %s",
					consumerRestartDelay,
				)
			}

			timer := time.NewTimer(consumerRestartDelay)
			select {
			case <-appCtx.Done():
				timer.Stop()
				return
			case <-timer.C:
			}

		}
	}()

	outboxRepository := repository.NewOutboxRepository(pool)
	outboxRelay := outbox.NewRelay(
		outboxRepository,
		producer,
		cfg.OutboxRelayBatchSize,
	)
	go func() {
		ticker := time.NewTicker(cfg.OutboxRelayInterval)
		defer ticker.Stop()

		for {
			if err := outboxRelay.RunOnce(appCtx); err != nil {
				log.Printf("outbox relay failed: %v", err)
			}

			select {
			case <-appCtx.Done():
				log.Println("outbox relay stopped")
				return
			case <-ticker.C:
			}
		}
	}()

	paymentService := service.NewPaymentService(
		paymentRepository,
		cfg.MockCheckoutURL,
		cfg.KafkaPaymentEventsTopic,
	)
	paymentHTTPHandler := httptransport.NewPaymentHTTPHandler(paymentService)
	paymentGRPCHandler := grpctransport.NewPaymentGRPCHandler(paymentService)

	payments := r.Group("/api/v1/mock/payments")
	payments.GET("/:paymentId", paymentHTTPHandler.GetByID)
	payments.POST("/:paymentId/succeed", paymentHTTPHandler.MarkSucceeded)
	payments.POST("/:paymentId/fail", paymentHTTPHandler.FailPayment)

	listener, err := net.Listen("tcp", ":"+cfg.GRPCPort)
	if err != nil {
		panic(fmt.Sprintf("failed to listen: %v", err))
	}
	grpcServer := grpc.NewServer(
		grpc.UnaryInterceptor(grpctransport.GinStyleLogger()))
	paymentv1.RegisterPaymentServiceServer(grpcServer, paymentGRPCHandler)
	reflection.Register(grpcServer)
	go func() {
		if err := grpcServer.Serve(listener); err != nil && !errors.Is(err, grpc.ErrServerStopped) {
			log.Printf("gRPC server failed: %v", err)
		}
	}()

	httpServer := &http.Server{
		Addr:    ":" + cfg.HTTPPort,
		Handler: r,
	}

	serverErr := make(chan error, 1)

	go func() {
		serverErr <- httpServer.ListenAndServe()
	}()

	select {
	case err := <-serverErr:
		if !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("HTTP server failed: %v", err)
		}
	case <-appCtx.Done():
		log.Println("shutdown signal received")
	}

	grpcServer.GracefulStop()

	shutdownCtx, cancelShutdown := context.WithTimeout(
		context.Background(),
		10*time.Second,
	)
	defer cancelShutdown()

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Printf("HTTP server shutdown failed: %v", err)
	}
}
