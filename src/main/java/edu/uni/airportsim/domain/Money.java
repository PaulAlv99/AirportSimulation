package edu.uni.airportsim.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        amount = Objects.requireNonNull(amount, "amount");
        currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money euros(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("EUR"));
    }
}
