package app.simple.inure.decorations.toggles;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import app.simple.inure.R;
import app.simple.inure.preferences.AppearancePreferences;
import app.simple.inure.themes.manager.ThemeManager;

public class Switch extends View {
    
    private final Paint thumbPaint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private final RectF backgroundRect = new RectF();
    private Drawable thumbDrawable;
    private ValueAnimator thumbAnimator;
    private ValueAnimator thumbSizeAnimator;
    private ValueAnimator backgroundAnimator;
    private OnCheckedChangeListener onCheckedChangeListener;
    
    private final float fixedThumbScale = 1F;
    private final float thumbScaleOnTouch = 1.50F;
    private float thumbX = 0;
    private float thumbY = 0;
    private float width = 0;
    private float height = 0;
    private float thumbPadding = 0;
    private float thumbDiameter = 0;
    private float currentThumbPosition = 0;
    private final float cornerRadius = 200;
    private float shadowRadius = 0;
    private float currentThumbScale = 1;
    private int backgroundColor = 0;
    private int duration = 0;
    
    private boolean isChecked = false;
    
    public Switch(Context context) {
        super(context);
        init();
    }
    
    public Switch(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public Switch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    public Switch(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    
    private void init() {
        setClipToOutline(false);
        
        if (AppearancePreferences.INSTANCE.getColoredIconShadows()) {
            shadowRadius = 10F;
        } else {
            shadowRadius = 0F;
        }
        
        backgroundPaint.setAntiAlias(true);
        
        thumbPaint.setAntiAlias(true);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(shadowRadius, 0, 0, Color.WHITE);
        
        backgroundColor = ThemeManager.INSTANCE.getTheme().getSwitchViewTheme().getSwitchOffColor();
        duration = getResources().getInteger(R.integer.animation_duration);
        
        thumbDrawable = ContextCompat.getDrawable(getContext(), R.drawable.switch_thumb);
        thumbDrawable.setTint(Color.WHITE);
        
        post(() -> {
            width = getWidth();
            height = getHeight();
            
            Log.d("Switch", "width: " + width);
            Log.d("Switch", "height: " + height);
            
            thumbDiameter = height - thumbPadding;
            
            backgroundRect.set(0, 0, width, height);
        });
        
        // updateSwitchState();
    }
    
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // Draw background
        backgroundPaint.setColor(backgroundColor);
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint);
        
        // Draw thumb
        thumbPaint.setShadowLayer(shadowRadius, 0, 0, Color.WHITE);
        canvas.drawCircle(thumbX, thumbY, (thumbDiameter / 2) * currentThumbScale, thumbPaint);
        Log.d("Switch", "thumbX: " + thumbX + " thumbY: " + thumbY + " thumbDiameter: " + thumbDiameter);
        
        // Position thumb based on currentThumbPosition
        // canvas.translate(thumbX, thumbY);
        
        super.onDraw(canvas);
    }
    
    @SuppressLint ("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN ->
                    animateThumbSize(true);
            case MotionEvent.ACTION_MOVE -> {
                thumbX = event.getX();
                invalidate();
            }
            case MotionEvent.ACTION_CANCEL -> {
                animateThumbSize(false);
                Log.d("Switch", "ACTION_CANCEL");
            }
            case MotionEvent.ACTION_UP -> {
                animateThumbSize(false);
                isChecked = event.getX() > width / 2;
                animateThumbPosition();
                animateBackgroundColor();
                if (onCheckedChangeListener != null) {
                    onCheckedChangeListener.onCheckedChanged(isChecked);
                }
            }
        }
        
