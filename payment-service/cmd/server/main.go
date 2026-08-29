package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net"
	paymentv1 "payment-service/gen/payment/v1"
	grpctransport "payment-service/internal/transport/grpc"
	"payment-service/migrations"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib" // Вот эта строка регистрирует драйвер

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

	cfg := config.Load()

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
	paymentService := service.NewPaymentService(paymentRepository, cfg.MockCheckoutURL)
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
		if err := grpcServer.Serve(listener); err != nil {
			log.Fatalf("failed to serve: %v", err)
		}
	}()

	if err := r.Run(":" + cfg.HTTPPort); err != nil {
		log.Fatalf("failed to start HTTP server: %v", err)
	}
}
