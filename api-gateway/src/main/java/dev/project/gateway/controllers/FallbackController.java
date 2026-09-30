package dev.project.gateway.controllers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {


    @RequestMapping("/global")
    public ResponseEntity<Map<String, String>> globalFallback(ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        log.warn(
                "Gateway fallback triggered: ", cause
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message",
                        "Service is temporarily unavailable. Please try again later."
                ));
    }


}
