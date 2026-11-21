package studio.galaxy.statusbar;

import android.animation.*;
import android.content.*;
import android.graphics.*;
import android.os.BatteryManager;
import android.view.*;
import android.util.Log;

final class ChargingWave extends BroadcastReceiver {
    private int previous=-1;
    private final Context app;
    private ChargingWave(Context context) {app=context;}
    static void install(Context context) {
        try {context.registerReceiver(new ChargingWave(context),new IntentFilter(Intent.ACTION_BATTERY_CHANGED));}
        catch(RuntimeException ex){Log.w("GalaxyBarStudio","Animation de charge indisponible",ex);}
    }
    public void onReceive(Context c,Intent intent) {
        int now=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
        if(now<0)return;
        boolean show=previous==0 && (now & (BatteryManager.BATTERY_PLUGGED_AC|BatteryManager.BATTERY_PLUGGED_USB))!=0;
        previous=now;
        if(!show || !ValueAnimator.areAnimatorsEnabled())return;
        int max=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100), raw=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        if(max<=0 || raw<0)return;
        try {new Wave(app,Math.min(100,raw*100/max)).start();}
        catch(RuntimeException e){Log.w("GalaxyBarStudio","Animation ignorée",e);}
    }
    private static final class Wave extends View {
        final WindowManager wm;
        final PixelBattery battery=new PixelBattery();
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect chip=new Rect();
        final int level;
        float progress;
        Wave(Context c,int level){super(c);wm=c.getSystemService(WindowManager.class);this.level=level;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);}
        void start(){
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            lp.setTitle("Galaxy Studio charging");lp.setFitInsetsTypes(0);
            wm.addView(this,lp);
            postDelayed(this::remove,1800);
            ValueAnimator anim=ValueAnimator.ofFloat(0,1);anim.setDuration(1250);
            anim.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidate();});
            anim.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){remove();}});
            anim.start();
        }
        void remove(){if(getParent()!=null)try{wm.removeViewImmediate(this);}catch(RuntimeException ignored){}}
        protected void onDraw(Canvas c){
            float radius=Math.max(1,(float)Math.hypot(getWidth()*.5,getHeight())*progress);
            int alpha=(int)(90*(1-progress));
            paint.setShader(new RadialGradient(getWidth()*.5f,getHeight(),radius,
                new int[]{0,0,Color.argb(alpha,115,224,172),0},new float[]{0,.65f,.9f,1},Shader.TileMode.CLAMP));
            c.drawCircle(getWidth()*.5f,getHeight(),radius,paint);
            float dp=getResources().getDisplayMetrics().density;
            int x=getWidth()/2,y=(int)(getHeight()*.88f);
            chip.set(x-(int)(21*dp),y-(int)(10*dp),x+(int)(21*dp),y+(int)(10*dp));
            int save=c.saveLayerAlpha(null,(int)(255*Math.min(1,(1-progress)*5)));
            battery.draw(c,chip,level,true,Color.WHITE,true);c.restoreToCount(save);
        }
    }
}
