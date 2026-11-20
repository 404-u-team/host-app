package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.AnalyticsDtos.*;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.AnalyticsService;
import dev.hostapp.backend.service.forecast.ForecastService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@SecurityRequirement(name = "bearer")
public class AnalyticsController {
    private final AnalyticsService analytics;
    private final ForecastService forecasts;

    public AnalyticsController(AnalyticsService analytics, ForecastService forecasts) {
        this.analytics = analytics;
        this.forecasts = forecasts;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestAttribute("user") User user) { return analytics.summary(user); }

    @GetMapping("/request-forecast")
    public Forecast forecast(@RequestParam(defaultValue = "REAL") Source source) {
        return forecasts.forecast(source);
    }

    @PostMapping("/request-forecast/demo-data")
    public DemoSeed seed(@RequestAttribute("user") User user) { return forecasts.seedDemo(user); }
}
