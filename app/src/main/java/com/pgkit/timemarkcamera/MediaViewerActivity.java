package com.pgkit.timemarkcamera;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App-owned gallery and image/video viewer. Never launches an external ACTION_VIEW intent. */
public final class MediaViewerActivity extends ComponentActivity {
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private final ExecutorService thumbnails = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<MediaLibrary.Item> items = new ArrayList<>();
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private FrameLayout stage;
    private LinearLayout header, footer;
    private TextView title, subtitle, status, headerAction;
    private final Set<String> selected = new LinkedHashSet<>();
    private boolean selecting;
    private ZoomImageView photo;
    private VideoPlayerView video;
    private boolean videoMuted, videoLooping;
    private float videoRate = 1f;
    private Bitmap shownBitmap;
    private int index;
    private int request;
    private boolean grid;
    private boolean chrome = true;
    private int resumePosition;
    private int initialIndex;
    private boolean initialGrid;
    private boolean restored;
    private boolean firstOpen = true;
    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            restored = true;
            initialIndex = savedState.getInt("index");
            initialGrid = savedState.getBoolean("grid");
            resumePosition = savedState.getInt("video_position");
            videoMuted = savedState.getBoolean("video_muted");
            videoLooping = savedState.getBoolean("video_looping");
            videoRate = savedState.getFloat("video_rate", 1f);
            selecting = savedState.getBoolean("selecting");
            ArrayList<String> savedSelection = savedState.getStringArrayList("selection");
            if (savedSelection != null) selected.addAll(savedSelection);
        }
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(8), 0);
        header.addView(icon(R.drawable.ic_close, getString(R.string.ui_close_gallery), this::finish), lp(48, 56));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        title = text(getString(R.string.ui_gallery), 18, true);
        subtitle = text(getString(R.string.ui_your_photos_and_videos), 12, false);
        subtitle.setTextColor(CameraUi.MUTED);
        heading.addView(title);
        heading.addView(subtitle);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        headerAction = CameraUi.text(this, getString(R.string.ui_share_multiple), 16, CameraUi.BLUE, false);
        headerAction.setGravity(Gravity.CENTER);
        headerAction.setPadding(dp(12), 0, dp(12), 0);
        headerAction.setOnClickListener(v -> {
            if (!grid || !selecting) { selecting = true; selected.clear(); showGrid(); }
            else { selected.clear(); showGrid(); }
        });
        UiMotion.press(headerAction);
        header.addView(headerAction, lp(-2, 56));
        root.addView(header, lp(-1, 56));
        stage = new FrameLayout(this);
        stage.setClipChildren(true);
        root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
        footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(footer, lp(-1, 96));
        setContentView(root);
        status = text(getString(R.string.ui_loading_gallery), 16, false);
        status.setGravity(Gravity.CENTER);
        stage.addView(status, new FrameLayout.LayoutParams(-1, -1));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        Uri selected = getIntent().getData();
        images.execute(() -> {
            try {
                List<MediaLibrary.Item> loaded = MediaLibrary.load(this);
                main.post(() -> {
                    if (isDestroyed()) return;
                    items.addAll(loaded);
                    if (items.isEmpty()) { empty(getString(R.string.ui_no_photos_or_videos_yet), getString(R.string.ui_photos_you_take_in_the_app_will_appear)); return; }
                    int selectedIndex = 0;
                    for (int i = 0; i < items.size(); i++) if (items.get(i).uri.equals(selected)) selectedIndex = i;
                    if (restored) selectedIndex = Math.min(initialIndex, items.size()-1);
                    if (restored && initialGrid) showGrid(); else open(selectedIndex);
                });
            } catch (Exception error) {
                main.post(() -> { if (!isDestroyed()) empty(getString(R.string.ui_could_not_load_gallery), getString(R.string.ui_return_to_the_camera_and_try_opening_the)); });
            }
        });
    }

    private void empty(String heading, String message) {
        clearStage();
        title.setVisibility(View.VISIBLE);
        subtitle.setVisibility(View.VISIBLE);
        title.setText(getString(R.string.ui_gallery));
        subtitle.setText(getString(R.string.ui_photos_and_videos_in_the_app));
        headerAction.setVisibility(View.GONE);
        TextView notice = text(heading + "\n\n" + message, 16, false);
        notice.setGravity(Gravity.CENTER);
        notice.setPadding(dp(32), dp(24), dp(32), dp(24));
        stage.addView(notice, new FrameLayout.LayoutParams(-1, -1));
        footer.removeAllViews();
        TextView camera = CameraUi.action(this, getString(R.string.ui_back_to_camera), true, this::finish);
        footer.addView(camera, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void showGrid() {
        if (items.isEmpty()) return;
        clearStage();
        grid = true;
        showChrome(true);
        title.setVisibility(View.VISIBLE);
        subtitle.setVisibility(View.VISIBLE);
        title.setText(selecting ? getString(R.string.ui_export_share) : getString(R.string.ui_gallery));
        subtitle.setText(getResources().getQuantityString(R.plurals.media_count, items.size(), items.size()));
        headerAction.setVisibility(View.VISIBLE);
        headerAction.setText(selecting ? getString(R.string.ui_deselect) : getString(R.string.ui_select));
        List<int[]> rows = new ArrayList<>();
        String previousDay = "";
        int start = 0;
        while (start < items.size()) {
            String day = dayKey(items.get(start).date);
            if (!day.equals(previousDay)) { rows.add(new int[]{-1, start}); previousDay = day; }
            int end = start;
            while (end < items.size() && end-start < 3 && dayKey(items.get(end).date).equals(day)) end++;
            int[] positions = new int[end-start];
            for (int i = start; i < end; i++) positions[i-start] = i;
            rows.add(positions);
            start = end;
        }
        ListView gallery = new ListView(this);
        gallery.setDivider(null);
        gallery.setClipToPadding(false);
        gallery.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int p) { return rows.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public boolean isEnabled(int position) { return false; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                int[] row = rows.get(position);
                if (row[0] == -1) {
                    int first = row[1];
                    String day = dayKey(items.get(first).date);
                    int count = 0;
                    boolean all = true;
                    for (MediaLibrary.Item item : items) if (dayKey(item.date).equals(day)) {
                        count++; all &= selected.contains(item.uri.toString());
                    }
                    LinearLayout heading = new LinearLayout(MediaViewerActivity.this);
                    heading.setGravity(Gravity.CENTER_VERTICAL);
                    heading.setPadding(dp(16), dp(8), dp(12), dp(8));
                    TextView label = text(dayLabel(items.get(first).date) + " (" + count + ")", 17, true);
                    heading.addView(label, new LinearLayout.LayoutParams(0, dp(40), 1));
                    label.setGravity(Gravity.CENTER_VERTICAL);
                    if (selecting) {
                        ImageView check = selectionMark(all);
                        if (!all) {
                            check.setBackground(CameraUi.shape(MediaViewerActivity.this, Color.WHITE, 0xff555555, 24));
                            check.setImageResource(R.drawable.ic_check);
                            check.setImageTintList(ColorStateList.valueOf(0xff555555));
                            check.setPadding(dp(4), dp(4), dp(4), dp(4));
                        }
                        FrameLayout touch = new FrameLayout(MediaViewerActivity.this);
                        touch.addView(check, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
                        final boolean remove = all;
                        touch.setContentDescription(getString(remove ? R.string.deselect_day : R.string.select_day, dayLabel(items.get(first).date)));
                        touch.setOnClickListener(v -> {
                            for (MediaLibrary.Item item : items) if (dayKey(item.date).equals(day)) {
                                if (remove) selected.remove(item.uri.toString()); else selected.add(item.uri.toString());
                            }
                            notifyDataSetChanged(); buildGridFooter();
                        });
                        heading.addView(touch, lp(48, 48));
                    }
                    return heading;
                }
                LinearLayout line = new LinearLayout(MediaViewerActivity.this);
                int edge = (getResources().getDisplayMetrics().widthPixels-dp(6))/3;
                for (int column = 0; column < 3; column++) {
                    FrameLayout cell = new FrameLayout(MediaViewerActivity.this);
                    LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, edge, 1);
                    if (column > 0) cellParams.leftMargin = dp(3);
                    cellParams.bottomMargin = dp(3);
                    line.addView(cell, cellParams);
                    if (column >= row.length) continue;
                    int mediaIndex = row[column];
                    MediaLibrary.Item item = items.get(mediaIndex);
                    cell.setBackgroundColor(CameraUi.SURFACE);
                    ImageView thumbnail = new ImageView(MediaViewerActivity.this);
                    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    cell.addView(thumbnail, new FrameLayout.LayoutParams(-1, -1));
                    loadThumbnail(thumbnail, item, edge);
                    if (item.video) {
                        TextView badge = CameraUi.text(MediaViewerActivity.this, getString(R.string.ui_video), 12, Color.WHITE, true);
                        badge.setPadding(dp(6), dp(3), dp(6), dp(3));
                        badge.setBackground(CameraUi.shape(MediaViewerActivity.this, 0x99000000, 0, 4));
                        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
                        badgeParams.setMargins(dp(4), dp(4), dp(4), dp(4));
                        cell.addView(badge, badgeParams);
                    }
                    boolean checked = selected.contains(item.uri.toString());
                    if (selecting) {
                        FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP | Gravity.END);
                        markParams.setMargins(dp(12), dp(12), dp(12), dp(12));
                        cell.addView(selectionMark(checked), markParams);
                        thumbnail.setAlpha(checked ? .72f : 1f);
                    }
                    cell.setSelected(checked);
                    cell.setContentDescription(getString(item.video ? R.string.video_number : R.string.photo_number, mediaIndex + 1)
                            + (selecting ? (checked ? getString(R.string.ui_selected) : getString(R.string.ui_not_selected)) : ""));
                    cell.setOnClickListener(v -> {
                        if (selecting) {
                            if (!selected.add(item.uri.toString())) selected.remove(item.uri.toString());
                            notifyDataSetChanged(); buildGridFooter();
                        } else open(mediaIndex);
                    });
                    cell.setOnLongClickListener(v -> {
                        selecting = true; selected.clear(); selected.add(item.uri.toString()); showGrid(); return true;
                    });
                }
                return line;
            }
        });
        stage.addView(gallery, new FrameLayout.LayoutParams(-1, -1));
        buildGridFooter();
        UiMotion.reveal(stage);
    }

    private void loadThumbnail(ImageView thumbnail, MediaLibrary.Item item, int edge) {
        String key = item.uri.toString();
        thumbnail.setTag(key);
        Bitmap cached = cache.get(key);
        thumbnail.setImageBitmap(cached);
        if (cached != null || thumbnails.isShutdown()) return;
        thumbnails.execute(() -> {
            try {
                Bitmap result = MediaLibrary.thumbnail(this, item, edge);
                if (result == null) return;
                if (isDestroyed()) { result.recycle(); return; }
                cache.put(key, result);
                main.post(() -> { if (!isDestroyed() && key.equals(thumbnail.getTag())) thumbnail.setImageBitmap(result); });
            } catch (Exception ignored) { /* An inaccessible/deleted file keeps its placeholder. */ }
        });
    }

    private ImageView selectionMark(boolean checked) {
        ImageView mark = new ImageView(this);
        mark.setBackground(CameraUi.shape(this, checked ? CameraUi.BLUE : 0x22000000,
                checked ? CameraUi.BLUE : Color.WHITE, 24));
        if (checked) {
            mark.setImageResource(R.drawable.ic_check);
            mark.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            mark.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        return mark;
    }

    private void buildGridFooter() {
        footer.removeAllViews();
        footer.setOrientation(LinearLayout.VERTICAL);
        TextView caption = text(selecting ? (selected.isEmpty() ? getString(R.string.ui_select_photos_to_share) : getResources().getQuantityString(R.plurals.selected_count, selected.size(), selected.size()))
                : getString(R.string.ui_tap_to_view_hold_to_select_multiple), 14, false);
        caption.setGravity(Gravity.CENTER);
        footer.addView(caption, lp(-1, 28));
        TextView action = CameraUi.action(this, selecting ? getString(R.string.ui_share) : getString(R.string.ui_select_multiple_photos), true, () -> {
            if (selecting) share(true); else { selecting = true; selected.clear(); showGrid(); }
        });
        action.setEnabled(!selecting || !selected.isEmpty());
        action.setAlpha(action.isEnabled() ? 1 : .4f);
        footer.addView(action, lp(-1, 48));
    }

    private String dayKey(long date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(date));
    }
    private String dayLabel(long date) {
        if (dayKey(date).equals(dayKey(System.currentTimeMillis()))) return getString(R.string.ui_today);
        return UiDateTime.date(this, date);
    }

    private void open(int position) {
        if (position < 0 || position >= items.size()) return;
        if (!firstOpen && index != position) resumePosition = 0;
        firstOpen = false;
        clearStage();
        grid = false;
        selecting = false;
        selected.clear();
        title.setVisibility(View.GONE);
        subtitle.setVisibility(View.GONE);
        headerAction.setVisibility(View.VISIBLE);
        headerAction.setText(getString(R.string.ui_share_multiple));
        showChrome(true);
        index = position;
        MediaLibrary.Item item = items.get(index);
        title.setText((item.video ? getString(R.string.ui_video) : getString(R.string.ui_photo_alternate)) + " · " + (index+1) + "/" + items.size());
        subtitle.setText(UiDateTime.dateTime(this, item.date, false));
        buildFooter();
        if (item.video) { showVideo(item); return; }
        photo = new ZoomImageView(this, delta -> open(index+delta), () -> showChrome(!chrome));
        stage.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        ProgressBar loading = new ProgressBar(this);
        stage.addView(loading, new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER));
        int ticket = request;
        images.execute(() -> {
            try {
                Bitmap bitmap = MediaLibrary.image(this, item.uri, 2560);
                main.post(() -> {
                    if (isDestroyed() || ticket != request) { bitmap.recycle(); return; }
                    shownBitmap = bitmap;
                    photo.setImageBitmap(bitmap);
                    stage.removeView(loading);
                    UiMotion.reveal(photo);
                });
            } catch (Exception | OutOfMemoryError error) {
                main.post(() -> {
                    if (ticket != request || isDestroyed()) return;
                    stage.removeView(loading);
                    TextView failed = text(getString(R.string.ui_could_not_open_photo_the_file_may_have), 16, false);
                    failed.setGravity(Gravity.CENTER);
                    stage.addView(failed, new FrameLayout.LayoutParams(-1, -1));
                });
            }
        });
    }

    private void buildFooter() {
        footer.removeAllViews();
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.addView(tool(R.drawable.ic_share, getString(R.string.ui_share), () -> share(false)), new LinearLayout.LayoutParams(0, -1, 1));
        footer.addView(tool(R.drawable.ic_more, getString(R.string.ui_more), this::showMore), new LinearLayout.LayoutParams(0, -1, 1));
        footer.addView(tool(R.drawable.ic_info, getString(R.string.ui_info), this::showInfo), new LinearLayout.LayoutParams(0, -1, 1));
        footer.addView(tool(R.drawable.ic_delete, getString(R.string.ui_delete), this::confirmDelete), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private View tool(int drawable, String label, Runnable action) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        ImageView image = new ImageView(this);
        image.setImageResource(drawable);
        image.setImageTintList(ColorStateList.valueOf(0xff262626));
        button.addView(image, lp(28, 28));
        TextView caption = text(label, 13, false);
        caption.setTextColor(0xff555555);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams words = lp(-1, -2);
        words.topMargin = dp(8);
        button.addView(caption, words);
        button.setContentDescription(label);
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());
        UiMotion.press(button);
        return button;
    }

    private void showMore() {
        AppSheet.choices(this, getString(R.string.ui_more), null, new String[]{getString(R.string.ui_photo_grid), getString(R.string.ui_share_multiple)}, null, -1, choice -> {
            selecting = choice == 1; selected.clear(); showGrid();
        });
    }

    private void share(boolean multiple) {
        ArrayList<Uri> uris = new ArrayList<>();
        boolean hasImage = false, hasVideo = false;
        for (MediaLibrary.Item item : items) if (multiple ? selected.contains(item.uri.toString()) : item == items.get(index)) {
            uris.add(item.uri); hasVideo |= item.video; hasImage |= !item.video;
        }
        if (uris.isEmpty()) return;
        Intent intent = new Intent(uris.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
        intent.setType(hasVideo ? (hasImage ? "*/*" : "video/mp4") : "image/jpeg");
        if (uris.size() > 1) intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        else intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        ClipData clip = ClipData.newUri(getContentResolver(), getString(R.string.ui_photos_and_videos_alternate), uris.get(0));
        for (int i = 1; i < uris.size(); i++) clip.addItem(new ClipData.Item(uris.get(i)));
        intent.setClipData(clip);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(intent, getString(R.string.ui_share))); }
        catch (android.content.ActivityNotFoundException error) { notice(getString(R.string.ui_could_not_share), getString(R.string.ui_no_app_is_available_to_receive_this_photo)); }
    }

    private void confirmDelete() {
        MediaLibrary.Item item = items.get(index);
        AppSheet sheet = new AppSheet(this, item.video ? getString(R.string.ui_delete_video) : getString(R.string.ui_delete_photo),
                getString(R.string.ui_the_file_will_be_deleted_from_your_device));
        TextView name = text(item.name, 14, false);
        name.setPadding(0, dp(8), 0, dp(8));
        sheet.content.addView(name);
        sheet.actions(getString(R.string.ui_delete), () -> {
            sheet.dialog.dismiss();
            images.execute(() -> {
                try {
                    int deleted = getContentResolver().delete(item.uri, null, null);
                    main.post(() -> {
                        if (isDestroyed()) return;
                        if (deleted == 0) { notice(getString(R.string.ui_could_not_delete), getString(R.string.ui_the_file_has_been_moved_or_no_longer)); return; }
                        items.remove(item); cache.remove(item.uri.toString());
                        if (items.isEmpty()) empty(getString(R.string.ui_no_photos_or_videos_yet), getString(R.string.ui_photos_you_take_in_the_app_will_appear));
                        else open(Math.min(index, items.size()-1));
                    });
                } catch (Exception error) {
                    main.post(() -> { if (!isDestroyed()) notice(getString(R.string.ui_could_not_delete), getString(R.string.ui_permission_to_delete_this_file_was_denied_the)); });
                }
            });
        });
        sheet.show();
    }

    private void notice(String heading, String message) { new AppSheet(this, heading, message).show(); }

    private void showVideo(MediaLibrary.Item item) {
        video = new VideoPlayerView(this, item.uri, resumePosition, videoMuted, videoLooping, videoRate,
                delta -> open(index + delta), () -> showChrome(true));
        resumePosition = 0;
        stage.addView(video, new FrameLayout.LayoutParams(-1, -1));
    }

    private void showInfo() {
        MediaLibrary.Item item = items.get(index);
        AppSheet sheet = new AppSheet(this, item.video ? getString(R.string.ui_video_info) : getString(R.string.ui_photo_info), null);
        String date = UiDateTime.dateTime(this, item.date, true);
        for (String line : new String[]{item.name, date, item.width + " × " + item.height + " px", item.video ? getString(R.string.ui_mp4_video) : getString(R.string.ui_jpeg_photo)}) {
            TextView value = CameraUi.text(this, line, 15, CameraUi.INK, false);
            value.setPadding(0, dp(10), 0, dp(10));
            value.setTextIsSelectable(true);
            sheet.content.addView(value, new LinearLayout.LayoutParams(-1, -2));
        }
        sheet.show();
    }

    private void showChrome(boolean visible) {
        chrome = visible;
        // Keep content geometry fixed while fading controls, so the photo never jumps.
        for (View bar : new View[]{header, footer}) {
            bar.animate().cancel();
            bar.setVisibility(View.VISIBLE);
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) bar.animate().alpha(visible ? 1 : 0).setDuration(140)
                    .withEndAction(() -> bar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE)).start();
            else { bar.setAlpha(visible ? 1 : 0); bar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE); }
        }
    }

    private void clearStage() {
        request++;
        if (video != null) {
            videoMuted = video.muted(); videoLooping = video.looping(); videoRate = video.rate();
            video.dispose(); video = null;
        }
        if (photo != null) { photo.setImageBitmap(null); photo = null; }
        stage.removeAllViews();
        // Detached image draw commands can still reference their bitmap. Let GC own disposal.
        shownBitmap = null;
    }

    private void back() {
        if (grid && selecting) { selecting = false; selected.clear(); showGrid(); }
        else if (grid && !firstOpen && !items.isEmpty()) open(Math.min(index, items.size()-1));
        else finish();
    }
    @Override protected void onPause() {
        if (video != null) { resumePosition = video.position(); video.pause(); }
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("index", index); state.putBoolean("grid", grid);
        state.putBoolean("selecting", selecting);
        state.putStringArrayList("selection", new ArrayList<>(selected));
        state.putInt("video_position", video != null ? video.position() : resumePosition);
        state.putBoolean("video_muted", video != null ? video.muted() : videoMuted);
        state.putBoolean("video_looping", video != null ? video.looping() : videoLooping);
        state.putFloat("video_rate", video != null ? video.rate() : videoRate);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        clearStage(); images.shutdownNow(); thumbnails.shutdownNow(); cache.evictAll();
        main.removeCallbacksAndMessages(null); super.onDestroy();
    }
    private ImageButton icon(int drawable, String label, Runnable action) {
        ImageButton v = new ImageButton(this);
        v.setImageResource(drawable); v.setImageTintList(ColorStateList.valueOf(0xff262626));
        v.setBackgroundColor(Color.TRANSPARENT); v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setContentDescription(label); v.setOnClickListener(w -> action.run()); UiMotion.press(v); return v;
    }
    private TextView text(String value, int size, boolean bold) { return CameraUi.text(this, value, size, 0xff262626, bold); }
    private int dp(int value) { return CameraUi.dp(this, value); }
    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
}
