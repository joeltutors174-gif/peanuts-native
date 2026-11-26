package dev.nxnative.android;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;

public final class BananaBackgroundView extends View
        implements Choreographer.FrameCallback {

    private static final long POP_MS = 260;
    private static final long RESPAWN_MS = 5000;
    private static final long GROW_MS = 380;
    private static final long MIN_FRAME_NS = 33_333_333L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shard = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triangle = new Path();
    private final float density;
    private final int baseAlpha = 75;

    private boolean wanted, attached, posted;
    private long lastNs;
    private double phase;
    private Runnable onPop;

    private final java.util.HashMap<Long, Long> popped =
            new java.util.HashMap<>();

    public BananaBackgroundView(Context c) {
        super(c);

        density = getResources().getDisplayMetrics().density;

        paint.setColor(Color.rgb(255, 32, 53));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAlpha(baseAlpha);

        shard.setColor(Color.rgb(255, 32, 53));

        setImportantForAccessibility(
                IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setRunning(boolean on) {
        wanted = on;
        update();
    }

    private boolean running() {
        return wanted && attached
                && ValueAnimator.areAnimatorsEnabled();
    }

    private void update() {
        boolean active = running();

        if (active && !posted) {
            lastNs = 0;
            posted = true;
            Choreographer.getInstance()
                    .postFrameCallback(this);
        } else if (!active && posted) {
            posted = false;
            Choreographer.getInstance()
                    .removeFrameCallback(this);
        }
    }

    @Override
    public void doFrame(long nowNs) {
        posted = false;

        if (!running()) return;

        if (lastNs == 0) lastNs = nowNs;

        long dt = nowNs - lastNs;

        if (dt >= MIN_FRAME_NS) {
            phase += Math.min(dt, 100_000_000L) / 1e9;
            lastNs = nowNs;
            invalidate();
        }

        posted = true;
        Choreographer.getInstance()
                .postFrameCallback(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        update();
        super.onDetachedFromWindow();
    }

    public void setOnPop(Runnable r) {
        onPop = r;
    }

    public boolean popAt(float x, float y) {

        double wx =
                phase * BananaGrid.SPEED_X_DP * density;
        double wy =
                phase * BananaGrid.SPEED_Y_DP * density;

        long key = BananaGrid.hit(
                x, y,
                getWidth(), getHeight(),
                wx, wy, density);

        if (key == BananaGrid.NONE) return false;

        long now = SystemClock.uptimeMillis();
        Long at = popped.get(key);

        if (at != null &&
                now - at < RESPAWN_MS + GROW_MS)
            return false;

        popped.put(key, now);
        invalidate();

        if (!running())
            postInvalidateDelayed(RESPAWN_MS + GROW_MS);

        if (onPop != null) onPop.run();

        return true;
    }

    private void drawSymbol(
            Canvas canvas, int type, float size) {

        float r = size / 2f;

        switch (type) {

            case 0:
                // Triangle
                triangle.reset();
                triangle.moveTo(0, -r);
                triangle.lineTo(r, r);
                triangle.lineTo(-r, r);
                triangle.close();
                canvas.drawPath(triangle, paint);
                break;

            case 1:
                // Circle
                canvas.drawCircle(0, 0, r, paint);
                break;

            case 2:
                // Cross
                canvas.drawLine(-r, -r, r, r, paint);
                canvas.drawLine(-r, r, r, -r, paint);
                break;

            case 3:
                // Square
                canvas.drawRoundRect(
                        -r, -r, r, r,
                        2 * density,
                        2 * density, paint);
                break;

            default:
                // Plus
                canvas.drawLine(-r, 0, r, 0, paint);
                canvas.drawLine(0, -r, 0, r, paint);
                break;
        }
    }

    private void drawGamer(
            Canvas canvas, int type,
            Long at, long now, boolean odd) {

        float scale = 1f;
        float alpha = 1f;

        if (at != null) {
            long age = now - at;

            if (age < POP_MS &&
                    ValueAnimator.areAnimatorsEnabled()) {

                float t = age / (float) POP_MS;

                scale = 1f + t * 0.6f;
                alpha = 1f - t;

                shard.setAlpha(
                        (int) (150 * (1f - t)));

                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2 / 7;
                    float reach = t * 30 * density;

                    canvas.drawCircle(
                            (float) Math.cos(a) * reach,
                            (float) Math.sin(a) * reach,
                            2 * density,
                            shard);
                }

            } else if (age < RESPAWN_MS) {
                return;
            } else {
                float t = Math.min(1f,
                        (age - RESPAWN_MS)
                                / (float) GROW_MS);

                scale = t;
                alpha = t;
            }
        }

        paint.setAlpha(
                Math.round(baseAlpha * alpha));

        canvas.save();

        float rotation = odd ? 18f : -12f;

        rotation += (float)
                Math.sin(phase * 0.5 + type) * 8f;

        canvas.rotate(rotation);
        canvas.scale(scale, scale);

        drawSymbol(canvas, type, 25f * density);

        canvas.restore();

        paint.setAlpha(baseAlpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) return;

        canvas.drawColor(Color.rgb(16, 16, 20));

        float step = BananaGrid.STEP_DP * density;
        float row = BananaGrid.ROW_DP * density;

        double wx =
                phase * BananaGrid.SPEED_X_DP * density;

        double wy =
                phase * BananaGrid.SPEED_Y_DP * density;

        int shiftC = (int) Math.floor(wx / step);
        int shiftR = (int) Math.floor(wy / (2 * row));

        float cx = w / 2f;
        float cy = h / 2f;

        float reach =
                (float) Math.hypot(w, h) / 2f + step;

        int cols =
                (int) Math.ceil(reach / step) + 1;

        int rows =
                2 * ((int) Math.ceil(reach / row / 2) + 1);

        long now = SystemClock.uptimeMillis();

        if (!popped.isEmpty()) {
            popped.values().removeIf(
                    at -> now - at >= RESPAWN_MS + GROW_MS);
        }

        canvas.save();
        canvas.rotate(BananaGrid.GRID_DEG, cx, cy);

        for (int r = -rows; r <= rows; r++) {

            int rowId = r - 2 * shiftR - 1;

            for (int c = -cols; c <= cols; c++) {

                int colId = c - shiftC - 1;

                int type = Math.floorMod(
                        rowId * 31 + colId * 17, 5);

                canvas.save();

                canvas.translate(
                        cx + (float) BananaGrid.centreX(
                                rowId, colId, step, wx),
                        cy + (float) BananaGrid.centreY(
                                rowId, row, wy));

                drawGamer(
                        canvas,
                        type,
                        popped.get(
                                BananaGrid.key(rowId, colId)),
                        now,
                        BananaGrid.odd(rowId));

                canvas.restore();
            }
        }

        canvas.restore();
    }
}
