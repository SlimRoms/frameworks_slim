/*
* Copyright (C) 2013-2017 SlimRoms Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package slim.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.TypedValue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHelper {

    public static final String ACTION_IMAGE_PICKED = "slim.intent.action.ACTION_IMAGE_PICKED";

    private static File sFolder = new File(Environment.getExternalStorageDirectory() +
                File.separator + ".slim" + File.separator + "icons");

    static {
        if (!sFolder.exists()) {
            sFolder.mkdirs();
        }
    }

    public static File getIconFolder() {
        return sFolder;
    }

    public static Drawable getColoredDrawable(Drawable d, int color) {
        if (d == null) {
            return null;
        }
        if (d instanceof VectorDrawable) {
            d.setTint(color);
            return d;
        }
        Bitmap colorBitmap = ((BitmapDrawable) d).getBitmap();
        Bitmap grayscaleBitmap = toGrayscale(colorBitmap);
        Paint pp = new Paint();
        pp.setAntiAlias(true);
        PorterDuffColorFilter frontFilter =
            new PorterDuffColorFilter(color, Mode.MULTIPLY);
        pp.setColorFilter(frontFilter);
        Canvas cc = new Canvas(grayscaleBitmap);
        final Rect rect = new Rect(0, 0, grayscaleBitmap.getWidth(), grayscaleBitmap.getHeight());
        cc.drawBitmap(grayscaleBitmap, rect, rect, pp);
        return new BitmapDrawable(grayscaleBitmap);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap drawableToShortcutIconBitmap (
            Context context, Drawable drawable, int dp) {
        if (drawable == null) {
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int size = Converter.dpToPx(context, dp);

        // ensure that the drawable is not larger than target size
        while (size < drawable.getIntrinsicHeight()
                || size < drawable.getIntrinsicWidth()) {
            size = size + 12;
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds((size - drawable.getIntrinsicWidth()) / 2,
                (size - drawable.getIntrinsicHeight()) / 2,
                (size + drawable.getIntrinsicWidth()) / 2,
                (size + drawable.getIntrinsicHeight()) / 2);
        drawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        ColorMatrix cm = new ColorMatrix();
        final Rect rect = new Rect(0, 0, width, height);
        cm.setSaturation(0);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, rect, rect, paint);
        return bmpGrayscale;
    }

    public static Drawable resize(Context context, Drawable image, int size) {
        if (image == null || context == null) {
            return null;
        }
        if (image instanceof VectorDrawable) {
            return image;
        } else {
            int newSize = Converter.dpToPx(context, size);
            Bitmap bitmap = ((BitmapDrawable) image).getBitmap();
            Bitmap scaledBitmap = Bitmap.createBitmap(newSize, newSize, Config.ARGB_8888);

            float ratioX = newSize / (float) bitmap.getWidth();
            float ratioY = newSize / (float) bitmap.getHeight();
            float middleX = newSize / 2.0f;
            float middleY = newSize / 2.0f;

            final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            paint.setAntiAlias(true);

            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

            Canvas canvas = new Canvas(scaledBitmap);
            canvas.setMatrix(scaleMatrix);
            canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2,
                    middleY - bitmap.getHeight() / 2, paint);
            return new BitmapDrawable(context.getResources(), scaledBitmap);
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 24;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(width, height,
                Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        BitmapShader shader = new BitmapShader(bitmap,  TileMode.CLAMP, TileMode.CLAMP);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        canvas.drawCircle(width/2, height/2, width/2, paint);

        return output;
    }

    /**
     * @param context callers context
     * @param uri Uri to handle
     * @return A bitmap from the requested uri
     * @throws IOException
     *
     * @Credit: StackOverflow
     *             http://stackoverflow.com/questions/35909008/pick-image
     *             -from-gallery-or-google-photos-failing
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        if (context == null || uri == null) {
            return null;
        }
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, new Rect(), options);
        options.inSampleSize = calculateInSampleSize(options, 100, 100);
        options.inJustDecodeBounds = false;
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor, new Rect(), options);
        parcelFileDescriptor.close();
        return image;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Uri saveImageFile(File image) {
        File imageFile = new File(sFolder.getAbsolutePath() + File.separator
                + "slim_" + System.currentTimeMillis() + ".png");
        image.renameTo(imageFile);
        imageFile.setReadable(true, false);
        return Uri.fromFile(imageFile);
    }

    public static Uri addBitmapToStorage(Bitmap b) {
        if (b == null) return null;
        File imageFile = new File(sFolder.getAbsolutePath() + File.separator
                + "slim_" + System.currentTimeMillis() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            return null;
        }
        return Uri.fromFile(imageFile);
    }
}
