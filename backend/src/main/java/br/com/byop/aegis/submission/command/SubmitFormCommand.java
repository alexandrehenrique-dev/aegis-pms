package br.com.byop.aegis.submission.command;

import java.util.Map;

public record SubmitFormCommand(
        String name,
        String email,
        String source,
        String ownerSubject,
        Integer score,
        Map<String, Object> answers
) {
}
