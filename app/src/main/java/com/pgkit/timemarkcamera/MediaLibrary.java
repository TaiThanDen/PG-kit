package com.pgkit.timemarkcamera;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;
import androidx.exifinterface.media.ExifInterface;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** App camera media; queries/decodes are called only on the viewer's worker thread. */
final class MediaLibrary {
    static final class Item {
        final Uri uri;
        final String name;
        final boolean video;
        final long date;
        final int width;
        final int height;
        Item(Uri uri, String name, boolean video, long date, int width, int height) {
            this.uri = uri; this.name = name; this.video = video;
            this.date = date; this.width = width; this.height = height;
        }
    }

    static List<Item> load(Context context) {
        List<Item> items = new ArrayList<>();
        read(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, items);
        read(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, items);
        items.sort(Comparator.comparingLong((Item item) -> item.date).reversed()
                .thenComparing(item -> item.name, Comparator.reverseOrder()));
        return items;
    }

    private static void read(Context context, Uri collection, boolean video, List<Item> target) {
        String[] columns = {"_id", "_display_name", "date_added", "width", "height"};
        String selection = "_display_name LIKE ?";
        String[] args = {"TIMEMARK_%"};
        // Scoped storage exposes this app's own media without asking for broad library access.
        // On older Android restrict to the app's output directory as well as the filename.
        if (Build.VERSION.SDK_INT < 29) {
            selection += " AND (_data LIKE ? OR _data LIKE ?)";
            args = new String[]{"TIMEMARK_%", "%/TimemarkCamera/%", "%/DCIM/Camera/TIMEMARK_%"};
        }
        try (Cursor c = context.getContentResolver().query(collection, columns, selection, args, "date_added DESC")) {
            if (c == null) return;
            while (c.moveToNext()) {
                target.add(new Item(ContentUris.withAppendedId(collection, c.getLong(0)),
                        c.getString(1), video, c.getLong(2) * 1000L, c.getInt(3), c.getInt(4)));
            }
        }
    }

    static Bitmap thumbnail(Context context, Item item, int edge) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            return context.getContentResolver().loadThumbnail(item.uri, new Size(edge, edge), null);
        }
        if (item.video) {
            return MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                    ContentUris.parseId(item.uri), MediaStore.Video.Thumbnails.MINI_KIND, null);
        }
        return image(context, item.uri, edge);
    }

    static Bitmap image(Context context, Uri uri, int edge) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = resolver.openInputStream(uri)) { BitmapFactory.decodeStream(stream, null, options); }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw new IllegalArgumentException(context.getString(R.string.ui_invalid_photo));
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, edge);
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream stream = resolver.openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(stream, null, options); }
        if (bitmap == null) throw new IllegalArgumentException(context.getString(R.string.ui_could_not_read_photo));
        Matrix matrix = new Matrix();
        try (InputStream stream = resolver.openInputStream(uri)) {
            int orientation = new ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case 2: matrix.setScale(-1, 1); break;
                case 3: matrix.setRotate(180); break;
                case 4: matrix.setScale(1, -1); break;
                case 5: matrix.setRotate(90); matrix.postScale(-1, 1); break;
                case 6: matrix.setRotate(90); break;
                case 7: matrix.setRotate(-90); matrix.postScale(-1, 1); break;
                case 8: matrix.setRotate(-90); break;
                default: break;
            }
        } catch (java.io.IOException ignored) { /* JPEG without EXIF remains viewable. */ }
        if (!matrix.isIdentity()) {
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (oriented != bitmap) bitmap.recycle();
            return oriented;
        }
        return bitmap;
    }

    static int sampleSize(int width, int height, int target) {
        if (width <= 0 || height <= 0 || target <= 0) throw new IllegalArgumentException("Invalid image bounds");
        int sample = 1;
        while (Math.max(width, height) / sample > target * 2L) sample *= 2;
        return sample;
    }
}
