package org.lineageos.xiaomi_bluetooth.earbuds;

import androidx.annotation.NonNull;


public class Earbud {

    private static final int EARBUDS_CHARGING_BIT_MASK = 0x80;
    private static final int EARBUDS_BATTERY_LEVEL_MASK = 0x7F;

    public final boolean charging;
    public final int battery;

    public Earbud(byte raw) {
        this.charging = (raw & EARBUDS_CHARGING_BIT_MASK) != 0;
        this.battery = raw & EARBUDS_BATTERY_LEVEL_MASK;
    }

    public boolean isValid() {
        return this.battery <= 100;
    }

    @NonNull
    @Override
    public String toString() {
        return "Earbud{" +
                "charging=" + charging +
                ", battery=" + battery +
                '}';
    }
}
