package com.happynest.pg.service;

import com.happynest.pg.domain.entity.AppSetting;
import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.PgNotification;
import com.happynest.pg.domain.entity.PushSubscription;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.entity.TemporaryResident;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.dto.NotificationView;
import com.happynest.pg.dto.PushSubscribeRequest;
import com.happynest.pg.repository.AppSettingRepository;
import com.happynest.pg.repository.PastResidentRepository;
import com.happynest.pg.repository.PaymentRepository;
import com.happynest.pg.repository.PgNotificationRepository;
import com.happynest.pg.repository.PushSubscriptionRepository;
import com.happynest.pg.repository.ResidentRepository;
import com.happynest.pg.repository.RoomRepository;
import com.happynest.pg.repository.TemporaryResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int[] JOINED_MARKS = {1, 10, 25, 50, 100, 150, 200, 250, 300, 400, 500, 750, 1000};
    private static final String INIT = "milestones.initialized";
    private static final String VAPID_PUBLIC = "vapid.public";
    private static final String VAPID_PRIVATE = "vapid.private";

    private final PgNotificationRepository notificationRepository;
    private final AppSettingRepository settingRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ResidentRepository residentRepository;
    private final PastResidentRepository pastResidentRepository;
    private final TemporaryResidentRepository temporaryResidentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Value("${app.notify.zone:Asia/Kolkata}")
    private String zoneId;

    @Value("${app.notify.morning-hour:8}")
    private int morningHour;

    @Value("${app.notify.rent-day:6}")
    private int rentDay;

    private final Object lock = new Object();
    private volatile PushService pushService;

    @Transactional
    public NotificationView.ListResult current() {
        refresh();
        return list();
    }

    @Transactional
    public void refresh() {
        synchronized (lock) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
            LocalDate today = now.toLocalDate();
            Map<String, String> state = loadState();
            boolean firstRun = state.get(INIT) == null;
            List<PgNotification> created = new ArrayList<>();

            if (now.getHour() >= morningHour) {
                created.addAll(dailyNotices(today, state));
            }
            created.addAll(milestones(today, state, firstRun));
            if (firstRun) {
                state.put(INIT, "true");
            }
            saveState(state);
            created.forEach(this::push);
        }
    }

    @Transactional(readOnly = true)
    public NotificationView.ListResult list() {
        List<NotificationView> items = notificationRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toView)
                .toList();
        return NotificationView.ListResult.builder()
                .items(items)
                .unread(notificationRepository.countBySeenFalse())
                .publicKey(settingRepository.findById(VAPID_PUBLIC).map(AppSetting::getSettingValue).orElse(""))
                .morningTime(morningLabel())
                .build();
    }

    @Transactional
    public void markRead(String id) {
        notificationRepository.findById(id).ifPresent(notice -> {
            notice.setSeen(true);
            notificationRepository.save(notice);
        });
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.findAll().forEach(notice -> notice.setSeen(true));
    }

    @Transactional
    public void subscribe(PushSubscribeRequest request) {
        ensureVapidKeys(loadState());
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);
        if (subscription.getId() == null) {
            subscription.setId(UUID.randomUUID().toString());
        }
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuthKey(request.getAuth());
        pushSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(pushSubscriptionRepository::delete);
    }

    private List<PgNotification> dailyNotices(LocalDate today, Map<String, String> state) {
        List<PgNotification> created = new ArrayList<>();
        for (TemporaryResident stay : temporaryResidentRepository.findAllByOrderByJoiningDateAscNameAsc()) {
            LocalDate lastDay = stay.checkoutDate();
            if (lastDay == null) {
                continue;
            }
            if (today.equals(lastDay)) {
                add(created, "vacate-tomorrow:" + stay.getId() + ":" + lastDay, today,
                        stay.getName() + " should vacate tomorrow");
            } else if (today.isAfter(lastDay)) {
                add(created, "vacate-today:" + stay.getId() + ":" + today, today,
                        stay.getName() + " should vacate today");
            }
            if (stay.getPaymentStatus() != PaymentStatus.PAID
                    && stay.getJoiningDate() != null
                    && !today.isBefore(stay.getJoiningDate())) {
                add(created, "temp-unpaid:" + stay.getId() + ":" + today, today,
                        stay.getName() + " (temporary) payment is pending");
            }
        }

        if (today.getDayOfMonth() >= rentDay) {
            paymentService.ensureMonthPayments(today.getMonthValue(), today.getYear());
            long unpaid = paymentRepository.countByMonthAndYearAndStatus(
                    today.getMonthValue(), today.getYear(), PaymentStatus.PENDING);
            if (unpaid > 0) {
                String message = unpaid == 1
                        ? "1 rent payment is overdue"
                        : unpaid + " rent payments are overdue";
                add(created, "rent-overdue:" + today, today, message);
            }
        }
        state.put("daily.ran." + today, "true");
        return created;
    }

    private List<PgNotification> milestones(LocalDate today, Map<String, String> state, boolean firstRun) {
        List<PgNotification> created = new ArrayList<>();
        List<Room> rooms = roomRepository.findAllByOrderByRoomNumberAsc();
        Map<Integer, int[]> floors = new LinkedHashMap<>();
        int occupied = 0;
        int vacant = 0;
        for (Room room : rooms) {
            int[] counts = floors.computeIfAbsent(room.getFloorNumber(), key -> new int[2]);
            for (Bed bed : room.getBeds()) {
                if (bed.getStatus() == BedStatus.VACANT) {
                    vacant++;
                    counts[0]++;
                } else if (bed.getStatus() == BedStatus.OCCUPIED) {
                    occupied++;
                    counts[1]++;
                }
            }
        }

        int fullFloors = 0;
        for (Map.Entry<Integer, int[]> entry : floors.entrySet()) {
            int floorVacant = entry.getValue()[0];
            int floorOccupied = entry.getValue()[1];
            boolean filled = floorOccupied > 0 && floorVacant == 0;
            String key = "floor.full." + entry.getKey();
            boolean wasFilled = "true".equals(state.get(key));
            if (!firstRun && filled && !wasFilled) {
                add(created, "floor-filled:" + entry.getKey() + ":" + today, today,
                        "Floor " + entry.getKey() + " is filled");
            }
            state.put(key, Boolean.toString(filled));
            if (filled) {
                fullFloors++;
            }
        }

        int previousFull = parseInt(state.get("floors.full.count"));
        if (!firstRun && fullFloors > previousFull && fullFloors > 0) {
            String message = fullFloors == 1
                    ? "Milestone : 1 floor is filled"
                    : "Milestone : " + fullFloors + " floors are filled";
            add(created, "floors-filled:" + fullFloors + ":" + today, today, message);
        }
        state.put("floors.full.count", Integer.toString(fullFloors));

        boolean allRooms = occupied > 0 && vacant == 0;
        boolean wasAllRooms = "true".equals(state.get("all.rooms.filled"));
        if (!firstRun && allRooms && !wasAllRooms) {
            add(created, "all-rooms:" + today, today, "Congratulations : ALL ROOMS ARE FILLED");
        }
        state.put("all.rooms.filled", Boolean.toString(allRooms));

        int usable = occupied + vacant;
        rememberShare(created, state, firstRun, today, usable, occupied, 2, "half.beds",
                "Milestone : half the beds are filled");
        rememberShare(created, state, firstRun, today, usable, occupied, 4,
                "three-quarter.beds", "Milestone : three quarters of the beds are filled");

        long joined = residentRepository.count() + pastResidentRepository.count();
        for (int mark : JOINED_MARKS) {
            String key = "joined." + mark;
            if (joined >= mark && !"true".equals(state.get(key))) {
                if (!firstRun) {
                    String message = mark == 1
                            ? "Milestone : your first resident joined the PG"
                            : "Milestone : " + mark + " people joined your PG";
                    add(created, "joined:" + mark, today, message);
                }
                state.put(key, "true");
            }
        }

        YearMonth month = YearMonth.from(today);
        paymentService.ensureMonthPayments(month.getMonthValue(), month.getYear());
        long residents = residentRepository.count();
        long unpaid = paymentRepository.countByMonthAndYearAndStatus(
                month.getMonthValue(), month.getYear(), PaymentStatus.PENDING);
        String rentKey = "rent.cleared." + month;
        boolean rentClear = residents > 0 && unpaid == 0;
        if (!firstRun && rentClear && !"true".equals(state.get(rentKey))) {
            String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            add(created, "all-rent-paid:" + month, today, "All " + monthName + " rent is paid");
        }
        state.put(rentKey, Boolean.toString(rentClear));

        List<TemporaryResident> temporary = temporaryResidentRepository.findAllByOrderByJoiningDateAscNameAsc();
        boolean tempClear = !temporary.isEmpty() && temporary.stream()
                .allMatch(stay -> stay.getPaymentStatus() == PaymentStatus.PAID);
        boolean wasTempClear = "true".equals(state.get("temp.all.paid"));
        if (!firstRun && tempClear && !wasTempClear) {
            add(created, "temp-all-paid:" + today, today, "All temporary stays are paid");
        }
        state.put("temp.all.paid", Boolean.toString(tempClear));
        return created;
    }

    private void rememberShare(
            List<PgNotification> created,
            Map<String, String> state,
            boolean firstRun,
            LocalDate today,
            int usable,
            int occupied,
            int parts,
            String key,
            String message) {
        boolean reached = usable > 0 && occupied * parts >= usable * (parts == 2 ? 1 : 3);
        if (reached && !"true".equals(state.get(key))) {
            if (!firstRun) {
                add(created, key, today, message);
            }
            state.put(key, "true");
        }
    }

    private void add(List<PgNotification> created, String dedupeKey, LocalDate today, String message) {
        if (notificationRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }
        PgNotification notice = new PgNotification();
        notice.setId(UUID.randomUUID().toString());
        notice.setDedupeKey(dedupeKey);
        notice.setMessage(message);
        notice.setNoticeDate(today);
        notice.setSeen(false);
        notice.setCreatedAt(Instant.now());
        created.add(notificationRepository.save(notice));
    }

    private void push(PgNotification notice) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            return;
        }
        PushService service = pushClient();
        if (service == null) {
            return;
        }
        String payload = "{\"title\":\"Happy Nest\",\"body\":" + quote(notice.getMessage())
                + ",\"url\":\"/notifications\"}";
        for (PushSubscription saved : subscriptions) {
            try {
                Subscription subscription = new Subscription(
                        saved.getEndpoint(),
                        new Subscription.Keys(saved.getP256dh(), saved.getAuthKey()));
                var response = service.send(new Notification(subscription, payload), Encoding.AES128GCM);
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    pushSubscriptionRepository.delete(saved);
                }
            } catch (Exception ex) {
                log.warn("Push failed: {}", ex.getMessage());
            }
        }
    }

    private PushService pushClient() {
        PushService existing = pushService;
        if (existing != null) {
            return existing;
        }
        String publicKey = setting(VAPID_PUBLIC);
        String privateKey = setting(VAPID_PRIVATE);
        if (publicKey == null || privateKey == null) {
            return null;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            PushService created = new PushService()
                    .setPublicKey(publicKey)
                    .setPrivateKey(privateKey)
                    .setSubject("https://happy-nest-pg-frontend.vercel.app");
            pushService = created;
            return created;
        } catch (Exception ex) {
            log.warn("Push client was not created: {}", ex.getMessage());
            return null;
        }
    }

    private void ensureVapidKeys(Map<String, String> state) {
        if (state.get(VAPID_PUBLIC) != null && state.get(VAPID_PRIVATE) != null) {
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(parameterSpec);
            KeyPair pair = generator.generateKeyPair();
            String publicKey = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(((ECPublicKey) pair.getPublic()).getQ().getEncoded(false));
            byte[] privateBytes = ((ECPrivateKey) pair.getPrivate()).getD().toByteArray();
            if (privateBytes.length > 32) {
                byte[] trimmed = new byte[32];
                System.arraycopy(privateBytes, privateBytes.length - 32, trimmed, 0, 32);
                privateBytes = trimmed;
            } else if (privateBytes.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(privateBytes, 0, padded, 32 - privateBytes.length, privateBytes.length);
                privateBytes = padded;
            }
            String privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(privateBytes);
            state.put(VAPID_PUBLIC, publicKey);
            state.put(VAPID_PRIVATE, privateKey);
            saveState(state);
            pushService = null;
        } catch (Exception ex) {
            log.warn("Could not create push keys: {}", ex.getMessage());
        }
    }

    private Map<String, String> loadState() {
        Map<String, String> state = new LinkedHashMap<>();
        settingRepository.findAll().forEach(setting -> state.put(setting.getSettingKey(), setting.getSettingValue()));
        ensureVapidKeys(state);
        return state;
    }

    private void saveState(Map<String, String> state) {
        state.forEach((key, value) -> settingRepository.save(new AppSetting(key, value)));
    }

    private String setting(String key) {
        return settingRepository.findById(key).map(AppSetting::getSettingValue).orElse(null);
    }

    private NotificationView toView(PgNotification notice) {
        return NotificationView.builder()
                .id(notice.getId())
                .message(notice.getMessage())
                .noticeDate(String.valueOf(notice.getNoticeDate()))
                .read(notice.isSeen())
                .createdAt(String.valueOf(notice.getCreatedAt()))
                .build();
    }

    private String morningLabel() {
        int hour = morningHour % 12 == 0 ? 12 : morningHour % 12;
        String suffix = morningHour < 12 ? "AM" : "PM";
        return hour + ":00 " + suffix;
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
