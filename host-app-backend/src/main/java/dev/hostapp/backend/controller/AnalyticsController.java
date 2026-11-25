package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.AnalyticsDtos.*;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@SecurityRequirement(name = "bearer")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestAttribute("user") User user) { return analytics.summary(user); }
}
