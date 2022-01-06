package uk.tw.energy.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeparateUsages {
    private Map<String, BigDecimal> electricityUsages;
    private Map<String,BigDecimal> gasUsages;
    private BigDecimal electricityUsagesOfWeek;
    private BigDecimal gasUsagesOfWeek;

}
