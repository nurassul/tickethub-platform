package httptransport

import (
	"errors"
	"log"
	"net/http"
	"payment-service/internal/domain"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

func (h *PaymentHTTPHandler) MarkSucceeded(
	c *gin.Context,
) {
	rawPaymentID := c.Param("paymentId")

	paymentID, err := uuid.Parse(rawPaymentID)
	if err != nil {
		c.JSON(
			http.StatusBadRequest, gin.H{
				"message": "paymentId not valid",
			})
		return
	}

	payment, err := h.paymentService.MarkSucceeded(c.Request.Context(), paymentID)
	if err != nil {
		if errors.Is(err, domain.ErrPaymentNotFound) {
			c.JSON(
				http.StatusNotFound, gin.H{
					"message": "payment not found",
				})
			return
		}

		if errors.Is(err, domain.ErrInvalidPaymentStatus) ||
			errors.Is(err, domain.ErrBookingExpired) {

			c.JSON(http.StatusConflict, gin.H{
				"message": err.Error(),
			})
			return
		}

		log.Printf("failed to mark 'SUCCEEDED' payment %s: %v", paymentID, err)
		c.JSON(
			http.StatusInternalServerError, gin.H{
				"message": "internal server error",
			})
		return
	}

	response := toPaymentResponse(payment)

	c.JSON(
		http.StatusOK, response)
}
