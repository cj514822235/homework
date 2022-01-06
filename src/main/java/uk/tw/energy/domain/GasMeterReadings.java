package uk.tw.energy.domain;

import java.util.List;

public class GasMeterReadings {

    private List<GasReading> gasReadings;
    private String smartMeterId;

    public GasMeterReadings() { }

    public GasMeterReadings(String smartMeterId, List<GasReading> gasReadings) {
        this.smartMeterId = smartMeterId;
        this.gasReadings = gasReadings;
    }

    public List<GasReading> getGasReadings() {
        return gasReadings;
    }

    public String getSmartMeterId() {
        return smartMeterId;
    }

}
