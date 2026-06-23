package br.com.byop.aegis.shared.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageBootstrapTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldCreateLocalStorageDirectory() {
        LocalStorageBootstrap bootstrap = new LocalStorageBootstrap(tempDir.toString());

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(tempDir.resolve("aegis").resolve("pms"))
                .exists()
                .isDirectory()
                .isWritable();
    }

    @Test
    void shouldThrowWhenStoragePathCannotBeCreated() throws Exception {
        Path fileInsteadOfDirectory = tempDir.resolve("file");
        Files.writeString(fileInsteadOfDirectory, "not-a-directory");

        LocalStorageBootstrap bootstrap = new LocalStorageBootstrap(fileInsteadOfDirectory.toString());

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falha ao preparar storage local em:");
    }

    @Test
    void shouldThrowWhenStorageDirectoryIsNotWritable() throws Exception {
        Path storageRoot = tempDir.resolve("aegis").resolve("pms");
        Files.createDirectories(storageRoot);

        try {
            Files.setPosixFilePermissions(storageRoot, PosixFilePermissions.fromString("r-xr-xr-x"));

            LocalStorageBootstrap bootstrap = new LocalStorageBootstrap(tempDir.toString());

            assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Storage local sem permissão de escrita em:");
        } finally {
            Files.setPosixFilePermissions(storageRoot, PosixFilePermissions.fromString("rwxrwxrwx"));
        }
    }
}