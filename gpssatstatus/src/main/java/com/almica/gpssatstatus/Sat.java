package com.almica.gpssatstatus;

import java.util.Objects;

public class Sat {
    public final int prn;
    public final boolean used;
    public final float snr;

    public Sat(int prn, boolean used, float snr) {
        this.prn = prn;
        this.used = used;
        this.snr = snr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sat sat = (Sat) o;
        return prn == sat.prn && used == sat.used && Float.compare(sat.snr, snr) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prn, used, snr);
    }
}
