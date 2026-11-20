package dev.hostapp.backend.service.forecast;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestRnnTest {
    @Test
    void learnsConstantSeriesAndUsesOnlyTheTrainingPrefix() {
        double[] days = new double[60];
        Arrays.fill(days, 12);
        RequestRnn original = RequestRnn.train(days, 45);
        Arrays.fill(days, 45, 60, 1000);
        RequestRnn changedHoldout = RequestRnn.train(days, 45);

        assertEquals(original.next(days, 45), changedHoldout.next(days, 45));
        assertEquals(12, original.twoDaysAhead(days, 45), 0.1);
        assertThrows(IllegalArgumentException.class, () -> RequestRnn.train(days, 14));
    }

    @Test
    void usesTheOrderOfDays() {
        double[] days = new double[105];
        double[] week = {15, 15, 11, 11, 7, 2, 2};
        for (int day = 0; day < days.length; day++) {
            days[day] = week[day % week.length];
        }
        RequestRnn model = RequestRnn.train(days, 80);
        // Both seven-day tails end with a 2, but follow different days of the week.
        assertTrue(model.next(days, 83) < model.next(days, 84));
    }
}
