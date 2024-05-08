package org.openhab.binding.tailwind.internal.dto;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class TailwindDoor implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 4331180371949634469L;

    @SerializedName("index")
    private long index; // Door index number (0-door1, 1-door2, 2-door3)

    @SerializedName("status")
    private String status; // Door events (open, close, lock, enable, disable, reboot)

    @SerializedName("lockup")
    private long lockup; // TailWind controller locked door (0-unlocked, 1-locked) to prevent injury

    @SerializedName("disabled")
    private long disabled; // Door is disabled via APP (0-not disabled, 1- disabled)

    public long getIndex() {
        return index;
    }

    public void setIndex(long value) {
        this.index = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public long getLockup() {
        return lockup;
    }

    public void setLockup(long value) {
        this.lockup = value;
    }

    public long getDisabled() {
        return disabled;
    }

    public void setDisabled(long value) {
        this.disabled = value;
    }

    @Override
    public String toString() {
        return "Door [index=" + index + ", status=" + status + ", lockup=" + lockup + ", disabled=" + disabled + "]";
    }
}
