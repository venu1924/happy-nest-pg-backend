package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Resident;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the optional notes for a part-paid deposit or a promised rent date. */
public final class NoteNotices {

    private static final Pattern DEPOSIT_PAID = Pattern.compile(
            "(?i)(?:(\\d+(?:\\.\\d+)?)\\s*(?:deposit|advance)\\s*paid|(?:deposit|advance)\\s*paid\\s*(\\d+(?:\\.\\d+)?)|paid\\s*(?:only\\s*)?(\\d+(?:\\.\\d+)?)\\s*(?:as\\s*)?(?:deposit|advance))");
    private static final Pattern REMAINING_AFTER = Pattern.compile(
            "(?i)remaining\\s+(?:rent\\s+)?(?:of\\s+)?(\\d+(?:\\.\\d+)?)");
    private static final Pattern REMAINING_BEFORE = Pattern.compile(
            "(?i)(\\d+(?:\\.\\d+)?)\\s+remaining");
    private static final Pattern PAID_ONLY = Pattern.compile(
            "(?i)paid\\s+only\\s+(\\d+(?:\\.\\d+)?)");
    private static final Pattern MONTH_DATE = Pattern.compile(
            "(?i)(\\d{1,2})(?:st|nd|rd|th)?\\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+(\\d{4}))?");
    private static final Pattern NUMERIC_DATE = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})");

    private NoteNotices() {}

    public static String advanceDue(Resident resident, LocalDate today) {
        if (resident.getJoiningDate() == null || today.isBefore(resident.getJoiningDate())) {
            return null;
        }
        BigDecimal paid = depositPaid(resident.getNotes());
        if (paid == null || resident.getDeposit() == null) {
            return null;
        }
        BigDecimal left = resident.getDeposit().subtract(paid);
        if (left.signum() <= 0) {
            return null;
        }
        return money(left) + " advance to be paid by " + resident.getName();
    }

    public static String rentRemainder(Resident resident, LocalDate today) {
        String notes = resident.getNotes();
        if (notes == null || !notes.toLowerCase(Locale.ROOT).contains("remaining")) {
            return null;
        }
        LocalDate due = promisedDate(notes, today);
        BigDecimal amount = remainingAmount(notes);
        if (due == null || amount == null || today.isBefore(due)) {
            return null;
        }
        return resident.getName() + " should pay remaining " + money(amount) + " rent today";
    }

    public static String vacate(String name, boolean onTheDay, BigDecimal refund) {
        String message = name + (onTheDay ? " should vacate today" : " should vacate tomorrow");
        if (refund != null && refund.signum() > 0) {
            message += " and refund " + money(refund);
        }
        return message;
    }

    private static BigDecimal depositPaid(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        Matcher matcher = DEPOSIT_PAID.matcher(notes);
        if (!matcher.find()) {
            return null;
        }
        for (int group = 1; group <= matcher.groupCount(); group++) {
            if (matcher.group(group) != null) {
                return new BigDecimal(matcher.group(group));
            }
        }
        return null;
    }

    private static BigDecimal remainingAmount(String notes) {
        Matcher after = REMAINING_AFTER.matcher(notes);
        if (after.find()) {
            return new BigDecimal(after.group(1));
        }
        Matcher before = REMAINING_BEFORE.matcher(notes);
        if (before.find()) {
            return new BigDecimal(before.group(1));
        }
        Matcher paidOnly = PAID_ONLY.matcher(notes);
        if (paidOnly.find()) {
            return new BigDecimal(paidOnly.group(1));
        }
        return null;
    }

    static LocalDate promisedDate(String notes, LocalDate today) {
        Matcher numeric = NUMERIC_DATE.matcher(notes);
        if (numeric.find()) {
            int day = Integer.parseInt(numeric.group(1));
            int month = Integer.parseInt(numeric.group(2));
            int year = Integer.parseInt(numeric.group(3));
            if (year < 100) {
                year += 2000;
            }
            return safeDate(year, month, day);
        }
        Matcher named = MONTH_DATE.matcher(notes);
        if (!named.find()) {
            return null;
        }
        int day = Integer.parseInt(named.group(1));
        int month = monthNumber(named.group(2));
        int year = named.group(3) != null ? Integer.parseInt(named.group(3)) : today.getYear();
        LocalDate date = safeDate(year, month, day);
        if (date != null && named.group(3) == null && date.isBefore(today.minusDays(45))) {
            date = safeDate(year + 1, month, day);
        }
        return date;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static int monthNumber(String name) {
        String shortName = name.toLowerCase(Locale.ROOT).substring(0, 3);
        return switch (shortName) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> Month.JANUARY.getValue();
        };
    }

    static String money(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
