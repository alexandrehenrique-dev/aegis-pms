package br.com.byop.aegis.submission.controller;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.dto.SubmissionSummary;
import br.com.byop.aegis.submission.service.SubmissionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequireModule(ModuleKey.FORMS)
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public SubmissionController(SubmissionService submissionService, ProductAccessPort productAccessPort,
                                AuthenticatedUserProvider authenticatedUserProvider) {
        this.submissionService = submissionService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/forms/{formId}/submissions")
    public List<SubmissionSummary> listSubmissions(@PathVariable("productId") UUID productId,
                                                   @PathVariable("formId") UUID formId,
                                                   Authentication authentication) {
        assertProductAccess(authentication, productId);
        return submissionService.listSubmissions(productId, formId);
    }

    @GetMapping("/api/v1/products/{productId}/forms/submissions")
    public List<SubmissionSummary> listProductSubmissions(@PathVariable("productId") UUID productId,
                                                          Authentication authentication) {
        assertProductAccess(authentication, productId);
        return submissionService.listProductSubmissions(productId);
    }

    @GetMapping("/api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}")
    public SubmissionDetail getSubmission(@PathVariable("productId") UUID productId,
                                          @PathVariable("formId") UUID formId,
                                          @PathVariable("submissionId") UUID submissionId,
                                          Authentication authentication) {
        assertProductAccess(authentication, productId);
        return submissionService.getSubmission(productId, formId, submissionId);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}
