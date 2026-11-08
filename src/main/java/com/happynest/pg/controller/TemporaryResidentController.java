package com.happynest.pg.controller;

import com.happynest.pg.dto.CreateTemporaryResidentRequest;
import com.happynest.pg.dto.ExtendStayRequest;
import com.happynest.pg.dto.MarkPaidRequest;
import com.happynest.pg.dto.TemporaryResidentDto;
import com.happynest.pg.service.TemporaryResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/temporary-residents")
@RequiredArgsConstructor
public class TemporaryResidentController {

    private final TemporaryResidentService temporaryResidentService;

    @GetMapping
    public List<TemporaryResidentDto> list() {
        return temporaryResidentService.list();
    }

    @GetMapping("/{id}")
    public TemporaryResidentDto get(@PathVariable String id) {
        return temporaryResidentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporaryResidentDto create(@Valid @RequestBody CreateTemporaryResidentRequest request) {
        return temporaryResidentService.create(request);
    }

    @PutMapping("/{id}/extend")
    public TemporaryResidentDto extend(
            @PathVariable String id,
            @Valid @RequestBody ExtendStayRequest request) {
        return temporaryResidentService.extend(id, request);
    }

    @PutMapping("/{id}/mark-paid")
    public TemporaryResidentDto markPaid(
            @PathVariable String id,
            @Valid @RequestBody MarkPaidRequest request) {
        return temporaryResidentService.markPaid(id, request);
    }

    @DeleteMapping("/{id}")
    public TemporaryResidentService.MapId delete(@PathVariable String id) {
        return temporaryResidentService.delete(id);
    }
}
