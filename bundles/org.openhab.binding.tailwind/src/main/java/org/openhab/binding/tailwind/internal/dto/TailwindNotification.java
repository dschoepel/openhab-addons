package org.openhab.binding.tailwind.internal.dto;

import com.google.gson.annotations.SerializedName;

public class TailwindNotification {
    /* ----- Notification details follow --------- */
    @SerializedName("door_idx")
    private Integer doorIdx; // Door index number (0-door1, 1-door2, 2-door3)

    @SerializedName("event")
    private String event; // Event type ((open, close, lock, enable, disable, reboot)

    public Integer getDoorIdx() {
        return doorIdx;
    }

    public void setDoorIdx(Integer doorIdx) {
        this.doorIdx = doorIdx;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return "Notify [doorIdx=" + doorIdx + ", event=" + event + "]";
    }
}
