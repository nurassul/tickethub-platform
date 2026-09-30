package dev.project.booking.api.services;


import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class GuestBookingTokenService {

    private static final Pattern TOKEN_FORMAT =
            Pattern.compile("[0-9a-f]{64}");


    public String hash(String token) {
        if (!isValid(token)) {
            throw new IllegalArgumentException(
                    "Invalid X-Booking-Token header"
            );
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    public boolean matches(String token, String storedHash) {
        if (!isValid(token) || storedHash == null || !TOKEN_FORMAT.matcher(storedHash).matches()) {
            return false;
        }

        byte[] actual = HexFormat.of().parseHex(hash(token));
        byte[] expected = HexFormat.of().parseHex(storedHash);

        return MessageDigest.isEqual(actual, expected);
    }


    private boolean isValid(String token) {
        return token != null
                && TOKEN_FORMAT.matcher(token).matches();
    }

}
