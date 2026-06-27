package br.com.byop.aegis.analytics.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsConfigTest {

    @Test
    void shouldCreateUtcClock() {
        Clock clock = new AnalyticsConfig().analyticsClock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
