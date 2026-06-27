package br.com.byop.aegis.asset.dto;

import java.util.Arrays;

public record AssetFileContent(byte[] content, String contentType, String filename) {

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssetFileContent(byte[] otherContent, String otherContentType, String otherFilename))) {
            return false;
        }
        return Arrays.equals(content, otherContent)
                && contentType.equals(otherContentType)
                && filename.equals(otherFilename);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + contentType.hashCode();
        result = 31 * result + filename.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AssetFileContent[content=" + Arrays.toString(content)
                + ", contentType=" + contentType
                + ", filename=" + filename + "]";
    }
}
