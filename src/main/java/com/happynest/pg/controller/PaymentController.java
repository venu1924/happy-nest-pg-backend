package com.happynest.pg.controller;

import com.happynest.pg.dto.MarkPaidRequest;
import com.happynest.pg.dto.PaymentDto;
import com.happynest.pg.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentDto> list(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return paymentService.getPayments(month, year);
    }

    @GetMapping("/{id}")
    public PaymentDto get(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @PutMapping("/{id}/mark-paid")
    public PaymentDto markPaid(@PathVariable String id, @Valid @RequestBody MarkPaidRequest request) {
        return paymentService.markPaid(id, request);
    }
}
