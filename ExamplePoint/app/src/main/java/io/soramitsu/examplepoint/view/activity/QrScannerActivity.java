package io.soramitsu.examplepoint.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

import io.soramitsu.examplepoint.R;

public class QrScannerActivity extends AppCompatActivity {

    public static final String EXTRA_QR_TEXT = "io.soramitsu.examplepoint.EXTRA_QR_TEXT";
    private static final int CAMERA_PERMISSION_REQUEST = 2001;

    private DecoratedBarcodeView barcodeView;
    private boolean hasCameraPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        barcodeView = findViewById(R.id.barcode_scanner);
        findViewById(R.id.close_button).setOnClickListener(v -> finish());
        barcodeView.decodeContinuous(callback);
        ensureCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasCameraPermission) {
            barcodeView.resume();
        } else {
            ensureCameraPermission();
        }
    }

    @Override
    protected void onPause() {
        barcodeView.pause();
        super.onPause();
    }

    private void ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true;
            barcodeView.resume();
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result == null || result.getText() == null) {
                return;
            }
            barcodeView.pause();
            deliverResult(result.getText());
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // no-op
        }
    };

    private void deliverResult(String text) {
        Intent data = new Intent();
        data.putExtra(EXTRA_QR_TEXT, text);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasCameraPermission = true;
                barcodeView.resume();
            } else {
                Toast.makeText(this, R.string.error_camera_permission_required, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}
