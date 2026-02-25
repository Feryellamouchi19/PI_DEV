package services;

import entities.Evenement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class EventReminderService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void scheduleReminder(Evenement ev, Duration before) {
        if (ev == null || ev.getDateDebut() == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trigger = ev.getDateDebut().minus(before);

        long delayMs = Duration.between(now, trigger).toMillis();
        if (delayMs <= 0) return; // trop tard

        scheduler.schedule(() -> {
            NotificationService.info(
                    "Rappel événement",
                    ev.getTitre() + " commence bientôt (" + before.toMinutes() + " min)"
            );
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}