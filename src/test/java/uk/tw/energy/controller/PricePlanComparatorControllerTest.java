package uk.tw.energy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.tw.energy.domain.DayCostElectricity;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.domain.PricePlanCost;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PricePlanComparatorControllerTest {

    private static final String PRICE_PLAN_1_ID = "test-supplier";
    private static final String PRICE_PLAN_2_ID = "best-supplier";
    private static final String PRICE_PLAN_3_ID = "second-best-supplier";
    private static final String SMART_METER_ID = "smart-meter-id";
    private PricePlanComparatorController controller;
    private MeterReadingService meterReadingService;
    private AccountService accountService;

    @BeforeEach
    public void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>(), new HashMap<>());
        List<PricePlan.PeakTimeMultiplier> peakTimeMultipliers = new ArrayList<>();
        PricePlan pricePlan1 = new PricePlan(PRICE_PLAN_1_ID, null, BigDecimal.TEN, peakTimeMultipliers);
        PricePlan pricePlan2 = new PricePlan(PRICE_PLAN_2_ID, null, BigDecimal.ONE, peakTimeMultipliers);
        PricePlan pricePlan3 = new PricePlan(PRICE_PLAN_3_ID, null, BigDecimal.valueOf(2), peakTimeMultipliers);

        List<PricePlan> pricePlans = Arrays.asList(pricePlan1, pricePlan2, pricePlan3);
        PricePlanService tariffService = new PricePlanService(pricePlans, meterReadingService);

        Map<String, String> meterToTariffs = new HashMap<>();
        meterToTariffs.put(SMART_METER_ID, PRICE_PLAN_1_ID);
        accountService = new AccountService(meterToTariffs);

        controller = new PricePlanComparatorController(tariffService, accountService);
    }

    @Test
    public void shouldCalculateCostForMeterReadingsForEveryPricePlan() {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(15.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        Map<String, BigDecimal> expectedPricePlanToCost = new HashMap<>();
        expectedPricePlanToCost.put(PRICE_PLAN_1_ID, BigDecimal.valueOf(100.0).setScale(4,RoundingMode.HALF_UP));
        expectedPricePlanToCost.put(PRICE_PLAN_2_ID, BigDecimal.valueOf(10.0).setScale(4,RoundingMode.HALF_UP));
        expectedPricePlanToCost.put(PRICE_PLAN_3_ID, BigDecimal.valueOf(20.0).setScale(4,RoundingMode.HALF_UP));

        Map<String, Object> expected = new HashMap<>();
        expected.put(PricePlanComparatorController.PRICE_PLAN_ID_KEY, PRICE_PLAN_1_ID);
        expected.put(PricePlanComparatorController.PRICE_PLAN_COMPARISONS_KEY, expectedPricePlanToCost);
        assertThat(controller.calculatedCostForEachPricePlan(SMART_METER_ID).getBody()).isEqualTo(expected);
    }

    @Test
    public void shouldRecommendCheapestPricePlansNoLimitForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(35.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(9.5000).setScale(4,RoundingMode.HALF_UP)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(19.0000).setScale(4,RoundingMode.HALF_UP )));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_1_ID, BigDecimal.valueOf(95.0000).setScale(4,RoundingMode.HALF_UP)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, null).getBody()).isEqualTo(expectedPricePlanToCost);
    }


    @Test
    public void shouldRecommendLimitedCheapestPricePlansForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(2700), BigDecimal.valueOf(5.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(20.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(9.375).setScale(4,RoundingMode.HALF_UP)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(18.75).setScale(4,RoundingMode.HALF_UP)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, 2).getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void shouldRecommendCheapestPricePlansMoreThanLimitAvailableForMeterUsage() throws Exception {

        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25.0));
        ElectricityReading otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, Arrays.asList(electricityReading, otherReading));

        List<Map.Entry<String, BigDecimal>> expectedPricePlanToCost = new ArrayList<>();
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_2_ID, BigDecimal.valueOf(14.0000).setScale(4,RoundingMode.HALF_UP)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_3_ID, BigDecimal.valueOf(28.0).setScale(4,RoundingMode.HALF_UP)));
        expectedPricePlanToCost.add(new AbstractMap.SimpleEntry<>(PRICE_PLAN_1_ID, BigDecimal.valueOf(140.0).setScale(4,RoundingMode.HALF_UP)));

        assertThat(controller.recommendCheapestPricePlans(SMART_METER_ID, 5).getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void givenNoMatchingMeterIdShouldReturnNotFound() {
        assertThat(controller.calculatedCostForEachPricePlan("not-found").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldReturnRightPriceWhenCalculateCostForMeterReadingsForWeek(){
        int dayOfWeek = Instant.now().atZone(ZoneId.systemDefault()).getDayOfWeek().getValue();
        LocalDateTime currenTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Instant todayStart = currenTime.toInstant(ZoneOffset.UTC);
        ElectricityReading electricityReading = new ElectricityReading(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek-1)).minusSeconds(1),BigDecimal.valueOf(25));
        ElectricityReading otherReading = new ElectricityReading(todayStart.minusMillis(TimeUnit.DAYS.toMillis(dayOfWeek+6)).plusSeconds(1),BigDecimal.valueOf(15));

        meterReadingService.storeReadings(SMART_METER_ID,Arrays.asList(electricityReading,otherReading));
        System.out.println(controller.getPriceOfLastWeekUsage(SMART_METER_ID));
        Map<String,BigDecimal> expectedWeekCost = new HashMap<>();
        expectedWeekCost.put("2022-01-03",BigDecimal.valueOf(25));
        expectedWeekCost.put("2021-12-27",BigDecimal.valueOf(15));
        expectedWeekCost.put(SMART_METER_ID,BigDecimal.valueOf(9600.0000).setScale(4,RoundingMode.HALF_UP));

        assertThat(controller.getPriceOfLastWeekUsage(SMART_METER_ID).getBody()).isEqualTo(expectedWeekCost);

    }

    @Test
    public void shouldReturnRightCostWhenGetDayElectricityUsage(){

        ElectricityReading electricityReading1 = new ElectricityReading(Instant.now(),BigDecimal.valueOf(20));
        DayCostElectricity dayCostElectricity = new DayCostElectricity(PRICE_PLAN_1_ID,electricityReading1);
        ElectricityReading electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25));
        ElectricityReading otherReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(15));

        meterReadingService.storeReadings(SMART_METER_ID,Arrays.asList(electricityReading,otherReading));


        Map<String,BigDecimal> expectedWeekCost = new HashMap<>();

        expectedWeekCost.put(SMART_METER_ID,BigDecimal.valueOf(4800.0000).setScale(4,RoundingMode.HALF_UP));

        assertThat(controller.getDayCostElectricityUsage(SMART_METER_ID,dayCostElectricity).getBody()).isEqualTo(expectedWeekCost);

    }
    @Test
    public void shouldReturnRightRankWhenGetCostForDayOfWeekRank(){
        Instant instant = Instant.parse("2021-11-27T03:34:58.118479Z");
        ElectricityReading electricityReading1 = new ElectricityReading(instant,BigDecimal.valueOf(20));
        DayCostElectricity dayCostElectricity = new DayCostElectricity(PRICE_PLAN_1_ID,electricityReading1);
        ElectricityReading firstElectricityReading = new ElectricityReading(instant.minusSeconds(3600), BigDecimal.valueOf(25));
        ElectricityReading otherReading = new ElectricityReading(instant.minusSeconds(1800), BigDecimal.valueOf(15));
        ElectricityReading secondElectricityReading = new ElectricityReading(instant.minusMillis(TimeUnit.DAYS.toMillis(1)),BigDecimal.valueOf(15));
        ElectricityReading secondOtherReading = new ElectricityReading(instant.minusMillis(TimeUnit.DAYS.toMillis(1)).minusSeconds(3600),BigDecimal.valueOf(5));
        meterReadingService.storeReadings(SMART_METER_ID,Arrays.asList(firstElectricityReading,otherReading,secondElectricityReading,secondOtherReading));

        Map<String,BigDecimal> expectedWeekCost = new HashMap<>();

        expectedWeekCost.put("FRIDAY",BigDecimal.valueOf(2400.0000).setScale(4,RoundingMode.HALF_UP));
        expectedWeekCost.put("SATURDAY",BigDecimal.valueOf(4800.0000).setScale(4,RoundingMode.HALF_UP));

        assertThat(controller.getCurrentPricePlanRankForDifferentDaysOfWeek(SMART_METER_ID,dayCostElectricity).getBody()).isEqualTo(expectedWeekCost);

    }
    @Test
    public void shouldReturnRightPricePlaneRankWhenGetPricePlanRank(){
        Instant instant = Instant.parse("2021-11-27T03:34:58.118479Z");
        ElectricityReading electricityReading1 = new ElectricityReading(instant,BigDecimal.valueOf(20));
        DayCostElectricity dayCostElectricity = new DayCostElectricity(PRICE_PLAN_1_ID,electricityReading1);
        ElectricityReading firstElectricityReading = new ElectricityReading(instant.minusSeconds(3600), BigDecimal.valueOf(25));
        ElectricityReading otherReading = new ElectricityReading(instant.minusSeconds(1800), BigDecimal.valueOf(15));
        ElectricityReading secondElectricityReading = new ElectricityReading(instant.plusMillis(TimeUnit.DAYS.toMillis(1)),BigDecimal.valueOf(15));
        ElectricityReading secondOtherReading = new ElectricityReading(instant.plusMillis(TimeUnit.DAYS.toMillis(1)).minusSeconds(3600),BigDecimal.valueOf(5));
        meterReadingService.storeReadings(SMART_METER_ID,Arrays.asList(firstElectricityReading,otherReading,secondElectricityReading,secondOtherReading));
        List<PricePlanCost> pricePlanCosts1 = new ArrayList<>();
        pricePlanCosts1.add(new PricePlanCost("best-supplier",BigDecimal.valueOf(480.0000).setScale(4,RoundingMode.HALF_UP)));
        pricePlanCosts1.add(new PricePlanCost("second-best-supplier",BigDecimal.valueOf(960.0000).setScale(4,RoundingMode.HALF_UP)));
        List<PricePlanCost> pricePlanCosts = new ArrayList<>();
        pricePlanCosts.add(new PricePlanCost("best-supplier",BigDecimal.valueOf(240.0000).setScale(4,RoundingMode.HALF_UP)));
        pricePlanCosts.add(new PricePlanCost("second-best-supplier",BigDecimal.valueOf(480.0000).setScale(4,RoundingMode.HALF_UP)));
        Map<String,List<PricePlanCost>> expectedWeekCost = new HashMap<>();
        int limit =2;
        expectedWeekCost.put("SATURDAY",pricePlanCosts1);
        expectedWeekCost.put("SUNDAY",pricePlanCosts);

        assertThat(controller.getPricePlanRank(SMART_METER_ID,dayCostElectricity,limit).getBody()).isEqualTo(expectedWeekCost);

    }

}
