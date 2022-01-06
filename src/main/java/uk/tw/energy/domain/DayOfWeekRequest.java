package uk.tw.energy.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayOfWeekRequest {
    int dayOfWeek;
    int numberOfLowest;
}
