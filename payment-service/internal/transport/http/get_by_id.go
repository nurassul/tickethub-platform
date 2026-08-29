package httptransport

import (
	"errors"
	"log"
	"net/http"
	"payment-service/internal/domain"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

func (h *PaymentHTTPHandler) GetByID(
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

	payment, err := h.paymentService.GetByID(c.Request.Context(), paymentID)
	if err != nil {
		if errors.Is(err, domain.ErrPaymentNotFound) {
			c.JSON(
				http.StatusNotFound, gin.H{
					"message": "payment not found",
				})
			return
		}

		log.Printf("failed to get payment %s: %v", paymentID, err)
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
