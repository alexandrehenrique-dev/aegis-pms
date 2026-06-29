package br.com.byop.aegis.submission.controller;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.submission.command.SubmitFormCommand;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.service.PublicSubmissionRateLimiter;
import br.com.byop.aegis.submission.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequireModule(ModuleKey.FORMS)
public class PublicSubmissionController {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final SubmissionService submissionService;
    private final PublicSubmissionRateLimiter rateLimiter;

    public PublicSubmissionController(SubmissionService submissionService, PublicSubmissionRateLimiter rateLimiter) {
        this.submissionService = submissionService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/v1/products/{productId}/forms/{formId}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDetail submit(@PathVariable("productId") UUID productId,
                                   @PathVariable("formId") UUID formId,
                                   @RequestBody SubmitFormCommand request,
                                   HttpServletRequest servletRequest) {
        rateLimiter.assertAllowed(clientIp(servletRequest), formId);
        return submissionService.submit(productId, formId, request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
