package com.example.finto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finto.ui.analytics.AnalyticsFragment;
import com.example.finto.ui.dashboard.DashboardFragment;
import com.example.finto.ui.inputs.OcrScannerFragment;
import com.example.finto.ui.inputs.ManualInputFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_ocr_input) {
                checkCameraPermissionAndStartScanner();
                return true;
            } else if (itemId == R.id.nav_manual_input) {
                 selectedFragment = new ManualInputFragment(); // Розкоментуєте, коли створите екран ручного вводу
            } else if (itemId == R.id.nav_analytics) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AnalyticsFragment())
                        .commit();
                return true;
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void checkCameraPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            loadFragment(new OcrScannerFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFragment(new OcrScannerFragment());
            } else {
                Toast.makeText(this, "Потрібен дозвіл на камеру для сканування чеків", Toast.LENGTH_LONG).show();
            }
        }
    }
}