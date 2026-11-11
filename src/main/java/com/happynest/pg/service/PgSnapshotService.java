package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.PastResident;
import com.happynest.pg.domain.entity.Payment;
import com.happynest.pg.domain.entity.Resident;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.entity.TemporaryResident;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import com.happynest.pg.dto.TemporaryResidentDto;
import com.happynest.pg.domain.entity.Transfer;
import com.happynest.pg.repository.PastResidentRepository;
import com.happynest.pg.repository.PaymentRepository;
import com.happynest.pg.repository.ResidentRepository;
import com.happynest.pg.repository.RoomRepository;
import com.happynest.pg.repository.TemporaryResidentRepository;
import com.happynest.pg.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact text of everything the assistant is allowed to know.
 * A small PG fits in one prompt, so any question can be answered without extra tool calls.
 */
@Service
@RequiredArgsConstructor
public class PgSnapshotService {

    private final ResidentRepository residentRepository;
    private final TemporaryResidentRepository temporaryResidentRepository;
    private final PastResidentRepository pastResidentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final TransferRepository transferRepository;
    private final PaymentService paymentService;

    @Transactional
    public String build() {
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.from(today);
        YearMonth previous = current.minusMonths(1);
        paymentService.ensureMonthPayments(current.getMonthValue(), current.getYear());

        Map<String, Resident> residents = new HashMap<>();
        for (Resident resident : residentRepository.findAllByOrderByNameAsc()) {
            residents.put(resident.getId(), resident);
        }
        Map<String, TemporaryResident> temporary = new HashMap<>();
        List<TemporaryResident> temporaryList =
                temporaryResidentRepository.findAllByOrderByJoiningDateAscNameAsc();
        for (TemporaryResident stay : temporaryList) {
            temporary.put(stay.getId(), stay);
        }

        Map<String, Payment> currentPayments = paymentsByResident(current);
        Map<String, Payment> previousPayments = paymentsByResident(previous);

        StringBuilder out = new StringBuilder();
        out.append("Today: ").append(today).append('\n');
        out.append("Current rent month: ").append(current).append('\n');
        out.append("Advance means the security deposit, unless the notes say otherwise.\n");
        out.append("If notes do not mention advance, say it is not recorded. Do not guess.\n\n");

        out.append("MONTHLY RESIDENTS (").append(residents.size()).append(")\n");
        if (residents.isEmpty()) {
            out.append("- none\n");
        }
        residents.values().stream()
                .sorted(Comparator.comparing(Resident::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(resident -> {
                    Payment nowPay = currentPayments.get(resident.getId());
                    Payment prevPay = previousPayments.get(resident.getId());
                    out.append("- ").append(resident.getName())
                            .append(" | phone ").append(resident.getPhone())
                            .append(" | room ").append(resident.getRoomNumber())
                            .append(" bed ").append(bedLabel(resident.getBedId()))
                            .append(" | sharing ").append(resident.getSharingType())
                            .append(" | joined ").append(resident.getJoiningDate())
                            .append(" | rent ").append(resident.getMonthlyRent())
                            .append(" | deposit/advance ").append(resident.getDeposit())
                            .append(" | refund ").append(resident.getRefundAmount())
                            .append(" | ").append(current).append(" rent ")
                            .append(paymentText(nowPay))
                            .append(" | ").append(previous).append(" rent ")
                            .append(paymentText(prevPay))
                            .append(" | notes: ").append(blank(resident.getNotes()))
                            .append('\n');
                });

        out.append("\nTEMPORARY RESIDENTS (").append(temporaryList.size()).append(")\n");
        if (temporaryList.isEmpty()) {
            out.append("- none\n");
        }
        for (TemporaryResident stay : temporaryList) {
            TemporaryResidentDto dto = DtoMapper.toTemporaryResidentDto(stay);
            out.append("- ").append(stay.getName())
                    .append(" | phone ").append(stay.getPhone())
                    .append(" | TEMPORARY STAY")
                    .append(" | room ").append(stay.getRoomNumber())
                    .append(" bed ").append(bedLabel(stay.getBedId()))
                    .append(" | sharing ").append(stay.getSharingType())
                    .append(" | joined ").append(stay.getJoiningDate())
                    .append(" | stay days ").append(stay.getStayDays())
                    .append(" | last day ").append(dto.getCheckoutDate())
                    .append(" | days left ").append(dto.getDaysLeft())
                    .append(Boolean.TRUE.equals(dto.isVacateToday()) ? " | VACATE TODAY" : "")
                    .append(" | per day ").append(stay.getAmountPerDay())
                    .append(" | total ").append(stay.getAmount())
                    .append(" | payment ").append(stay.getPaymentStatus())
                    .append(" | notes: ").append(blank(stay.getNotes()))
                    .append('\n');
        }

        out.append("\nROOMS AND BEDS\n");
        List<Room> rooms = roomRepository.findAllByOrderByRoomNumberAsc();
        int floor = -1;
        for (Room room : rooms) {
            if (room.getFloorNumber() != floor) {
                floor = room.getFloorNumber();
                out.append("Floor ").append(floor).append('\n');
            }
            long vacant = room.getBeds().stream().filter(b -> b.getStatus() == BedStatus.VACANT).count();
            out.append("  Room ").append(room.getRoomNumber())
                    .append(" type ").append(room.getType())
                    .append(" mode ").append(room.getOccupancyMode())
                    .append(" capacity ").append(room.getCapacity())
                    .append(" vacant beds ").append(vacant)
                    .append('\n');
            room.getBeds().stream()
                    .sorted(Comparator.comparing(Bed::getBedLabel))
                    .forEach(bed -> out.append("    Bed ").append(bed.getBedLabel())
                            .append(' ').append(bed.getStatus())
                            .append(' ').append(occupant(bed, residents, temporary))
                            .append('\n'));
        }

        List<Transfer> transfers = transferRepository.findAllByOrderByTransferDateDesc();
        out.append("\nTRANSFERS (").append(transfers.size()).append(")\n");
        if (transfers.isEmpty()) {
            out.append("- none\n");
        }
        int shown = 0;
        for (Transfer transfer : transfers) {
            if (shown++ >= 40) {
                break;
            }
            Resident person = residents.get(transfer.getResidentId());
            out.append("- ").append(person == null ? transfer.getResidentId() : person.getName())
                    .append(" | ").append(transfer.getTransferDate())
                    .append(" | from room ").append(transfer.getFromRoomNumber())
                    .append(" bed ").append(bedLabel(transfer.getFromBedId()))
                    .append(" to room ").append(transfer.getToRoomNumber())
                    .append(" bed ").append(bedLabel(transfer.getToBedId()))
                    .append(" | note: ").append(blank(transfer.getNote()))
                    .append('\n');
        }

        List<PastResident> past = pastResidentRepository.findAllByOrderByCheckoutDateDesc();
        out.append("\nPAST RESIDENTS (").append(past.size()).append(")\n");
        if (past.isEmpty()) {
            out.append("- none\n");
        }
        for (PastResident person : past) {
            out.append("- ").append(person.getName())
                    .append(" | left ").append(person.getCheckoutDate())
                    .append(" | last room ").append(person.getLastRoomNumber())
                    .append(" | deposit ").append(person.getDeposit())
                    .append(" | refund ").append(person.getRefundAmount())
                    .append(" | reason: ").append(blank(person.getDeletionReason()))
                    .append(" | notes: ").append(blank(person.getNotes()))
                    .append('\n');
        }
        return out.toString();
    }

    private Map<String, Payment> paymentsByResident(YearMonth month) {
        Map<String, Payment> map = new HashMap<>();
        for (Payment payment : paymentRepository.findByMonthAndYear(month.getMonthValue(), month.getYear())) {
            map.put(payment.getResidentId(), payment);
        }
        return map;
    }

    private static String occupant(Bed bed, Map<String, Resident> residents, Map<String, TemporaryResident> temporary) {
        if (bed.getStatus() != BedStatus.OCCUPIED) {
            return "";
        }
        if (bed.getOccupantType() == OccupantType.TEMPORARY) {
            TemporaryResident stay = temporary.get(bed.getResidentId());
            return "TEMPORARY " + (stay == null ? "unknown" : stay.getName());
        }
        Resident resident = residents.get(bed.getResidentId());
        return "MONTHLY " + (resident == null ? "unknown" : resident.getName());
    }

    private static String paymentText(Payment payment) {
        if (payment == null) {
            return "NO RECORD";
        }
        String text = String.valueOf(payment.getStatus());
        if (payment.getPaymentMethod() != null) {
            text += " via " + payment.getPaymentMethod();
        }
        if (payment.getPaidDate() != null) {
            text += " on " + payment.getPaidDate();
        }
        return text;
    }

    private static String bedLabel(String bedId) {
        if (bedId == null || !bedId.contains("-")) {
            return bedId == null ? "-" : bedId;
        }
        return bedId.substring(bedId.lastIndexOf('-') + 1);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "-" : value.replace('\n', ' ').trim();
    }
}
