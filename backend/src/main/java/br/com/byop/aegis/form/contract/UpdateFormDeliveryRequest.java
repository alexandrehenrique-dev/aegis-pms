package br.com.byop.aegis.form.contract;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UpdateFormDeliveryRequest(
        @NotNull List<Map<String, Object>> channels
) {
}
