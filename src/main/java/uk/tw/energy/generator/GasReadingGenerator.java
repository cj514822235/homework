package uk.tw.energy.generator;

import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.GasReading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GasReadingGenerator {
    public List<GasReading> generate(int number) {
        List<GasReading> readings = new ArrayList<>();
        Instant now = Instant.now().plusMillis(TimeUnit.HOURS.toMillis(8));

        Random readingRandomiser = new Random();
        for (int i = 0; i < number; i++) {
            double positiveRandomValue = Math.abs(readingRandomiser.nextGaussian());
            BigDecimal randomReading = BigDecimal.valueOf(positiveRandomValue).setScale(4, RoundingMode.CEILING);
            GasReading gasReading = new GasReading(now.minusSeconds(i * 3600L), randomReading);
            readings.add(gasReading);
        }

        readings.sort(Comparator.comparing(GasReading::getTime));
        return readings;
    }
}
