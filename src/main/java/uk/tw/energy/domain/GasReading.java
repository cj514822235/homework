package uk.tw.energy.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class GasReading {
    private Instant time;
    private BigDecimal reading;

    public GasReading() { }

    public GasReading(Instant time, BigDecimal reading) {
        this.time = time;
        this.reading = reading;
    }

    public BigDecimal getReading() {
        return reading;
    }

    public Instant getTime() {
        return time;
    }
}
