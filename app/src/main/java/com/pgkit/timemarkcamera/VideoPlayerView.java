package com.pgkit.timemarkcamera;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Video fills the viewing area; controls overlay it without changing media geometry. */
final class VideoPlayerView extends FrameLayout {
    private final Activity activity;
    private final VideoView video;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinearLayout center, bottom, top;
    private final ImageButton play, mute, repeat;
    private final TextView time, speed, feedback;
    private final SeekBar seek;
    private final Runnable onInteraction;
    private MediaPlayer player;
    private boolean prepared, disposed, dragging, controlsVisible = true, muted, looping, settingsOpen;
    private float rate = 1f;
    private final int initialPosition;
    private final Runnable hide = () -> setControls(false);
    private final Runnable clearFeedback = () -> feedbackVisibility(false);
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (disposed || !prepared) return;
            if (!dragging) { seek.setProgress(video.getCurrentPosition()); updateTime(video.getCurrentPosition()); }
            handler.postDelayed(this, 250);
        }
    };

    VideoPlayerView(Activity activity, Uri uri, int position, boolean muted, boolean looping, float rate,
                    IntConsumer swipe, Runnable onInteraction) {
        super(activity);
        this.activity = activity;
        this.initialPosition = position;
        this.muted = muted;
        this.looping = looping;
        this.rate = rate;
        this.onInteraction = onInteraction;
        setBackgroundColor(Color.WHITE);
        video = new VideoView(activity);
        addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        GestureDetector gestures = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                setControls(!controlsVisible); return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e) {
                skip(e.getX() < getWidth()/2f ? -10000 : 10000); return true;
            }
            @Override public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                if (a == null || Math.abs(b.getX()-a.getX()) < dp(80) || Math.abs(vx) < Math.abs(vy)*1.5f) return false;
                swipe.accept(b.getX() < a.getX() ? 1 : -1); return true;
            }
        });
        View touch = new View(activity);
        touch.setContentDescription("Video. Chạm để hiện điều khiển; chạm hai lần bên trái hoặc phải để tua 10 giây.");
        touch.setOnTouchListener((v, event) -> gestures.onTouchEvent(event));
        addView(touch, new FrameLayout.LayoutParams(-1, -1));

        top = new LinearLayout(activity);
        top.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(8), dp(12), dp(8));
        mute = button(R.drawable.ic_volume, "Tắt tiếng", this::toggleMute);
        top.addView(mute, linear(48, 48));
        speed = CameraUi.text(activity, "1×", 14, Color.WHITE, true);
        speed.setGravity(Gravity.CENTER);
        speed.setBackground(CameraUi.shape(activity, 0x88000000, 0, 24));
        speed.setContentDescription("Tốc độ phát");
        speed.setOnClickListener(v -> showSpeed());
        UiMotion.press(speed);
        LinearLayout.LayoutParams speedParams = linear(56, 48); speedParams.leftMargin = dp(8);
        top.addView(speed, speedParams);
        addView(top, new FrameLayout.LayoutParams(-1, dp(64), Gravity.TOP));

        center = new LinearLayout(activity);
        center.setGravity(Gravity.CENTER);
        center.addView(skipControl(false), linear(56, 56));
        play = button(R.drawable.ic_play, "Phát video", this::togglePlayback);
        play.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams playParams = linear(64, 64);
        playParams.leftMargin = playParams.rightMargin = dp(24);
        center.addView(play, playParams);
        center.addView(skipControl(true), linear(56, 56));
        addView(center, new FrameLayout.LayoutParams(-1, dp(88), Gravity.CENTER));
        play.setEnabled(false);

        bottom = new LinearLayout(activity);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(16), dp(12), dp(16), dp(12));
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xaa000000}));
        seek = new SeekBar(activity);
        seek.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0x66ffffff));
        seek.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        seek.setContentDescription("Tiến độ video");
        seek.setEnabled(false);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) { dragging = true; handler.removeCallbacks(hide); }
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) { if (user) updateTime(value); }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (prepared) video.seekTo(bar.getProgress());
                dragging = false; scheduleHide();
            }
        });
        bottom.addView(seek, linear(-1, 40));
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        time = CameraUi.text(activity, "Đang tải video…", 13, Color.WHITE, false);
        row.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
        repeat = button(R.drawable.ic_repeat, "Bật phát lặp", this::toggleLoop);
        row.addView(repeat, linear(48, 48));
        bottom.addView(row, linear(-1, 48));
        addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        feedback = CameraUi.text(activity, "", 15, Color.WHITE, true);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(dp(16), dp(10), dp(16), dp(10));
        feedback.setBackground(CameraUi.shape(activity, 0xbb000000, 0, 20));
        feedback.setVisibility(INVISIBLE);
        FrameLayout.LayoutParams feedbackParams = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        feedbackParams.topMargin = dp(76);
        addView(feedback, feedbackParams);

        video.setOnPreparedListener(media -> {
            if (disposed) return;
            player = media; prepared = true;
            player.setLooping(this.looping);
            player.setVolume(this.muted ? 0 : 1, this.muted ? 0 : 1);
            // Setting PlaybackParams may start MediaPlayer. Pause before showing the initial frame.
            try { player.setPlaybackParams(player.getPlaybackParams().setSpeed(this.rate)); player.pause(); }
            catch (RuntimeException ignored) { this.rate = 1f; }
            seek.setMax(video.getDuration()); seek.setEnabled(true); play.setEnabled(true);
            video.seekTo(Math.max(1, Math.min(initialPosition, video.getDuration())));
            updateButtons(); tick.run();
        });
        video.setOnCompletionListener(media -> {
            if (disposed) return;
            updateButtons(); setControls(true);
        });
        video.setOnErrorListener((media, what, extra) -> {
            if (!disposed) {
                prepared = false; handler.removeCallbacks(tick); handler.removeCallbacks(hide);
                time.setText("Không phát được video"); play.setEnabled(false); seek.setEnabled(false);
                setControls(true);
            }
            return true;
        });
        video.setVideoURI(uri);
    }

    private View skipControl(boolean forward) {
        LinearLayout control = new LinearLayout(activity);
        control.setOrientation(LinearLayout.VERTICAL); control.setGravity(Gravity.CENTER);
        control.setBackground(CameraUi.shape(activity, 0x66000000, 0, 40));
        ImageView arrow = new ImageView(activity);
        arrow.setImageResource(forward ? R.drawable.ic_forward : R.drawable.ic_rewind);
        arrow.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        control.addView(arrow, linear(24, 24));
        TextView label = CameraUi.text(activity, "10 giây", 11, Color.WHITE, false);
        control.addView(label);
        control.setContentDescription(forward ? "Tua tới 10 giây" : "Tua lùi 10 giây");
        control.setFocusable(true); control.setOnClickListener(v -> skip(forward ? 10000 : -10000));
        UiMotion.press(control); return control;
    }

    private void togglePlayback() {
        if (!prepared) return;
        if (video.isPlaying()) video.pause();
        else {
            if (video.getCurrentPosition() >= video.getDuration()-100) video.seekTo(0);
            video.start();
        }
        updateButtons(); setControls(true);
    }
    private void skip(int delta) {
        if (!prepared) return;
        int target = (int)Math.max(0L, Math.min((long)video.getDuration(), (long)video.getCurrentPosition()+delta));
        video.seekTo(target); seek.setProgress(target); updateTime(target);
        feedback.setText(delta < 0 ? "Lùi 10 giây" : "Tới 10 giây");
        feedbackVisibility(true); handler.removeCallbacks(clearFeedback); handler.postDelayed(clearFeedback, 700);
        setControls(true);
    }
    private void toggleMute() {
        if (!prepared) return;
        muted = !muted; player.setVolume(muted ? 0 : 1, muted ? 0 : 1);
        updateButtons(); scheduleHide();
    }
    private void toggleLoop() {
        if (!prepared) return;
        looping = !looping; player.setLooping(looping); updateButtons(); scheduleHide();
    }
    private void showSpeed() {
        if (!prepared) return;
        settingsOpen = true; handler.removeCallbacks(hide);
        float[] rates = {.5f, .75f, 1f, 1.25f, 1.5f, 2f};
        AppSheet sheet = new AppSheet(activity, "Tốc độ phát", "Chỉ đổi tốc độ xem, video gốc giữ nguyên.");
        for (float value : rates) {
            TextView choice = CameraUi.action(activity, speedLabel(value), value == rate, () -> {
                if (!disposed && prepared) {
                    boolean wasPlaying = video.isPlaying();
                    try {
                        player.setPlaybackParams(player.getPlaybackParams().setSpeed(value));
                        if (!wasPlaying) player.pause();
                        rate = value;
                    } catch (RuntimeException error) { feedback.setText("Video không hỗ trợ tốc độ này"); feedbackVisibility(true); handler.postDelayed(clearFeedback, 1600); }
                    updateButtons();
                }
                sheet.dialog.dismiss();
            });
            LinearLayout.LayoutParams params = linear(-1, 48); params.bottomMargin = dp(8);
            sheet.content.addView(choice, params);
        }
        sheet.dialog.setOnDismissListener(dialog -> { settingsOpen = false; scheduleHide(); });
        sheet.show();
    }
    private void updateButtons() {
        boolean playing = prepared && video.isPlaying();
        play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        play.setContentDescription(playing ? "Tạm dừng video" : (prepared && video.getCurrentPosition() >= video.getDuration()-100 ? "Phát lại video" : "Phát video"));
        mute.setImageResource(muted ? R.drawable.ic_muted : R.drawable.ic_volume);
        mute.setContentDescription(muted ? "Bật tiếng" : "Tắt tiếng");
        repeat.setImageTintList(ColorStateList.valueOf(looping ? 0xff86b5ff : Color.WHITE));
        repeat.setSelected(looping); repeat.setContentDescription(looping ? "Tắt phát lặp" : "Bật phát lặp");
        speed.setText(speedLabel(rate));
    }
    private static String speedLabel(float value) { return String.format(Locale.US, "%s×", value == (int)value ? String.valueOf((int)value) : String.valueOf(value)); }
    private void updateTime(int position) { time.setText(duration(position) + " / " + duration(video.getDuration())); }
    private void scheduleHide() {
        handler.removeCallbacks(hide);
        AccessibilityManager accessibility = (AccessibilityManager)activity.getSystemService(Activity.ACCESSIBILITY_SERVICE);
        if (prepared && video.isPlaying() && !dragging && !settingsOpen
                && (accessibility == null || !accessibility.isTouchExplorationEnabled())) handler.postDelayed(hide, 2500);
    }
    private void setControls(boolean visible) {
        if (disposed) return;
        controlsVisible = visible;
        for (View view : new View[]{top, center, bottom}) {
            view.animate().cancel(); view.setVisibility(VISIBLE);
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) view.animate().alpha(visible ? 1 : 0).setDuration(160)
                    .withEndAction(() -> view.setVisibility(visible ? VISIBLE : INVISIBLE)).start();
            else { view.setAlpha(visible ? 1 : 0); view.setVisibility(visible ? VISIBLE : INVISIBLE); }
        }
        if (visible) { onInteraction.run(); scheduleHide(); }
    }
    private void feedbackVisibility(boolean visible) { if (!disposed) feedback.setVisibility(visible ? VISIBLE : INVISIBLE); }
    @Override protected void onLayout(boolean changed, int left, int topEdge, int right, int bottomEdge) {
        super.onLayout(changed, left, topEdge, right, bottomEdge);
        // Align controls with the fitted video, including portrait/landscape letterboxing.
        top.setTranslationY(video.getTop());
        bottom.setTranslationY(-(getHeight() - video.getBottom()));
    }
    void pause() { if (prepared) { video.pause(); updateButtons(); setControls(true); } handler.removeCallbacks(hide); }
    int position() { return prepared ? video.getCurrentPosition() : initialPosition; }
    boolean muted() { return muted; }
    boolean looping() { return looping; }
    float rate() { return rate; }
    void dispose() { disposed = true; handler.removeCallbacksAndMessages(null); video.stopPlayback(); player = null; }
    private ImageButton button(int drawable, String label, Runnable action) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(drawable); button.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackground(CameraUi.shape(activity, 0x77000000, 0, 48));
        button.setContentDescription(label); button.setOnClickListener(v -> action.run()); UiMotion.press(button); return button;
    }
    private int dp(int value) { return CameraUi.dp(activity, value); }
    private LinearLayout.LayoutParams linear(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height)); }
    private static String duration(int millis) { return String.format(Locale.US, "%d:%02d", millis/60000, (millis/1000)%60); }
}
