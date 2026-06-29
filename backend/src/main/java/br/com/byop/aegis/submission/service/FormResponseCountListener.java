package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.form.api.FormResponseReadModelService;
import br.com.byop.aegis.submission.api.SubmissionReceivedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FormResponseCountListener {

    private final FormResponseReadModelService readModelService;

    public FormResponseCountListener(FormResponseReadModelService readModelService) {
        this.readModelService = readModelService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionReceived(SubmissionReceivedEvent event) {
        readModelService.registerResponse(event.formId(), event.receivedAt());
    }
}
