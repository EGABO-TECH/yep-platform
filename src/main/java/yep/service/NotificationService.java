package yep.service;

import yep.data.DataStore;
import yep.model.Notification;
import yep.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** SEND Notification(user, text) from the pseudo-code. */
public class NotificationService {
    private final DataStore store;

    public NotificationService(DataStore store) {
        this.store = store;
    }

    public void send(User user, String text, String link) {
        store.notifications.add(new Notification(store.newId(), user, text, link, LocalDateTime.now()));
    }

    public List<Notification> forUser(User user) {
        return store.notifications.stream()
                .filter(n -> n.getUser().getUserId() == user.getUserId())
                .sorted(Comparator.comparing(Notification::getSentAt).reversed())
                .toList();
    }

    public long unreadCount(User user) {
        return forUser(user).stream().filter(n -> !n.isRead()).count();
    }

    public void markAllRead(User user) {
        forUser(user).forEach(Notification::markRead);
    }
}
