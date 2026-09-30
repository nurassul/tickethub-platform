package dev.project.booking.api.aspect;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.api.services.GuestBookingTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GuestBookingTokenService guestBookingTokenService;

    private static final String HEADER = "Idempotency-Key";

    @Around("@annotation(dev.project.booking.api.annotation.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String idempotencyKey = request.getHeader(HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header: " + HEADER);
        }

        String guestToken = request.getHeader("X-Booking-Token");
        String guestTokenHash = guestBookingTokenService.hash(guestToken);

        String redisKey = "idemp:booking:v2:"
                + guestTokenHash
                + ":"
                + idempotencyKey;
        String cachedValue = redisTemplate.opsForValue().get(redisKey);

        if (cachedValue != null) {
            if ("PROCESSING".equals(cachedValue)) {
                log.warn("Request already processing. Rejecting duplicate");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is already being processed");
            }

            log.info("Found ready response in Redis. Sending it to client");

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Type returnType = signature.getMethod().getGenericReturnType();

            try {
                if (signature.getReturnType() == ResponseEntity.class && returnType instanceof ParameterizedType paramType) {
                    Type actualBodyType = paramType.getActualTypeArguments()[0];

                    Object parsedBody = objectMapper.readValue(cachedValue, objectMapper.constructType(actualBodyType));

                    return ResponseEntity.ok(parsedBody);
                } else {
                    return objectMapper.readValue(cachedValue, objectMapper.constructType(returnType));
                }
            } catch (Exception e) {
                log.error("Error parsing JSON from Redis", e);
                throw new RuntimeException("Failed to deserialize cached response", e);
            }


        }

        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(1));

        if (Boolean.FALSE.equals(isFirst)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race condition prevented");
        }

        try {
            Object result = joinPoint.proceed();

            Object bodyToCache = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                bodyToCache = responseEntity.getBody();
            }

            String jsonResult = objectMapper.writeValueAsString(bodyToCache);
            redisTemplate.opsForValue().set(redisKey, jsonResult, Duration.ofHours(24));
            return result;

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }

    }


}
