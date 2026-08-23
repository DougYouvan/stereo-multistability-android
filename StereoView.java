package ai.youvan.stereomultistability;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public final class StereoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private StimulusSpec spec = StimulusCatalog.all().get(0);
    private boolean crossed = false;

    public StereoView(Context context) {
        super(context);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        setBackgroundColor(Color.WHITE);
    }

    public void setStimulus(StimulusSpec spec, boolean crossed) {
        this.spec = spec;
        this.crossed = crossed;
        invalidate();
    }

    public StimulusSpec getStimulus() { return spec; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float sx=(float)(getWidth()/StereoMath.IMAGE_WIDTH);
        float sy=(float)(getHeight()/StereoMath.IMAGE_HEIGHT);
        float scale=Math.min(sx, sy);
        float ox=(getWidth()-(float)StereoMath.IMAGE_WIDTH*scale)/2f;
        float oy=(getHeight()-(float)StereoMath.IMAGE_HEIGHT*scale)/2f;
        paint.setStrokeWidth(Math.max(2f, 3f*scale));

        double[][] left = StereoMath.canonicalProject(StereoMath.LEFT, StereoMath.LEFT_PANEL_CENTER_X);
        double[][] right = StereoMath.canonicalProject(StereoMath.rightVertices(spec), StereoMath.RIGHT_PANEL_CENTER_X);
        if (!crossed) {
            drawWire(canvas,left,scale,ox,oy);
            drawWire(canvas,right,scale,ox,oy);
        } else {
            double[][] rAtLeft = StereoMath.canonicalProject(StereoMath.rightVertices(spec), StereoMath.LEFT_PANEL_CENTER_X);
            double[][] lAtRight = StereoMath.canonicalProject(StereoMath.LEFT, StereoMath.RIGHT_PANEL_CENTER_X);
            drawWire(canvas,rAtLeft,scale,ox,oy);
            drawWire(canvas,lAtRight,scale,ox,oy);
        }
    }

    private void drawWire(Canvas canvas,double[][] pts,float scale,float ox,float oy) {
        for(int[] e: StereoMath.EDGES) {
            float x1=ox+(float)pts[e[0]][0]*scale;
            float y1=oy+(float)pts[e[0]][1]*scale;
            float x2=ox+(float)pts[e[1]][0]*scale;
            float y2=oy+(float)pts[e[1]][1]*scale;
            canvas.drawLine(x1,y1,x2,y2,paint);
        }
    }

    public double canonicalToScreenScale() {
        return Math.min(getWidth()/StereoMath.IMAGE_WIDTH, getHeight()/StereoMath.IMAGE_HEIGHT);
    }
}
