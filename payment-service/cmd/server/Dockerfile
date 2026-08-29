FROM golang:1.25.7-bookworm AS builder

WORKDIR /build

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux \
    go build \
    -trimpath \
    -ldflags="-s -w" \
    -o /out/payment-service \
    ./cmd/server

FROM alpine:3.21
WORKDIR /app
RUN apk add --no-cache ca-certificates \
&& addgroup -S payment \
&& adduser -S payment -G payment
COPY --from=builder /out/payment-service ./payment-service
COPY --from=builder /build/migrations ./migrations
USER payment
EXPOSE 8085
EXPOSE 9090
ENTRYPOINT ["./payment-service"]