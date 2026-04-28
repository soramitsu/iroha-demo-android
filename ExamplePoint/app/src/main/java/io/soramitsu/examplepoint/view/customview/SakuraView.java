package io.soramitsu.examplepoint.view.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.soramitsu.examplepoint.R;

/**
 * Lightweight animated layer that draws drifting Sakura petals with a parallax offset influenced
 * by the accelerometer. Pointer events are disabled so the view never intercepts touches.
 */
public class SakuraView extends View implements SensorEventListener {

    private static final int MAX_PETALS = 50;
    private static final float SENSOR_SMOOTHING = 0.08f;

    private final Paint petalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path petalPath = new Path();
    private final Random random = new Random();
    private final List<Petal> petals = new ArrayList<>();

    @Nullable
    private SensorManager sensorManager;
    @Nullable
    private Sensor accelerometer;

    private float parallaxX = 0f;
    private float parallaxY = 0f;
    private float targetParallaxX = 0f;
    private float targetParallaxY = 0f;
    private boolean isAnimating;

    private int lightOuter;
    private int lightInner;
    private int darkOuter;
    private int darkInner;

    public SakuraView(Context context) {
        this(context, null);
    }

    public SakuraView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SakuraView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setAlpha(0.85f);
        petalPaint.setStyle(Paint.Style.FILL);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightOuter = ContextCompat.getColor(context, R.color.sakura_outer_light);
        lightInner = ContextCompat.getColor(context, R.color.sakura_inner_light);
        darkOuter = ContextCompat.getColor(context, R.color.sakura_outer_dark);
        darkInner = ContextCompat.getColor(context, R.color.sakura_inner_dark);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        petals.clear();
        if (w == 0 || h == 0) {
            return;
        }
        for (int i = 0; i < MAX_PETALS; i++) {
            petals.add(createPetal(w, h));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        accelerometer = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                : null;
        if (accelerometer != null && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDetachedFromWindow();
    }

    private void startAnimation() {
        if (isAnimating) {
            return;
        }
        isAnimating = true;
        postInvalidateOnAnimation();
    }

    private void stopAnimation() {
        isAnimating = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (petals.isEmpty()) {
            return;
        }
        parallaxX += (targetParallaxX - parallaxX) * SENSOR_SMOOTHING;
        parallaxY += (targetParallaxY - parallaxY) * SENSOR_SMOOTHING;
        for (Petal petal : petals) {
            updatePetal(petal, getWidth(), getHeight());
            drawPetal(canvas, petal);
        }
        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    private Petal createPetal(int width, int height) {
        Petal petal = new Petal();
        petal.x = random.nextFloat() * width;
        petal.y = random.nextFloat() * height;
        petal.size = 30f + random.nextFloat() * 45f;
        petal.speedY = 0.3f + random.nextFloat() * 1.4f;
        petal.speedX = -0.3f + random.nextFloat() * 0.6f;
        petal.rotation = random.nextFloat() * 360f;
        petal.rotationSpeed = -0.2f + random.nextFloat() * 0.4f;
        petal.sway = 0.5f + random.nextFloat();
        petal.palette = random.nextBoolean() ? Palette.LIGHT : Palette.DARK;
        return petal;
    }

    private void updatePetal(Petal petal, int width, int height) {
        final float wind = parallaxX * 4.5f;
        petal.y += petal.speedY + Math.abs(parallaxY) * 0.2f;
        petal.x += petal.speedX + wind + (float) Math.sin(petal.y / 65f) * petal.sway;
        petal.rotation += petal.rotationSpeed;

        if (petal.y > height + petal.size) {
            petal.y = -petal.size;
            petal.x = random.nextFloat() * width;
        }
        if (petal.x > width + petal.size) {
            petal.x = -petal.size;
        } else if (petal.x < -petal.size) {
            petal.x = width + petal.size;
        }
    }

    private void drawPetal(Canvas canvas, Petal petal) {
        final int outer = petal.palette == Palette.LIGHT ? lightOuter : darkOuter;
        final int inner = petal.palette == Palette.LIGHT ? lightInner : darkInner;
        petalPaint.setColor(outer);
        canvas.save();
        final float offsetX = parallaxX * 25f;
        final float offsetY = parallaxY * 18f;
        canvas.translate(petal.x + offsetX, petal.y + offsetY);
        canvas.rotate(petal.rotation);
        final float w = petal.size * 0.35f;
        final float h = petal.size * 0.65f;
        petalPath.reset();
        petalPath.moveTo(0, -h);
        petalPath.cubicTo(w, -h, w * 1.6f, -h * 0.1f, w * 0.4f, h * 0.2f);
        petalPath.cubicTo(w * 0.2f, h * 0.6f, w * 0.1f, h * 0.95f, 0, h);
        petalPath.cubicTo(-w * 0.1f, h * 0.95f, -w * 0.2f, h * 0.6f, -w * 0.4f, h * 0.2f);
        petalPath.cubicTo(-w * 1.6f, -h * 0.1f, -w, -h, 0, -h);
        canvas.drawPath(petalPath, petalPaint);
        petalPaint.setColor(inner);
        canvas.drawCircle(0, h * 0.4f, w * 0.5f, petalPaint);
        canvas.restore();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        final float normX = clamp(-event.values[0] / SensorManager.GRAVITY_EARTH, -1f, 1f);
        final float normY = clamp(event.values[1] / SensorManager.GRAVITY_EARTH, -1f, 1f);
        targetParallaxX = normX;
        targetParallaxY = normY;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Palette {
        LIGHT,
        DARK
    }

    private static final class Petal {
        float x;
        float y;
        float size;
        float speedY;
        float speedX;
        float rotation;
        float rotationSpeed;
        float sway;
        Palette palette;
    }
}
