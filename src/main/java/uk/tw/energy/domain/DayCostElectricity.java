package uk.tw.energy.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayCostElectricity {

    private String pricePlanId;
    private ElectricityReading electricityReading;


}

