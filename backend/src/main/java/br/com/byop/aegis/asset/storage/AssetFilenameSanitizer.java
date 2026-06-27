package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.exception.InvalidAssetFilenameException;

import java.nio.file.Path;
import java.text.Normalizer;

/**
 * Sanitiza o nome original de um arquivo antes de vira-lo parte de um
 * {@code storageKey} — remove componentes de caminho ({@code ..}, {@code /},
 * {@code \}), normaliza acentuacao/espacos e rejeita (lancando
 * {@link InvalidAssetFilenameException}) quando o nome fica vazio apos a
 * sanitizacao. Compartilhado por {@link LocalStorageProvider} e
 * {@link S3StorageProvider} — ambos seguem exatamente a mesma regra.
 */
final class AssetFilenameSanitizer {

    private AssetFilenameSanitizer() {
    }

    static String sanitize(String originalFilename) {
        String fileNameOnly = extractFileName(originalFilename);
        int dotIndex = fileNameOnly.lastIndexOf('.');
        String base = dotIndex > 0 ? fileNameOnly.substring(0, dotIndex) : fileNameOnly;
        String extension = dotIndex > 0 ? fileNameOnly.substring(dotIndex + 1) : "";

        String normalizedBase = normalize(base);
        if (normalizedBase.isBlank()) {
            throw new InvalidAssetFilenameException(originalFilename);
        }

        String normalizedExtension = sanitizeExtension(extension);
        return normalizedExtension.isBlank() ? normalizedBase : normalizedBase + "." + normalizedExtension;
    }

    private static String extractFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidAssetFilenameException(originalFilename);
        }
        Path fileName = Path.of(originalFilename).getFileName();
        if (fileName == null) {
            throw new InvalidAssetFilenameException(originalFilename);
        }
        return fileName.toString();
    }

    /**
     * Normaliza passo a passo, sem regex (java:S5850/S5852 — evita qualquer
     * risco de backtracking catastrofico em entrada adversarial do nome de
     * arquivo): decompoe acentos via {@link Normalizer}, descarta marcas de
     * combinacao, mantem apenas {@code [a-z0-9]} (minusculo) e colapsa
     * qualquer outro caractere numa unica barra, sem barra nas pontas.
     */
    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder result = new StringBuilder(decomposed.length());
        boolean pendingSeparator = false;
        for (int i = 0; i < decomposed.length(); i++) {
            char current = decomposed.charAt(i);
            if (Character.getType(current) == Character.NON_SPACING_MARK) {
                continue;
            }
            char lower = Character.toLowerCase(current);
            if (isAsciiLetterOrDigit(lower)) {
                result.append(lower);
                pendingSeparator = false;
            } else if (!result.isEmpty() && !pendingSeparator) {
                result.append('-');
                pendingSeparator = true;
            }
        }
        if (pendingSeparator) {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    private static String sanitizeExtension(String extension) {
        StringBuilder result = new StringBuilder(extension.length());
        for (int i = 0; i < extension.length(); i++) {
            char current = Character.toLowerCase(extension.charAt(i));
            if (isAsciiLetterOrDigit(current)) {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static boolean isAsciiLetterOrDigit(char value) {
        return (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9');
    }
}
