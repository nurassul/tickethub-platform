package dev.project.booking.integration.payment;

import dev.project.contracts.payment.v1.PaymentServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PaymentGrpcConfig {


    // сетевое соединение с Go service
    @Bean(destroyMethod = "shutdown")
    public ManagedChannel managedChannel(
            @Value("${clients.payment.grpc.host}") String host,
            @Value("${clients.payment.grpc.port}") int port) {
        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
    }


    // generated объект для вызова RPC
    @Bean
    public PaymentServiceGrpc.PaymentServiceBlockingStub paymentBlockingStub(ManagedChannel paymentManagedChannel) {
        return PaymentServiceGrpc.newBlockingStub(paymentManagedChannel);
    }



}
