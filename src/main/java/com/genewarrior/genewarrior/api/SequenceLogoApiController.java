package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.LogoRequest;
import com.genewarrior.genewarrior.api.dto.SequenceLogoDto;
import com.genewarrior.genewarrior.service.SequenceLogoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class SequenceLogoApiController {
    private final SequenceLogoService service;

    public SequenceLogoApiController(SequenceLogoService service) {
        this.service = service;
    }

    @PostMapping("/api/logos")
    SequenceLogoDto calculate(@Valid @RequestBody LogoRequest request) {
        return service.calculate(request);
    }
}
