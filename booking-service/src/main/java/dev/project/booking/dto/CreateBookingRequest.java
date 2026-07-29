package dev.project.booking.dto;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(

        @NotNull UUID eventId,

        @Email
        String customerEmail,

        @Pattern(
                regexp = "^\\+?[0-9]{10,15}$",
                message = "Phone must contain 10 to 15 digits"
        )
        String customerPhone,

        @NotEmpty
        @Size(max = 10)
        List<@NotNull UUID> seatIds
){
    @AssertTrue(message = "Email or phone must be provided")
    public boolean hasContact() {
        return (customerEmail != null && !customerEmail.isBlank())
                || (customerPhone != null && !customerPhone.isBlank());
    }
}
