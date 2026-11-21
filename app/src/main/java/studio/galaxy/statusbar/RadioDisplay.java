package studio.galaxy.statusbar;

import android.telephony.ServiceState;

final class RadioDisplay {
    enum Mode { NORMAL, EMERGENCY, NO_SERVICE }
    static Mode mode(int voice, int data, boolean emergency, boolean ntn) {
        if (ntn || voice == ServiceState.STATE_POWER_OFF || voice == ServiceState.STATE_IN_SERVICE ||
                data == ServiceState.STATE_IN_SERVICE) return Mode.NORMAL;
        if (voice == ServiceState.STATE_EMERGENCY_ONLY || emergency) return Mode.EMERGENCY;
        return voice == ServiceState.STATE_OUT_OF_SERVICE && data == ServiceState.STATE_OUT_OF_SERVICE
                ? Mode.NO_SERVICE : Mode.NORMAL;
    }
}
