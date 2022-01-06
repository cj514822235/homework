package uk.tw.energy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.GasMeterReadings;
import uk.tw.energy.domain.GasReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.domain.SeparateUsages;
import uk.tw.energy.service.MeterReadingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    public MeterReadingController(MeterReadingService meterReadingService) {
        this.meterReadingService = meterReadingService;
    }

    @PostMapping("/store")
    public ResponseEntity storeReadings(@RequestBody MeterReadings meterReadings) {
        if (!isMeterReadingsValid(meterReadings)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        meterReadingService.storeReadings(meterReadings.getSmartMeterId(), meterReadings.getElectricityReadings());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/gas-store")
    public ResponseEntity storeGasReadings(@RequestBody GasMeterReadings gasMeterReadings) {
        if (!isGasMeterReadingsValid(gasMeterReadings)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        meterReadingService.storeGasReadings(gasMeterReadings.getSmartMeterId(), gasMeterReadings.getGasReadings());
        return ResponseEntity.ok().build();
    }

    private boolean isMeterReadingsValid(MeterReadings meterReadings) {
        String smartMeterId = meterReadings.getSmartMeterId();
        List<ElectricityReading> electricityReadings = meterReadings.getElectricityReadings();
        return smartMeterId != null && !smartMeterId.isEmpty()
                && electricityReadings != null && !electricityReadings.isEmpty();
    }
    private boolean isGasMeterReadingsValid(GasMeterReadings gasMeterReadings) {
        String smartMeterId = gasMeterReadings.getSmartMeterId();
        List<GasReading> gasReadings = gasMeterReadings.getGasReadings();
        return smartMeterId != null && !smartMeterId.isEmpty()
                && gasReadings != null && !gasReadings.isEmpty();
    }

    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity readReadings(@PathVariable String smartMeterId) {
        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);
        return readings.isPresent()
                ? ResponseEntity.ok(readings.get())
                : ResponseEntity.notFound().build();
    }
    @GetMapping("gas/{smartMeterId}")
    public ResponseEntity gasReadings(@PathVariable String smartMeterId){
        Optional<List<GasReading>> readings = meterReadingService.getGasReadings(smartMeterId);
        return readings.isPresent()? ResponseEntity.ok(readings.get()):ResponseEntity.notFound().build();
    }

    @GetMapping("separate-usage/{smartMeterId}")

    public ResponseEntity separateUsages(@PathVariable String smartMeterId){
       Optional<SeparateUsages> lastWeekUsage = meterReadingService.getSeparateUsages(smartMeterId);
       return lastWeekUsage.isPresent()? ResponseEntity.ok(lastWeekUsage.get()):ResponseEntity.notFound().build();
    }

}
