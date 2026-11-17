package com.pgkit.timemarkcamera;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Metadata and JPEG quality for the captured, pixel-normalized photo. */
final class PhotoSaveMetadata {
    private static final String[] CAMERA_TAGS = {
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
            ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FLASH, ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP, ExifInterface.TAG_GPS_TIMESTAMP
    };

    private PhotoSaveMetadata() { }

    static int jpegQuality(int width, int height) {
        int longest = Math.max(width, height);
        return longest >= 4000 ? 95 : longest > 1600 ? 99 : 90;
    }

    static void write(File rawFile, ExifInterface destination, Date capturedAt,
                      int width, int height) throws IOException {
        ExifInterface source = new ExifInterface(rawFile.getAbsolutePath());
        for (String tag : CAMERA_TAGS) {
            String value = source.getAttribute(tag);
            if (value != null) destination.setAttribute(tag, value);
        }
        double[] location = source.getLatLong();
        if (location != null) destination.setLatLong(location[0], location[1]);
        String date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(capturedAt);
        String offset = new SimpleDateFormat("XXX", Locale.US).format(capturedAt);
        destination.setAttribute(ExifInterface.TAG_DATETIME, date);
        destination.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, date);
        destination.setAttribute(ExifInterface.TAG_OFFSET_TIME, offset);
        destination.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset);
        destination.setAttribute(ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_NORMAL));
        destination.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, Integer.toString(width));
        destination.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, Integer.toString(height));
        destination.saveAttributes();
    }
}
