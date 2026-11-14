package io.github.victorleasilva.capo.lab.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import io.github.victorleasilva.capo.support.SeedApplication;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.DefaultResourceLoader;

/** Um descritor fora do JSON Schema impede a aplicação de subir, e o log cita o arquivo e o campo (AC-002.4). */
@Tag("RF-002")
@ExtendWith(OutputCaptureExtension.class)
class DescriptorStartupTest {

    private static final String WITHOUT_VARIANTS = """
            {
              "schemaVersion": 1,
              "id": "sem-versoes",
              "title": "Sem versões",
              "summary": "Falta o campo variants.",
              "studyPage": "experimentos/sem-versoes.mdx",
              "fairness": "same-result",
              "parameters": [],
              "measurements": [{"id": "durationMs", "label": "Duração", "unit": "ms"}],
              "verificationQuestions": ["Um?", "Dois?", "Três?"]
            }
            """;

    @TempDir
    Path extraClasspath;

    @Test
    void invalidDescriptorPreventsStartup(CapturedOutput output) throws IOException {
        Path dir = Files.createDirectories(extraClasspath.resolve("lab/experiments"));
        Files.writeString(dir.resolve("sem-versoes.json"), WITHOUT_VARIANTS);
        String url = PostgresTestDatabase.createDatabase("descriptor");

        // O classpath da aplicação mais o diretório temporário: o classpath*: dos descritores enxerga os dois.
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[] {extraClasspath.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            assertThatThrownBy(() -> SeedApplication.start(new DefaultResourceLoader(classLoader), url, "10000"))
                    .hasStackTraceContaining("sem-versoes.json");
        }

        assertThat(output.getAll()).contains("sem-versoes.json").contains("/variants");
    }
}
