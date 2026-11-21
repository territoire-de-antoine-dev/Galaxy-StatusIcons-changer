package studio.galaxy.statusbar;

import android.graphics.*;

final class PixelBattery {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body=new Path();
    private final RectF box=new RectF();
    private final Path bolt=new Path();
    private final Typeface font=Typeface.create("sans-serif-medium",Typeface.BOLD);
    PixelBattery() {
        bolt.moveTo(9,1);bolt.lineTo(4,7);bolt.lineTo(7,7);bolt.lineTo(6,11);
        bolt.lineTo(11,5);bolt.lineTo(8,5);bolt.close();
    }
    void draw(Canvas c, Rect bounds,int level,boolean charging,int tint,boolean percent) {
        int save=c.save();
        float scale=Math.min(bounds.width()/26f,bounds.height()/13f);
        c.translate(bounds.exactCenterX()-13*scale,bounds.exactCenterY()-6.5f*scale);c.scale(scale,scale);
        box.set(0,0,24,13);body.reset();body.addRoundRect(box,4,4,Path.Direction.CW);
        int ink=charging ? 0xff70e09e : (level<=15 ? 0xffe36b63 : tint);
        p.setStyle(Paint.Style.FILL);p.setColor(ink);c.drawRoundRect(23,4,26,9,1,1,p);
        p.setAlpha(85);c.drawPath(body,p);p.setAlpha(255);
        int clip=c.save();c.clipPath(body);c.clipRect(0,0,24*level/100f,13);c.drawPath(body,p);c.restoreToCount(clip);
        if(charging || percent) {
            int dark=Color.luminance(ink)>.42 ? Color.BLACK : Color.WHITE;
            for(int pass=0;pass<2;pass++){
                int region=c.save();float x=24*level/100f;
                c.clipRect(pass==0?0:x,0,pass==0?x:24,13);
                p.setColor(pass==0?dark:tint);
                if(charging) c.drawPath(bolt,p);
                if(percent){p.setTypeface(font);p.setTextSize(charging?7:9);p.setTextAlign(Paint.Align.CENTER);
                    c.drawText(Integer.toString(level),charging?16:12,6.5f-(p.ascent()+p.descent())/2,p);}
                c.restoreToCount(region);
            }
        }
        c.restoreToCount(save);
    }
}
