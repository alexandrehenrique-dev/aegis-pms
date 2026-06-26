package br.com.byop.aegis.system.controller;

import br.com.byop.aegis.system.dto.SystemStatusResponse;
import br.com.byop.aegis.system.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/api/v1/system/status")
    public SystemStatusResponse systemStatus() {
        return systemStatusService.currentStatus();
    }
}
