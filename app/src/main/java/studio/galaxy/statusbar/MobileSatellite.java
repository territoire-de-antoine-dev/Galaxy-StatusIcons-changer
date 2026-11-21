package studio.galaxy.statusbar;

import android.content.Context;
import android.telephony.*;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import java.util.List;
import java.util.WeakHashMap;
import de.robv.android.xposed.*;

final class MobileSatellite extends TelephonyCallback implements TelephonyCallback.ServiceStateListener,
        TelephonyCallback.SignalStrengthsListener, View.OnAttachStateChangeListener {
    private static final WeakHashMap<View,Boolean> bound = new WeakHashMap<>();
    private final ImageView slot;
    private final TelephonyManager sim;
    private final SatelliteIcon icon;
    private boolean registered, showing;
    private int priorAlpha;
    private MobileSatellite(ImageView slot,TelephonyManager sim) {
        this.slot=slot; this.sim=sim; icon=new SatelliteIcon(slot);
        slot.addOnAttachStateChangeListener(this);
        slot.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->icon.setBounds(0,0,r-l,b-t));
        if (slot.isAttachedToWindow()) onViewAttachedToWindow(slot);
    }
    static void install(ClassLoader loader) {
        try {
            Class<?> viewType=loader.loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView");
            XposedBridge.hookAllMethods(viewType,"constructAndBind",new XC_MethodHook(){
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.getResult() instanceof View) prepare((View)p.getResult());
                }
            });
            Object wm=XposedHelpers.callStaticMethod(Class.forName("android.view.WindowManagerGlobal"),"getInstance");
            for (Object root : (List<?>)XposedHelpers.callMethod(wm,"getWindowViews"))
                if (root instanceof View) find((View)root,viewType);
        } catch (Throwable error) { Log.w("GalaxyBarStudio","Association des vues mobiles incomplète",error); }
    }
    private static void find(View v,Class<?> type) {
        if (type.isInstance(v)) prepare(v);
        else if (v instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++) find(group.getChildAt(i),type);
        }
    }
    private static void prepare(View view) {
        try {
            if(bound.containsKey(view)) return;
            int sub=XposedHelpers.getIntField(view,"subId");
            if (sub < 0 || sub == Integer.MAX_VALUE) return;
            int id=view.getResources().getIdentifier("mobile_signal","id",Profile.TARGET);
            View v=view.findViewById(id);
            if (!(v instanceof ImageView)) return;
            TelephonyManager service=view.getContext().getSystemService(TelephonyManager.class);
            new MobileSatellite((ImageView)v,service.createForSubscriptionId(sub));
            bound.put(view,true);
        } catch (RuntimeException e) { Log.w("GalaxyBarStudio","Vue mobile conservée",e); }
    }
    public void onViewAttachedToWindow(View v) {
        if (registered) return;
        try { sim.registerTelephonyCallback(v.getContext().getMainExecutor(),this); registered=true; }
        catch (RuntimeException ex) { Log.w("GalaxyBarStudio","État radio indisponible",ex); }
    }
    public void onViewDetachedFromWindow(View v) {
        boolean wasRegistered=registered; registered=false;
        if (wasRegistered) try {sim.unregisterTelephonyCallback(this);} catch(RuntimeException ignored) {}
        icon.level=-1;
        display(RadioDisplay.Mode.NORMAL);
    }
    public void onSignalStrengthsChanged(SignalStrength signal) {
        if (!registered) return;
        icon.level=signal==null || signal.getCellSignalStrengths().isEmpty() ? -1 : signal.getLevel();
        icon.invalidateSelf();
    }
    public void onServiceStateChanged(ServiceState state) {
        if (!registered) return;
        RadioDisplay.Mode mode=RadioDisplay.Mode.NORMAL;
        try {
            if(state!=null) mode=RadioDisplay.mode(state.getState(),
                (int)XposedHelpers.callMethod(state,"getDataRegistrationState"),
                (boolean)XposedHelpers.callMethod(state,"isEmergencyOnly"),
                (boolean)XposedHelpers.callMethod(state,"isUsingNonTerrestrialNetwork"));
        } catch (RuntimeException | LinkageError ignored) { }
        display(mode);
    }
    private void display(RadioDisplay.Mode mode) {
        icon.mode=mode;
        if(mode!=RadioDisplay.Mode.NORMAL && !showing) {
            priorAlpha=slot.getImageAlpha(); slot.setImageAlpha(0);
            icon.setBounds(0,0,slot.getWidth(),slot.getHeight());
            slot.getOverlay().add(icon); showing=true;
        } else if (mode==RadioDisplay.Mode.NORMAL && showing) {
            slot.getOverlay().remove(icon); slot.setImageAlpha(priorAlpha); showing=false;
        }
        icon.invalidateSelf();
    }
}
