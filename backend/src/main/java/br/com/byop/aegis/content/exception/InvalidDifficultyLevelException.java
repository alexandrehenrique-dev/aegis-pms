package br.com.byop.aegis.content.exception;

public class InvalidDifficultyLevelException extends RuntimeException {

    public InvalidDifficultyLevelException(String value) {
        super("Invalid difficulty level: " + value);
    }
}
