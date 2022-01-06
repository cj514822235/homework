package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.DayCostElectricity;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.domain.PricePlanCost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PricePlanService {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }


    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        return electricityReadings.map(readings -> pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(readings, t))));

    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.multiply(timeElapsed).setScale(4,RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::getReading)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::getTime))
                .get();
        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::getTime))
                .get();

        return BigDecimal.valueOf(Duration.between(first.getTime(), last.getTime()).getSeconds() / 3600.0);
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForLastWeek(String smartMeterId, String pricePlanId) {

        int dayOfWeek = Instant.now().atZone(ZoneId.systemDefault()).getDayOfWeek().getValue();
        LocalDateTime currenTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Instant todayStart = currenTime.toInstant(ZoneOffset.UTC);

        List<ElectricityReading> weekElectricityReading = meterReadingService.getReadings(smartMeterId).get().stream().filter(electricityReading -> electricityReading
                .getTime().isBefore(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek-1)))&&
                electricityReading.getTime().isAfter(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek+6)))
                ).collect(Collectors.toList());

        PricePlan pricePlan = pricePlans.stream().filter(pricePlan1 -> pricePlan1.getPlanName().equals(pricePlanId)).collect(Collectors.toList()).get(0);
        Map<String,List<ElectricityReading>> electricityReadingMap =getElectricityReadingMap(weekElectricityReading);
        Map<String,BigDecimal> dayOfWeekCostMap = getDayOfWeekCostMap(electricityReadingMap,pricePlan);
        Map<String,BigDecimal> weekElectricityReadingMap = new HashMap<>();
        final BigDecimal[] sumCostOfWeek = {BigDecimal.valueOf(0)};
        dayOfWeekCostMap.forEach((s, electricityReading) -> {
            sumCostOfWeek[0] = sumCostOfWeek[0].add(electricityReading);

        });
        electricityReadingMap.forEach((s, electricityReadings) -> {
           BigDecimal sumUsage = electricityReadings.stream().map(ElectricityReading::getReading).reduce(BigDecimal.ZERO,BigDecimal::add);
           String  date = getFormatTime(electricityReadings.get(0).getTime());
           weekElectricityReadingMap.put(date,sumUsage);
        });
        weekElectricityReadingMap.put(smartMeterId,sumCostOfWeek[0]);
        LinkedHashMap<String,BigDecimal> linkedHashMap = weekElectricityReadingMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(e1,e2)->e1, LinkedHashMap::new));
        return Optional.of(linkedHashMap);
    }



    public Map<String, BigDecimal> getDayCostElectricityUsage(String smartMeterId, DayCostElectricity dayCostElectricity) {
        LocalDateTime currenTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Instant todayStart = currenTime.toInstant(ZoneOffset.UTC);
        List<ElectricityReading> electricityReadingList = meterReadingService.getReadings(smartMeterId).get().stream()
                .filter(electricityReading -> electricityReading.getTime().isAfter(todayStart)&&
                        electricityReading.getTime().isBefore(Instant.now())).collect(Collectors.toList());
        Map<String,BigDecimal> map = new HashMap<>();
        PricePlan pricePlan = pricePlans.stream().filter(pricePlan1 -> pricePlan1.getPlanName().equals(dayCostElectricity.getPricePlanId())).findFirst().get();
        BigDecimal sum = electricityReadingList.stream().map(ElectricityReading::getReading).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageUsage = sum.divide(BigDecimal.valueOf(electricityReadingList.size()),RoundingMode.HALF_UP);
        BigDecimal costOfDayUsage = averageUsage.multiply(BigDecimal.valueOf(24))
                .multiply(pricePlan.getPrice(dayCostElectricity.getElectricityReading().getTime().atZone(ZoneId.systemDefault()).toLocalDateTime()))
                .setScale(4,RoundingMode.HALF_UP);
        map.put(smartMeterId,costOfDayUsage);
        return map;
    }


    public Map<String, BigDecimal> getCostForDayOfWeekRank(String smartMeterId, DayCostElectricity dayCostElectricity) {
        List<ElectricityReading> electricityReadingList = getElectricityReadings(smartMeterId,dayCostElectricity);

        Map<String, List<ElectricityReading>> stringListHashMap = getElectricityReadingMap(electricityReadingList);

        PricePlan pricePlan = pricePlans.stream().filter(pricePlan1 -> pricePlan1.getPlanName().equals(dayCostElectricity.getPricePlanId())).findFirst().get();
        Map<String, BigDecimal> map = getDayOfWeekCostMap(stringListHashMap, pricePlan);
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(e1,e2)->e1, LinkedHashMap::new));
        }


    public Map<String, List<PricePlanCost>> getPricePlanRank(String smartMeterId, DayCostElectricity dayCostElectricity, Integer limit) {
         List<ElectricityReading> electricityReadingList = getElectricityReadings(smartMeterId, dayCostElectricity);
         Map<String,List<ElectricityReading>> electricityReadingMap = getElectricityReadingMap(electricityReadingList);
         Map<String, List<PricePlanCost>> map = new HashMap<>();
         electricityReadingMap.forEach(((s, electricityReadings) -> {
             List<PricePlanCost> pricePlanCosts = new ArrayList<>();
             pricePlans.forEach(pricePlan -> {
                    BigDecimal sumUsage = electricityReadings.stream().map(ElectricityReading::getReading).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal averageUsage = sumUsage.divide(BigDecimal.valueOf(electricityReadings.size()),RoundingMode.HALF_UP);
                    BigDecimal day0fWeekUsage = averageUsage.multiply(BigDecimal.valueOf(24)).setScale(4,RoundingMode.HALF_UP);
                    BigDecimal dayOfWeekCost = day0fWeekUsage.multiply(pricePlan.getPrice(electricityReadings.get(0).getTime().atZone(ZoneId.systemDefault()).toLocalDateTime())).setScale(4,RoundingMode.HALF_UP);
                    PricePlanCost pricePlanCost = new PricePlanCost(pricePlan.getPlanName(),dayOfWeekCost);
                    pricePlanCosts.add(pricePlanCost);
                });
             List<PricePlanCost> pricePlanCostList = pricePlanCosts.stream().sorted(Comparator.comparing(PricePlanCost::getUsage)).limit(limit).collect(Collectors.toList());
             map.put(s,pricePlanCostList);
             }));
         return map;
    }


    private List<ElectricityReading> getElectricityReadings(String smartMeterId, DayCostElectricity dayCostElectricity) {
        int dayOfWeek = dayCostElectricity.getElectricityReading().getTime().atZone(ZoneId.systemDefault()).getDayOfWeek().getValue();
        Instant instant = LocalDateTime.of(dayCostElectricity.getElectricityReading().getTime().atZone(ZoneId.systemDefault()).toLocalDate(),LocalTime.MIN).toInstant(ZoneOffset.UTC);
        return meterReadingService.getReadings(smartMeterId).get().stream()
                .filter(electricityReading -> electricityReading.getTime().isAfter(instant.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek-1)))
                        &&electricityReading.getTime().isBefore(instant.plusMillis(TimeUnit.DAYS.toMillis(8-dayOfWeek)))
                ).collect(Collectors.toList());
    }

    private Map<String, List<ElectricityReading>> getElectricityReadingMap(List<ElectricityReading> electricityReadingList) {
        Map<String,List<ElectricityReading>> stringListHashMap = new HashMap<>();

        electricityReadingList.forEach(electricityReading ->{
            if(stringListHashMap.containsKey(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString())){
                List<ElectricityReading> list1 = stringListHashMap.get(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString());
                list1.add(electricityReading);
                stringListHashMap.put(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list1 );
            }else {
                List<ElectricityReading> list = new ArrayList<>();
                list.add(electricityReading);
                stringListHashMap.put(electricityReading.getTime().minusMillis(TimeUnit.HOURS.toMillis(8)).atZone(ZoneId.systemDefault()).getDayOfWeek().toString(),list);
            }});
        return stringListHashMap;
    }

    private Map<String, BigDecimal> getDayOfWeekCostMap(Map<String, List<ElectricityReading>> stringListHashMap, PricePlan pricePlan) {
        Map<String,BigDecimal> map = new HashMap<>();
        stringListHashMap.forEach((s, electricityReadings) -> {
            BigDecimal sumUsage = electricityReadings.stream().map(ElectricityReading::getReading).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal averageUsage = sumUsage.divide(BigDecimal.valueOf(electricityReadings.size()),RoundingMode.HALF_UP);
            BigDecimal day0fWeekUsage = averageUsage.multiply(BigDecimal.valueOf(24)).setScale(4,RoundingMode.HALF_UP);
            BigDecimal dayOfWeekCost = day0fWeekUsage.multiply(pricePlan.getPrice(electricityReadings.get(0).getTime().atZone(ZoneId.systemDefault()).toLocalDateTime())).setScale(4,RoundingMode.HALF_UP);
            map.put(s,dayOfWeekCost);
        });
        return map;
    }

    private String getFormatTime(Instant instant){
        ZonedDateTime instant1 = instant.atZone(ZoneId.systemDefault());
        return DateTimeFormatter.ofPattern(YYYY_MM_DD).format(instant1);
    }

}
