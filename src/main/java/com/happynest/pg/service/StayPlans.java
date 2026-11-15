package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Resident;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class StayPlans {

    private StayPlans() {}

    public static boolean joinsLaterMonth(LocalDate joining, LocalDate today) {
        return joining != null && joining.isAfter(YearMonth.from(today).atEndOfMonth());
    }

    public static boolean matches(Bed bed, String residentBedId) {
        return residentBedId != null && residentBedId.equals(bed.getBedId());
    }

    public static long vacantBedsNextMonth(List<Bed> beds, List<Resident> residents, LocalDate today) {
        LocalDate nextMonthEnd = YearMonth.from(today).plusMonths(1).atEndOfMonth();
        long count = 0;
        for (Bed bed : beds) {
            if (reservedNextMonth(bed, residents, today, nextMonthEnd)) {
                continue;
            }
            if (bed.getStatus() == BedStatus.VACANT
                    || bed.getOccupantType() == OccupantType.TEMPORARY) {
                count++;
                continue;
            }
            Resident holder = holder(bed, residents);
            if (holder != null && holder.getVacateDate() != null && !holder.getVacateDate().isAfter(nextMonthEnd)) {
                count++;
            }
        }
        return count;
    }

    public static long residentsThisMonth(List<Resident> residents, LocalDate today) {
        LocalDate thisMonthEnd = YearMonth.from(today).atEndOfMonth();
        long count = 0;
        for (Resident resident : residents) {
            if (resident.getJoiningDate() == null || !resident.getJoiningDate().isAfter(thisMonthEnd)) {
                count++;
            }
        }
        return count;
    }

    /** People still here next month, plus people joining next month. Temporary stays are not in this list. */
    public static long residentsNextMonth(List<Resident> residents, LocalDate today) {
        YearMonth next = YearMonth.from(today).plusMonths(1);
        LocalDate nextEnd = next.atEndOfMonth();
        LocalDate thisEnd = YearMonth.from(today).atEndOfMonth();
        long count = 0;
        for (Resident resident : residents) {
            LocalDate joining = resident.getJoiningDate();
            if (joining != null && YearMonth.from(joining).equals(next)) {
                count++;
                continue;
            }
            boolean hereNow = joining == null || !joining.isAfter(thisEnd);
            boolean leaving = resident.getVacateDate() != null && !resident.getVacateDate().isAfter(nextEnd);
            if (hereNow && !leaving) {
                count++;
            }
        }
        return count;
    }

    public static boolean bookableForLaterJoin(Bed bed, List<Resident> residents, LocalDate joining) {
        LocalDate today = LocalDate.now();
        if (reservedNextMonth(bed, residents, today, joining.withDayOfMonth(joining.lengthOfMonth()))) {
            return false;
        }
        if (bed.getStatus() != BedStatus.OCCUPIED || bed.getOccupantType() == OccupantType.TEMPORARY) {
            return true;
        }
        Resident holder = holder(bed, residents);
        return holder != null
                && holder.getVacateDate() != null
                && !holder.getVacateDate().isAfter(joining.withDayOfMonth(joining.lengthOfMonth()));
    }

    private static boolean reservedNextMonth(Bed bed, List<Resident> residents, LocalDate today, LocalDate until) {
        for (Resident resident : residents) {
            if (!matches(bed, resident.getBedId()) || resident.getJoiningDate() == null) {
                continue;
            }
            if (resident.getJoiningDate().isAfter(today) && !resident.getJoiningDate().isAfter(until)) {
                return true;
            }
        }
        return false;
    }

    private static Resident holder(Bed bed, List<Resident> residents) {
        if (bed.getResidentId() == null) {
            return null;
        }
        for (Resident resident : residents) {
            if (resident.getId().equals(bed.getResidentId())) {
                return resident;
            }
        }
        return null;
    }
}
