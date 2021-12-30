package com.sydien.blindreader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ImageProcessorTesseract implements Runnable {
    private final Activity context;
    private final ImageReader mImageReader;
    private final TessBaseAPI mTessBase;
    private final TextView tv_OCRText;
    private final TextToSpeech mTextToSpeech;

    public ImageProcessorTesseract(Activity c, ImageReader mImageReader, TessBaseAPI recognizer, TextView ocrView, TextToSpeech textToSpeech) {
        this.context = c;
        this.mImageReader = mImageReader;
        this.mTessBase = recognizer;
        this.tv_OCRText = ocrView;
        this.mTextToSpeech = textToSpeech;
    }

    @Override
    public void run() {
        final Image image = mImageReader.acquireLatestImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        try{
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            saveImage(bitmap, "TMP");

            mTessBase.setImage(bitmap);
            String ocrResult = mTessBase.getUTF8Text();
            if (ocrResult == null) {
                ocrResult = Configs.TESS_NO_TEXT;
            }
            Log.d("TESSTEXT", ocrResult);
            String finalOcrResult = ocrResult;
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_OCRText.setText(finalOcrResult);
                }
            });
            if (mTextToSpeech.isSpeaking()) {
                mTextToSpeech.stop();
            }
            mTextToSpeech.speak(ocrResult, TextToSpeech.QUEUE_FLUSH, null);
        } finally {
            image.close();
            buffer.clear();
        }
    }

    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Log.d("SAVEDBITMAP", file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
