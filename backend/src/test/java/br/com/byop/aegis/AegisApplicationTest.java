package br.com.byop.aegis;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class AegisApplicationTest {

    @Test
    void shouldRunApplicationMain() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            AegisApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(AegisApplication.class, args));
        }
    }
}