        return super.onTouchEvent(event);
    }
    
    private void updateSwitchState() {
        if (isChecked) {
            currentThumbPosition = width;
            currentThumbScale = fixedThumbScale;
            backgroundColor = AppearancePreferences.INSTANCE.getAccentColor();
            thumbX = width - thumbDiameter;
        } else {
            currentThumbPosition = 0;
            currentThumbScale = fixedThumbScale;
            backgroundColor = ThemeManager.INSTANCE.getTheme().getSwitchViewTheme().getSwitchOffColor();
            thumbX = 0;
        }
        
        invalidate();
    }
    
    private void animateThumbPosition() {
        if (thumbAnimator != null && thumbAnimator.isRunning()) {
            thumbAnimator.cancel();
        }
        
        if (isChecked) {
            thumbAnimator = ValueAnimator.ofFloat(currentThumbPosition, width);
            thumbAnimator.setInterpolator(new OvershootInterpolator());
            thumbAnimator.setDuration(duration);
            thumbAnimator.addUpdateListener(animation -> {
                currentThumbPosition = (float) animation.getAnimatedValue();
                invalidate();
            });
        } else {
            thumbAnimator = ValueAnimator.ofFloat(currentThumbPosition, 0);
            thumbAnimator.setInterpolator(new OvershootInterpolator());
            thumbAnimator.setDuration(duration);
            thumbAnimator.addUpdateListener(animation -> {
                currentThumbPosition = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        
        thumbAnimator.start();
    }
    
    private void animateThumbSize(boolean isTouchDown) {
        if (thumbSizeAnimator != null && thumbSizeAnimator.isRunning()) {
            thumbSizeAnimator.cancel();
        }
        
        if (isTouchDown) {
            thumbSizeAnimator = ValueAnimator.ofFloat(currentThumbScale, thumbScaleOnTouch);
            thumbSizeAnimator.setDuration(duration);
            thumbAnimator.setInterpolator(new DecelerateInterpolator());
            thumbSizeAnimator.addUpdateListener(animation -> {
                currentThumbScale = (float) animation.getAnimatedValue();
                invalidate();
            });
        } else {
            thumbSizeAnimator = ValueAnimator.ofFloat(currentThumbScale, fixedThumbScale);
            thumbSizeAnimator.setDuration(duration);
            thumbAnimator.setInterpolator(new DecelerateInterpolator());
            thumbSizeAnimator.addUpdateListener(animation -> {
                currentThumbScale = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        
        thumbSizeAnimator.start();
    }
    
    private void animateBackgroundColor() {
        if (backgroundAnimator != null && backgroundAnimator.isRunning()) {
            backgroundAnimator.cancel();
        }
        
        if (isChecked) {
            backgroundAnimator = ValueAnimator.ofArgb(backgroundColor, AppearancePreferences.INSTANCE.getAccentColor());
            backgroundAnimator.setDuration(duration);
            backgroundAnimator.addUpdateListener(animation -> {
                backgroundColor = (int) animation.getAnimatedValue();
                backgroundPaint.setColor(backgroundColor);
                invalidate();
            });
        } else {
            backgroundAnimator = ValueAnimator.ofArgb(backgroundColor, ThemeManager.INSTANCE.getTheme().getSwitchViewTheme().getSwitchOffColor());
            backgroundAnimator.setDuration(duration);
            backgroundAnimator.addUpdateListener(animation -> {
                backgroundColor = (int) animation.getAnimatedValue();
                backgroundPaint.setColor(backgroundColor);
                invalidate();
            });
        }
        
        backgroundAnimator.start();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = getResources().getDimensionPixelSize(R.dimen.switch_width);
        int desiredHeight = getResources().getDimensionPixelSize(R.dimen.switch_height);
        
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int width;
        int height;
        
        // Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            // Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            // Be whatever you want
            width = desiredWidth;
        }
        
        // Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            // Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            // Be whatever you want
            height = desiredHeight;
        }
        
        thumbPadding = height * 0.25F;
        thumbDiameter = height * 0.5F;
        thumbY = (float) height / 2;
        thumbX = thumbDiameter / 2 + thumbPadding;
        
        // MUST CALL THIS
        setMeasuredDimension(width, height);
    }
    
    public void setChecked(boolean checked) {
        isChecked = checked;
        animateThumbPosition();
        animateBackgroundColor();
    }
    
    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }
}
