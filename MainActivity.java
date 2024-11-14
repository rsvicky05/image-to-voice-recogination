package com.example.voiceeye;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "TextReaderApp";

    private ImageView imageView;
    private TextToSpeech textToSpeech;
    private Bitmap capturedImage;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureButton = findViewById(R.id.button_capture);
        Button readTextButton = findViewById(R.id.button_read_text);
        imageView = findViewById(R.id.imageView);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.ENGLISH);
            } else {
                Log.e(TAG, "Text-to-Speech initialization failed");
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize TextRecognizer with default options
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        captureButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        });

        readTextButton.setOnClickListener(v -> {
            if (capturedImage != null) {
                processImage();
            } else {
                Log.w(TAG, "No image captured to process");
                Toast.makeText(this, "Please capture an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                capturedImage = (Bitmap) data.getExtras().get("data");
                if (capturedImage != null) {
                    imageView.setImageBitmap(capturedImage);
                } else {
                    Log.e(TAG, "Captured image is null");
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Intent data or extras is null");
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImage() {
        try {
            InputImage image = InputImage.fromBitmap(capturedImage, 0);
            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        StringBuilder recognizedText = new StringBuilder();
                        for (Text.TextBlock block : result.getTextBlocks()) {
                            recognizedText.append(block.getText()).append("\n");
                        }
                        String finalText = recognizedText.toString();
                        Log.d(TAG, "Recognized text: " + finalText);
                        if (!finalText.isEmpty()) {
                            speak(finalText);
                        } else {
                            Log.w(TAG, "No text found in the image");
                            Toast.makeText(this, "No text found in the image", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Text recognition failed", e);
                        Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.e(TAG, "Text-to-Speech failed: Text is empty or TTS not initialized");
            Toast.makeText(this, "Failed to read text", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
