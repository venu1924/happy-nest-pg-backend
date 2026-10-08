package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Payment;
import com.happynest.pg.domain.entity.Resident;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.dto.MarkPaidRequest;
import com.happynest.pg.dto.PaymentDto;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.PaymentRepository;
import com.happynest.pg.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ResidentRepository residentRepository;

    @Transactional(readOnly = true)
    public List<PaymentDto> getPayments(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        Map<String, Resident> residents = residentRepository.findAll().stream()
                .collect(Collectors.toMap(Resident::getId, Function.identity()));

        return paymentRepository.findByMonthAndYear(m, y).stream()
                .map(p -> toDto(p, residents.get(p.getResidentId())))
                .sorted(paymentComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found.", HttpStatus.NOT_FOUND));
        Resident resident = residentRepository.findById(payment.getResidentId()).orElse(null);
        return toDto(payment, resident);
    }

    @Transactional
    public PaymentDto markPaid(String id, MarkPaidRequest req) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found.", HttpStatus.NOT_FOUND));
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ApiException("Payment is already marked as paid.", HttpStatus.BAD_REQUEST);
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setPaidDate(req.getPaidDate() != null ? req.getPaidDate() : LocalDate.now());
        payment.setReceivedBy(req.getReceivedBy() != null && !req.getReceivedBy().isBlank()
                ? req.getReceivedBy()
                : "Admin");
        paymentRepository.save(payment);
        Resident resident = residentRepository.findById(payment.getResidentId()).orElse(null);
        return toDto(payment, resident);
    }

    private PaymentDto toDto(Payment p, Resident r) {
        return PaymentDto.builder()
                .id(p.getId())
                .residentId(p.getResidentId())
                .month(p.getMonth())
                .year(p.getYear())
                .amount(p.getAmount())
                .status(p.getStatus())
                .paymentMethod(p.getPaymentMethod())
                .paidDate(p.getPaidDate())
                .receivedBy(p.getReceivedBy())
                .residentName(r != null ? r.getName() : "Unknown")
                .roomNumber(r != null ? r.getRoomNumber() : null)
                .phone(r != null ? r.getPhone() : null)
                .build();
    }

    private Comparator<PaymentDto> paymentComparator() {
        return (a, b) -> {
            if (a.getPaidDate() != null && b.getPaidDate() != null) {
                return b.getPaidDate().compareTo(a.getPaidDate());
            }
            if (a.getPaidDate() != null) return -1;
            if (b.getPaidDate() != null) return 1;
            String na = a.getResidentName() != null ? a.getResidentName() : "";
            String nb = b.getResidentName() != null ? b.getResidentName() : "";
            return na.compareToIgnoreCase(nb);
        };
    }
}
