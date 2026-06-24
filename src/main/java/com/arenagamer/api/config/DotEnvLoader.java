package com.arenagamer.api.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Carrega variáveis do arquivo {@code .env} na raiz do projeto quando a app
 * é iniciada pela IDE (IntelliJ/Eclipse), que não lê {@code .env} automaticamente.
 * Variáveis já definidas no ambiente do SO não são sobrescritas.
 */
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void loadIfPresent() {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Falha silenciosa: application.yml continua com defaults.
        }
    }
}
