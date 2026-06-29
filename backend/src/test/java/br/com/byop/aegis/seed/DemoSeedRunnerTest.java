package br.com.byop.aegis.seed;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DemoSeedRunnerTest {

    @Test
    void shouldDelegateToSeedService() {
        DemoSeedService service = mock(DemoSeedService.class);
        DemoSeedRunner runner = new DemoSeedRunner(service);

        runner.run(new DefaultApplicationArguments());

        verify(service).seed();
    }

    @Test
    void shouldRunOnlyWithLocalProfile() {
        Profile profile = DemoSeedRunner.class.getAnnotation(Profile.class);

        assertThat(profile.value()).containsExactly("local");
    }
}
