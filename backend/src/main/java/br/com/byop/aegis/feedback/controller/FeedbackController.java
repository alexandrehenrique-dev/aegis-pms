package br.com.byop.aegis.feedback.controller;

import br.com.byop.aegis.feedback.contract.CreateFeedbackRequest;
import br.com.byop.aegis.feedback.contract.UpdateFeedbackStatusRequest;
import br.com.byop.aegis.feedback.dto.FeedbackSummary;
import br.com.byop.aegis.feedback.service.FeedbackService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public FeedbackController(FeedbackService feedbackService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.feedbackService = feedbackService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/api/v1/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackSummary createFeedback(@Valid @RequestBody CreateFeedbackRequest request,
                                          Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return feedbackService.create(caller, request);
    }

    @GetMapping("/api/v1/feedback")
    public List<FeedbackSummary> listFeedback(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return feedbackService.listAll(caller);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/feedback")
    public List<FeedbackSummary> listTenantFeedback(@PathVariable("tenantId") UUID tenantId,
                                                    Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return feedbackService.listByTenant(caller, tenantId);
    }

    @PutMapping("/api/v1/feedback/{feedbackId}/status")
    public FeedbackSummary updateFeedbackStatus(@PathVariable("feedbackId") String feedbackId,
                                                @Valid @RequestBody UpdateFeedbackStatusRequest request,
                                                Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return feedbackService.updateStatus(caller, feedbackId, request.status());
    }
}
