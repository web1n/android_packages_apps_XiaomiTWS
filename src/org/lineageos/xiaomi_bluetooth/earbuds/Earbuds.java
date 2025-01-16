package org.lineageos.xiaomi_bluetooth.earbuds;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Earbuds {

    public final String macAddress;

    public final Earbud left;
    public final Earbud right;
    public final Earbud chargingCase;

    public Earbuds(String macAddress,
                   Earbud left, Earbud right, Earbud chargingCase) {
        this.macAddress = macAddress;

        this.left = left.isValid() ? left : null;
        this.right = right.isValid() ? right : null;
        this.chargingCase = chargingCase.isValid() ? chargingCase : null;
    }

    public boolean isValid() {
        return this.left != null || this.right != null || this.chargingCase != null;
    }

    @NonNull
    @Override
    public String toString() {
        return "Earbuds{" +
                "macAddress=" + macAddress +
                ", left=" + left +
                ", right=" + right +
                ", case=" + chargingCase +
                '}';
    }

    @Nullable
    public static Earbuds fromBytes(String macAddress, byte leftByte, byte rightByte, byte caseByte) {
        if (macAddress == null) {
            return null;
        }

        Earbud left = new Earbud(leftByte);
        Earbud right = new Earbud(rightByte);
        Earbud chargingCase = new Earbud(caseByte);
        if (!right.isValid() && !left.isValid() && !chargingCase.isValid()) {
            return null;
        }

        return new Earbuds(macAddress, left, right, chargingCase);
    }

}
