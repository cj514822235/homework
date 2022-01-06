package uk.tw.energy.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PricePlanCost {
    private String pricePlan;
    private BigDecimal Usage;
}
