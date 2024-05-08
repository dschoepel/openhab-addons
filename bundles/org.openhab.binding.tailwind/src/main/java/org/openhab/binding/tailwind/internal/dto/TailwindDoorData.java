package org.openhab.binding.tailwind.internal.dto;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class TailwindDoorData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6777053863921520937L;

    @SerializedName("door1")
    private TailwindDoor door1; // Garage door 1 (index = 0)

    @SerializedName("door2")
    private TailwindDoor door2; // Garage door 2 (index = 1)

    @SerializedName("door3")
    private TailwindDoor door3; // Garage door 3 (index = 2)

    public TailwindDoor getDoor1() {
        return door1;
    }

    public void setDoor1(TailwindDoor value) {
        this.door1 = value;
    }

    public TailwindDoor getDoor2() {
        return door2;
    }

    public void setDoor2(TailwindDoor value) {
        this.door2 = value;
    }

    public TailwindDoor getDoor3() {
        return door3;
    }

    public void setDoor3(TailwindDoor value) {
        this.door3 = value;
    }

    @Override
    public String toString() {
        return "DoorData [door1=" + door1 + ", door2=" + door2 + ", door3=" + door3 + "]";
    }

}
