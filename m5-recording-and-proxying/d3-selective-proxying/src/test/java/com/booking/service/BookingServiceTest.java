package com.booking.service;

import com.booking.domain.BookingPayment;
import com.booking.domain.CreditCard;
import com.booking.gateway.PayBuddyGateway;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.booking.service.BookingResponse.BookingResponseStatus.SUCCESS;
import static com.booking.service.BookingResponse.BookingResponseStatus.SUSPECTED_FRAUD;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BookingServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private BookingService bookingService;

    @Before
    public void setUp() {
        bookingService = new BookingService(
                new PayBuddyGateway("localhost", 8080));

        stubFor(any(anyUrl()).willReturn(
                aResponse().proxiedFrom("http://localhost:8081")));
    }

    @Test
    public void shouldSucceedToPayForBooking() {
        // Given
        stubFor(get(urlPathEqualTo("/blacklisted-cards/1234-1234-1234-1234"))
                .willReturn(okJson("{" +
                        "  \"blacklisted\": \"false\"" +
                        "}")));

        // When
        final BookingResponse bookingResponse = bookingService.payForBooking(
                new BookingPayment(
                        "1111",
                        new BigDecimal("20.55"),
                        new CreditCard("1234-1234-1234-1234",
                                LocalDate.of(2018, 2, 1))));

        // Then
        assertThat(bookingResponse).isEqualTo(
                new BookingResponse("1111", SUCCESS));
    }

    @Test
    public void shouldFailToPayForBookingDueToFraud() {
        // Given
        stubFor(get(urlPathEqualTo("/blacklisted-cards/1234-1234-1234-1234"))
                .willReturn(okJson("{" +
                        "  \"blacklisted\": \"true\"" +
                        "}")));

        // When
        final BookingResponse bookingResponse = bookingService.payForBooking(
                new BookingPayment(
                        "1111",
                        new BigDecimal("20.55"),
                        new CreditCard("1234-1234-1234-1234",
                                LocalDate.of(2018, 2, 1))));

        // Then
        assertThat(bookingResponse)
                .isEqualTo(new BookingResponse(
                        "1111",
                        SUSPECTED_FRAUD));
    }
}
