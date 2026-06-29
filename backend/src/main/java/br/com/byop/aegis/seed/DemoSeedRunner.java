package br.com.byop.aegis.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Executa o seed de demonstracao somente no profile local.
 */
@Component
@Profile("local")
public class DemoSeedRunner implements ApplicationRunner {

    private final DemoSeedService demoSeedService;

    public DemoSeedRunner(DemoSeedService demoSeedService) {
        this.demoSeedService = demoSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        demoSeedService.seed();
    }
}
