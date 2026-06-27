package br.com.byop.aegis.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AnalyticsConfig {

    @Bean
    Clock analyticsClock() {
        return Clock.systemUTC();
    }
}
