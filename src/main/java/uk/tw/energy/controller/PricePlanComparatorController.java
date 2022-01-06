package uk.tw.energy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.domain.DayCostElectricity;
import uk.tw.energy.domain.DayOfWeekRequest;
import uk.tw.energy.domain.PricePlanCost;
import uk.tw.energy.exception.BadRequest;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    public final static String PRICE_PLAN_ID_KEY = "pricePlanId";
    public final static String PRICE_PLAN_COMPARISONS_KEY = "pricePlanComparisons";
    private final PricePlanService pricePlanService;
    private final AccountService accountService;

    public PricePlanComparatorController(PricePlanService pricePlanService, AccountService accountService) {
        this.pricePlanService = pricePlanService;
        this.accountService = accountService;
    }

    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> calculatedCostForEachPricePlan(@PathVariable String smartMeterId) {
        String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> pricePlanComparisons = new HashMap<>();
        pricePlanComparisons.put(PRICE_PLAN_ID_KEY, pricePlanId);
        pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptionsForPricePlans.get());

        return consumptionsForPricePlans.isPresent()
                ? ResponseEntity.ok(pricePlanComparisons)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendCheapestPricePlans(@PathVariable String smartMeterId,
                                                                                           @RequestParam(value = "limit", required = false) Integer limit) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Map.Entry<String, BigDecimal>> recommendations = new ArrayList<>(consumptionsForPricePlans.get().entrySet());
        recommendations.sort(Comparator.comparing(Map.Entry::getValue));

        if (limit != null && limit < recommendations.size()) {
            recommendations = recommendations.subList(0, limit);
        }

        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/last-week/{smartMeterId}")
    public ResponseEntity<Map<String,BigDecimal>>getPriceOfLastWeekUsage(@PathVariable String smartMeterId){

        String pricePlanId = Optional.ofNullable(accountService.getPricePlanIdForSmartMeterId(smartMeterId)).orElseThrow(()->
                new BadRequest("PricePlanId Not Found"));
        Optional<Map<String, BigDecimal>> lastWeekCostPrice =
                pricePlanService.getConsumptionCostOfElectricityReadingsForLastWeek(smartMeterId,pricePlanId);
        return ResponseEntity.ok(lastWeekCostPrice.get());

    }

    @GetMapping("/day-electricity/{smartMeterId}")
    public ResponseEntity<Map<String,BigDecimal>>getDayCostElectricityUsage(@PathVariable String smartMeterId, @RequestBody DayCostElectricity dayCostElectricity){
        Map<String,BigDecimal> dayCostElectricityUsage = pricePlanService.getDayCostElectricityUsage(smartMeterId,dayCostElectricity);

        return ResponseEntity.ok(dayCostElectricityUsage);
    }
    @GetMapping("/cost-rank/{smartMeterId}")
    public ResponseEntity<Map<String,BigDecimal>>getCurrentPricePlanRankForDifferentDaysOfWeek(@PathVariable String smartMeterId,
                                                                                                @RequestBody DayCostElectricity dayCostElectricity){
        Map<String,BigDecimal> dayOfWeekRank = pricePlanService.getCostForDayOfWeekRank(smartMeterId,dayCostElectricity);

        return ResponseEntity.ok(dayOfWeekRank);
    }

    @GetMapping("/rank/{smartMeterId}")
    public ResponseEntity<Map<String,List<PricePlanCost>>>getPricePlanRank(@PathVariable String smartMeterId, @RequestBody DayCostElectricity dayCostElectricity,
                                                                            @RequestParam(value = "limit", required = false) Integer limit){
        Map<String,List<PricePlanCost>>pricePlanRankMap = pricePlanService.getPricePlanRank(smartMeterId,dayCostElectricity,limit);

        return ResponseEntity.ok(pricePlanRankMap);

    }


}
