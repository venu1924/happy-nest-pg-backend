package com.happynest.pg.controller;

import com.happynest.pg.dto.*;
import com.happynest.pg.service.ResidentService;
import com.happynest.pg.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;
    private final TransferService transferService;

    @GetMapping
    public List<ResidentDto> list() {
        return residentService.getResidents();
    }

    @GetMapping("/past")
    public List<PastResidentDto> past() {
        return residentService.getPastResidents();
    }

    @GetMapping("/{id}")
    public ResidentDto get(@PathVariable String id) {
        return residentService.getResident(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResidentDto create(@Valid @RequestBody CreateResidentRequest request) {
        return residentService.create(request);
    }

    @PutMapping("/{id}")
    public ResidentDto update(@PathVariable String id, @Valid @RequestBody UpdateResidentRequest request) {
        return residentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResidentService.MapId delete(
            @PathVariable String id,
            @RequestBody(required = false) DeleteResidentRequest request) {
        return residentService.delete(id, request != null ? request : new DeleteResidentRequest());
    }

    @PostMapping("/{id}/transfer")
    public TransferResultDto transfer(
            @PathVariable String id,
            @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(id, request);
    }
}
