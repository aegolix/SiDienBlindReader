package com.sydien.blindreader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;

import java.nio.ByteBuffer;

public class ImageProcessorMLKit implements Runnable {
    private final ImageReader mImageReader;
    private final TextRecognizer mTextRecognizer;
    private final TextView tv_OCRText;
    private final TextToSpeech mTextToSpeech;

    public ImageProcessorMLKit(ImageReader mImageReader, TextRecognizer recognizer, TextView ocrView, TextToSpeech textToSpeech) {
        this.mImageReader = mImageReader;
        this.mTextRecognizer = recognizer;
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
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
            Task<Text> result =
                    mTextRecognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text visionText) {
                                    // Task completed successfully
                                    String ocrResult = visionText.getText();
                                    Log.d("OCR", ocrResult);
                                    tv_OCRText.setText(ocrResult);
                                    if (mTextToSpeech.isSpeaking()) {
                                        mTextToSpeech.stop();
                                    }
                                    mTextToSpeech.speak(ocrResult, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            })
                            .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.d("OCR-Error", e.getMessage());
                                    }
                                })
                            .addOnCompleteListener(new OnCompleteListener<Text>() {
                                @Override
                                public void onComplete(@NonNull Task<Text> task) {
                                    image.close();
                                    buffer.clear();
                                }
                            });
        } finally {
            image.close();
            buffer.clear();
        }
    }
}
