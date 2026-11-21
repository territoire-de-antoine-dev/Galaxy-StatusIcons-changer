package studio.galaxy.statusbar;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

final class SatelliteIcon extends Drawable {
    private final ImageView slot;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    RadioDisplay.Mode mode = RadioDisplay.Mode.NORMAL;
    int level = -1;
    SatelliteIcon(ImageView slot) { this.slot = slot; }
    public void draw(Canvas canvas) {
        if (mode == RadioDisplay.Mode.NORMAL || slot.getWidth()==0 || slot.getHeight()==0) return;
        int restore = canvas.save();
        float size = Math.min(slot.getWidth(),slot.getHeight());
        canvas.translate((slot.getWidth()-size)/2,(slot.getHeight()-size)/2);
        canvas.scale(size/24f,size/24f);
        boolean graded = mode == RadioDisplay.Mode.EMERGENCY;
        if (graded) { canvas.translate(0,1); canvas.scale(.80f,.80f); }
        int tint = slot.getImageTintList()==null ? Color.WHITE :
                slot.getImageTintList().getColorForState(slot.getDrawableState(),Color.WHITE);
        paint.setColor(tint);
        paint.setColorFilter(slot.getColorFilter());
        paint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.rotate(-38,12,12);
        canvas.drawRoundRect(8.5f,7,15.5f,17,1.6f,1.6f,paint);
        canvas.drawRoundRect(0.8f,8,7,15,1,1,paint);
        canvas.drawRoundRect(17,8,23.2f,15,1,1,paint);
        canvas.drawRect(6,10.5f,18,12.2f,paint);
        canvas.drawRect(11,16,13,20,paint);
        canvas.restore();
        if (graded) {
            // Four dots distinguish measured terrestrial levels; unknown has only a hollow ring.
            for (int i=0;i<4;i++) {
                paint.setStyle(i<level ? Paint.Style.FILL : Paint.Style.STROKE);
                paint.setStrokeWidth(.85f);
                canvas.drawCircle(24.5f,23-i*5,1.5f,paint);
                if (level<0) break;
            }
        }
        canvas.restoreToCount(restore);
    }
    public void setAlpha(int alpha) {}
    public void setColorFilter(ColorFilter filter) {}
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
