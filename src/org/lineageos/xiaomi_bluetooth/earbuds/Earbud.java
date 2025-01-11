package org.lineageos.xiaomi_bluetooth.earbuds;

import androidx.annotation.NonNull;

import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.*;


public class Earbud {

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
