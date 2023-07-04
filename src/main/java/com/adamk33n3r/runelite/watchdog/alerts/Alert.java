package com.adamk33n3r.runelite.watchdog.alerts;

import com.adamk33n3r.runelite.watchdog.TriggerType;
import com.adamk33n3r.runelite.watchdog.notifications.Notification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public abstract class Alert {
    private boolean enabled = true;
    private String name;
    private int debounceTime;

    @Nullable
    private transient AlertGroup parent;

    @Setter(AccessLevel.PROTECTED)
    private List<Notification> notifications = new ArrayList<>();

    public Alert(String name) {
        this.name = name;
        this.debounceTime = 0;
    }

    public TriggerType getType() {
        return Arrays.stream(TriggerType.values())
            .filter(tType -> tType.getImplClass() == this.getClass())
            .findFirst()
            .orElse(null);
    }

    public void moveNotificationTo(Notification notification, int pos) {
        this.notifications.remove(notification);
        this.notifications.add(pos, notification);
    }

    public void moveNotificationToTop(Notification notification) {
        this.notifications.remove(notification);
        this.notifications.add(0, notification);
    }

    public void moveNotificationToBottom(Notification notification) {
        this.notifications.remove(notification);
        this.notifications.add(notification);
    }

    public void moveNotificationUp(Notification notification) {
        int curIdx = this.notifications.indexOf(notification);
        int newIdx = curIdx - 1;

        if (newIdx < 0) {
            return;
        }

        this.notifications.remove(notification);
        this.notifications.add(newIdx, notification);
    }

    public void moveNotificationDown(Notification notification) {
        int curIdx = this.notifications.indexOf(notification);
        int newIdx = curIdx + 1;

        if (newIdx >= this.notifications.size()) {
            return;
        }

        this.notifications.remove(notification);
        this.notifications.add(newIdx, notification);
    }

    @Nullable
    public List<AlertGroup> getAncestors() {
        if (this.parent == null) {
            return null;
        }

        ArrayList<AlertGroup> ancestors = new ArrayList<>();
        AlertGroup alertGroup = this.parent;
        do {
            ancestors.add(0, alertGroup);
        } while ((alertGroup = alertGroup.getParent()) != null);

        return ancestors;
    }
}
