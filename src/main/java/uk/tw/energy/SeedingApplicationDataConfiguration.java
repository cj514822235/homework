package uk.tw.energy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.GasPricePlan;
import uk.tw.energy.domain.GasReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.generator.ElectricityReadingsGenerator;
import uk.tw.energy.generator.GasReadingGenerator;

import java.math.BigDecimal;

import java.time.DayOfWeek;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Configuration
public class SeedingApplicationDataConfiguration {

    private static final String MOST_EVIL_PRICE_PLAN_ID = "price-plan-0";
    private static final String RENEWABLES_PRICE_PLAN_ID = "price-plan-1";
    private static final String STANDARD_PRICE_PLAN_ID = "price-plan-2";

    private static final String GAS_MOST_EVIL_PRICE_PLAN_ID = "gas-price-plan-0";
    private static final String GAS_RENEWABLES_PRICE_PLAN_ID = "gas-price-plan-1";
    private static final String GAS_STANDARD_PRICE_PLAN_ID = "gas-price-plan-2";

    @Bean
    public List<PricePlan> pricePlans() {
        final List<PricePlan> pricePlans = new ArrayList<>();

        List<PricePlan.PeakTimeMultiplier> peakTimeMultipliers = new ArrayList<>();
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.SUNDAY,BigDecimal.valueOf(0.8)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.MONDAY,BigDecimal.valueOf(0.3)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.TUESDAY,BigDecimal.valueOf(0.2)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.WEDNESDAY,BigDecimal.valueOf(0.2)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.THURSDAY,BigDecimal.valueOf(0.4)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.FRIDAY,BigDecimal.valueOf(0.2)));
        peakTimeMultipliers.add(new PricePlan.PeakTimeMultiplier(DayOfWeek.SATURDAY,BigDecimal.valueOf(1)));

        pricePlans.add(new PricePlan(MOST_EVIL_PRICE_PLAN_ID, "Dr Evil's Dark Energy", BigDecimal.TEN, peakTimeMultipliers));
        pricePlans.add(new PricePlan(RENEWABLES_PRICE_PLAN_ID, "The Green Eco", BigDecimal.valueOf(2), peakTimeMultipliers));
        pricePlans.add(new PricePlan(STANDARD_PRICE_PLAN_ID, "Power for Everyone", BigDecimal.ONE,emptyList()));
        return pricePlans;
    }
    @Bean
    public List<GasPricePlan> gasPricePlans(){
        final List<GasPricePlan> gasPricePlans = new ArrayList<>();

        gasPricePlans.add(new GasPricePlan(GAS_MOST_EVIL_PRICE_PLAN_ID,"Dr Evil's Dark Gas",BigDecimal.TEN,emptyList()));
        gasPricePlans.add(new GasPricePlan(GAS_RENEWABLES_PRICE_PLAN_ID,"The Green Gas",BigDecimal.valueOf(2),emptyList()));
        gasPricePlans.add(new GasPricePlan(GAS_STANDARD_PRICE_PLAN_ID,"Power for Gas",BigDecimal.ONE,emptyList()));
        return gasPricePlans;
    }


    @Bean
    public Map<String, List<ElectricityReading>> perMeterElectricityReadings() {
        final Map<String, List<ElectricityReading>> readings = new HashMap<>();
        final ElectricityReadingsGenerator electricityReadingsGenerator = new ElectricityReadingsGenerator();
        smartMeterToPricePlanAccounts()
                .keySet()
                .forEach(smartMeterId -> readings.put(smartMeterId, electricityReadingsGenerator.generate(720)));
        return readings;
    }

    @Bean
    public Map<String,List<GasReading>> perGasReadings(){
        final  Map<String,List<GasReading>> gasReadings = new HashMap<>();
        final GasReadingGenerator generator = new GasReadingGenerator();
        gasToPricePlanAccounts().keySet().forEach(smartMeterId->gasReadings.put(smartMeterId,generator.generate(720)));
        return gasReadings;
    }

    @Bean
    public Map<String, String> smartMeterToPricePlanAccounts() {
        final Map<String, String> smartMeterToPricePlanAccounts = new HashMap<>();
        smartMeterToPricePlanAccounts.put("smart-meter-0", MOST_EVIL_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-1", RENEWABLES_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-2", MOST_EVIL_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-3", STANDARD_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-4", RENEWABLES_PRICE_PLAN_ID);
        return smartMeterToPricePlanAccounts;
    }
    @Bean
    public Map<String,String> gasToPricePlanAccounts(){
        final Map<String,String> gasToPricePlanAccounts = new HashMap<>();
        gasToPricePlanAccounts.put("smart-meter-0",GAS_MOST_EVIL_PRICE_PLAN_ID);
        gasToPricePlanAccounts.put("smart-meter-1",GAS_MOST_EVIL_PRICE_PLAN_ID);
        gasToPricePlanAccounts.put("smart-meter-2",GAS_MOST_EVIL_PRICE_PLAN_ID);
        return gasToPricePlanAccounts;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }


}
