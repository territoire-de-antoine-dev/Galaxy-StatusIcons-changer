package studio.galaxy.statusbar;

import org.junit.Test;
import static org.junit.Assert.*;
import static android.telephony.ServiceState.*;
import static studio.galaxy.statusbar.RadioDisplay.Mode.*;

public class RadioDisplayTest {
    @Test public void distinguishesEmergencyOutagePowerAndDataService() {
        assertEquals(EMERGENCY, RadioDisplay.mode(STATE_OUT_OF_SERVICE,STATE_OUT_OF_SERVICE,true,false));
        assertEquals(EMERGENCY, RadioDisplay.mode(STATE_EMERGENCY_ONLY,STATE_OUT_OF_SERVICE,false,false));
        assertEquals(NO_SERVICE, RadioDisplay.mode(STATE_OUT_OF_SERVICE,STATE_OUT_OF_SERVICE,false,false));
        assertEquals(NORMAL, RadioDisplay.mode(STATE_OUT_OF_SERVICE,STATE_IN_SERVICE,false,false));
        assertEquals(NORMAL, RadioDisplay.mode(STATE_IN_SERVICE,STATE_OUT_OF_SERVICE,false,false));
        assertEquals(NORMAL, RadioDisplay.mode(STATE_POWER_OFF,STATE_OUT_OF_SERVICE,false,false));
        assertEquals(NORMAL, RadioDisplay.mode(STATE_EMERGENCY_ONLY,STATE_OUT_OF_SERVICE,true,true));
    }
}
