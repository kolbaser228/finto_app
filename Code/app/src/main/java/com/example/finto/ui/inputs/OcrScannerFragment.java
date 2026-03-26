package com.example.finto.ui.inputs;
import android.app.AlertDialog;

import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finto.data.local.AppDatabase;
import com.example.finto.data.remote.AiApiClient;
import com.example.finto.data.remote.AiApiClientImpl;
import com.example.finto.data.repository.OcrRepository;
import com.example.finto.R;
import com.example.finto.data.repository.ParsedItem;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class OcrScannerFragment extends Fragment {

    private PreviewView previewView;
    private TextView tvScanningStatus;
    private Button btnCapture;

    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    private boolean isProcessing = false;
    private boolean takePhotoFlag = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ocr_scanner, container, false);
        previewView = view.findViewById(R.id.previewView);
        tvScanningStatus = view.findViewById(R.id.tvScanningStatus);
        btnCapture = view.findViewById(R.id.btnCapture);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        btnCapture.setOnClickListener(v -> {
            if (!isProcessing) {
                takePhotoFlag = true;
                tvScanningStatus.setText("Обробка ШІ...");
                btnCapture.setEnabled(false);
            }
        });

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("OcrScanner", "Помилка ініціалізації камери", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        if (takePhotoFlag) {
            takePhotoFlag = false;
            isProcessing = true;

            try {
                Bitmap bitmap = imageProxy.toBitmap();
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                final Bitmap finalBitmap = bitmap;

                InputImage image = InputImage.fromBitmap(finalBitmap, 0);

                textRecognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            String rawText = visionText.getText();

                            if (!rawText.isEmpty() && rawText.length() > 10) {

                                tvScanningStatus.setText("Текст знайдено! Відправка до ШІ...");

                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                                    byte[] imageBytes = baos.toByteArray();
                                    String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                                    sendImageToAi(base64Image);
                                });

                            } else {
                                requireActivity().runOnUiThread(() -> {
                                    tvScanningStatus.setText("Текст не знайдено. Наведіть краще.");
                                    btnCapture.setEnabled(true);
                                    isProcessing = false;
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("OcrScanner", "Помилка ML Kit", e);
                            requireActivity().runOnUiThread(() -> {
                                tvScanningStatus.setText("Помилка камери. Спробуйте ще.");
                                btnCapture.setEnabled(true);
                                isProcessing = false;
                            });
                        })
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                        });

            } catch (Exception e) {
                e.printStackTrace();
                imageProxy.close();
                requireActivity().runOnUiThread(() -> {
                    btnCapture.setEnabled(true);
                    isProcessing = false;
                });
            }
        } else {
            imageProxy.close();
        }
    }

    private void sendImageToAi(String base64Image) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AiApiClient aiClient = new AiApiClientImpl();
        OcrRepository repo = new OcrRepository(db.transactionDao(), aiClient);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<ParsedItem> parsedItems = repo.analyzeReceiptImage(base64Image);

                requireActivity().runOnUiThread(() -> {
                    showVerificationDialog(parsedItems, repo);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Помилка обробки.";
                    tvScanningStatus.setText(errorMessage);

                    tvScanningStatus.postDelayed(() -> {
                        tvScanningStatus.setText("Наведіть камеру на чек");
                        btnCapture.setEnabled(true);
                        isProcessing = false;
                    }, 3500);
                });
            }
        });
    }

    private void showVerificationDialog(List<ParsedItem> items, OcrRepository repo) {
        tvScanningStatus.setText("Перевірте дані");

        StringBuilder sb = new StringBuilder();
        double totalSum = 0;
        for (ParsedItem item : items) {
            sb.append("• ").append(item.comment)
                    .append(" — ").append(item.amount).append(" грн").append("\n");
            totalSum += item.amount;
        }
        sb.append("\nЗагальна сума: $").append(String.format("%.2f", totalSum));

        new AlertDialog.Builder(requireContext())
                .setTitle("Розпізнано з чека")
                .setMessage(sb.toString())
                .setCancelable(false)
                .setPositiveButton("Зберегти", (dialog, which) -> {
                    tvScanningStatus.setText("Збереження...");
                    btnCapture.setEnabled(false);

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        repo.saveTransactions(items);

                        requireActivity().runOnUiThread(() -> {
                            tvScanningStatus.setText("Успішно збережено!");
                            tvScanningStatus.postDelayed(() -> {
                                tvScanningStatus.setText("Наведіть камеру на чек");
                                btnCapture.setEnabled(true);
                                isProcessing = false;
                            }, 2000);
                        });
                    });
                })
                .setNegativeButton("Скасувати", (dialog, which) -> {
                    tvScanningStatus.setText("Скасовано");
                    tvScanningStatus.postDelayed(() -> {
                        tvScanningStatus.setText("Наведіть камеру на чек");
                        btnCapture.setEnabled(true);
                        isProcessing = false;
                    }, 1000);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }
}