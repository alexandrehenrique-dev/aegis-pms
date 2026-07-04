package br.com.byop.aegis.shared.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class LocalStorageBootstrap implements ApplicationRunner {

    private final String localStoragePath;

    public LocalStorageBootstrap(
            @Value("${aegis.storage.local-path:${AEGIS_STORAGE_LOCAL_PATH:./data/assets}}")
            String localStoragePath
    ) {
        this.localStoragePath = localStoragePath;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path storageRoot = Path.of(localStoragePath, "aegis", "pms").toAbsolutePath().normalize();

        try {
            Files.createDirectories(storageRoot);

            if (!Files.isWritable(storageRoot)) {
                throw new IllegalStateException(
                        "Storage local sem permissão de escrita em: " + storageRoot
                );
            }

            log.info("Storage local pronto em: {}", storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Falha ao preparar storage local em: " + storageRoot,
                    exception
            );
        }
    }
}