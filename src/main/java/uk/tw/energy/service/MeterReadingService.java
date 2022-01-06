package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.GasReading;
import uk.tw.energy.domain.SeparateUsages;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MeterReadingService {

    private final Map<String, List<ElectricityReading>> meterAssociatedReadings;
    private final Map<String,List<GasReading>> gasMeterAssociatedReadings;


    public MeterReadingService(Map<String, List<ElectricityReading>> meterAssociatedReadings, Map<String,List<GasReading>> gasMeterAssociatedReadings) {
        this.meterAssociatedReadings = meterAssociatedReadings;
        this.gasMeterAssociatedReadings= gasMeterAssociatedReadings;
    }

    public Optional<List<ElectricityReading>> getReadings(String smartMeterId) {
        return Optional.ofNullable(meterAssociatedReadings.get(smartMeterId));
    }

    public void storeReadings(String smartMeterId, List<ElectricityReading> electricityReadings) {
        if (!meterAssociatedReadings.containsKey(smartMeterId)) {
            meterAssociatedReadings.put(smartMeterId, new ArrayList<>());
        }
        meterAssociatedReadings.get(smartMeterId).addAll(electricityReadings);
    }

    public Optional<List<GasReading>> getGasReadings(String smartMeterId) {
        return Optional.ofNullable(gasMeterAssociatedReadings.get(smartMeterId));
    }

    public void storeGasReadings(String smartMeterId, List<GasReading> gasReadings) {
        if (!meterAssociatedReadings.containsKey(smartMeterId)) {
            gasMeterAssociatedReadings.put(smartMeterId, new ArrayList<>());
        }
        gasMeterAssociatedReadings.get(smartMeterId).addAll(gasReadings);
    }

    public Optional<SeparateUsages> getSeparateUsages(String smartMeterId) {
        Map<String,BigDecimal> dayOfWeekElectricityReadings = new HashMap<>();
        Map<String,BigDecimal> dayOfWeekGasReadings = new HashMap<>();
        int dayOfWeek = Instant.now().atZone(ZoneId.systemDefault()).getDayOfWeek().getValue();
        LocalDateTime currenTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Instant todayStart = currenTime.toInstant(ZoneOffset.UTC);

        List<ElectricityReading> weekElectricityReadings = getReadings(smartMeterId).get().stream().filter(electricityReading -> electricityReading
                .getTime().isBefore(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek-1)))&&
                electricityReading.getTime().isAfter(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek+6)))
        ).collect(Collectors.toList());

        List<GasReading> weekGasReadings = getGasReadings(smartMeterId).get().stream().filter(gasReading -> gasReading
                .getTime().isBefore(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek-1)))&&
                gasReading.getTime().isAfter(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek+6)))
        ).collect(Collectors.toList());
        Map<String,List<BigDecimal>> electricityReadingsMap = getReadingMap(weekElectricityReadings);
        Map<String,List<BigDecimal>> gasReadingMap = getGasReadingMap(weekGasReadings);
        electricityReadingsMap.forEach((s, bigDecimals) -> {
            BigDecimal bigDecimal = bigDecimals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            dayOfWeekElectricityReadings.put(s,bigDecimal);
        });
        gasReadingMap.forEach((s, bigDecimals) -> {
            BigDecimal bigDecimal = bigDecimals.stream().reduce(BigDecimal.ZERO,BigDecimal::add);
            dayOfWeekGasReadings.put(s,bigDecimal);
        });
        final BigDecimal[] electricityUsage = {BigDecimal.valueOf(0)};
        dayOfWeekElectricityReadings.forEach((s, bigDecimals) -> electricityUsage[0] = electricityUsage[0].add(bigDecimals));
        final BigDecimal[] gasUsage = {BigDecimal.valueOf(0)};
        dayOfWeekGasReadings.forEach((s, bigDecimal) -> gasUsage[0] = gasUsage[0].add(bigDecimal));
        SeparateUsages separateUsages = SeparateUsages.builder()
                .gasUsages(dayOfWeekGasReadings)
                .electricityUsages(dayOfWeekElectricityReadings)
                .electricityUsagesOfWeek(electricityUsage[0])
                .gasUsagesOfWeek(gasUsage[0])
                .build();
        return Optional.of(separateUsages);

    }

    private Map<String, List<BigDecimal>> getReadingMap(List<ElectricityReading> electricityReadingList) {
        Map<String,List<BigDecimal>> readingsMap = new HashMap<>();
        electricityReadingList.forEach(electricityReading ->{
            if(readingsMap.containsKey(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString())){
                List<BigDecimal> list1 = readingsMap.get(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString());
                list1.add(electricityReading.getReading());
                readingsMap.put(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list1 );
            }else {
                List<BigDecimal> list = new ArrayList<>();
                list.add(electricityReading.getReading());
                readingsMap.put(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list);
            }});


        return readingsMap;
    }
    private Map<String, List<BigDecimal>> getGasReadingMap(List<GasReading> gasReadingList) {

        Map<String,List<BigDecimal>> gasReadingsMap = new HashMap<>();
        gasReadingList.forEach(gasReading ->{
            if(gasReadingsMap.containsKey(gasReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString())){
                List<BigDecimal> list1 = gasReadingsMap.get(gasReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString());
                list1.add(gasReading.getReading());
                gasReadingsMap.put(gasReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list1 );
            }else {
                List<BigDecimal> list = new ArrayList<>();
                list.add(gasReading.getReading());
                gasReadingsMap.put(gasReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list);
            }});
        return gasReadingsMap;
    }

}
