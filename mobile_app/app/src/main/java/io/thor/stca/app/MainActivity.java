package io.thor.stca.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;


public class MainActivity extends AppCompatActivity {
    private final static int SCANNER_ACTIVITY = 116;

    private TextView mTextView;
    private ImageView mImageView;
    private FloatingActionButton mButtonScan;

    private String lastScannedBarcode = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Ion.getDefault(this).getConscryptMiddleware().enable(false);

        mImageView = findViewById(R.id.imageView);
        mTextView = findViewById(R.id.textView);

        Typeface custom_font = Typeface.createFromAsset(getAssets(),  "font.ttf");
        mTextView.setTypeface(custom_font);

        mButtonScan = findViewById(R.id.buttonScan);
        mButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBarcodeScan();
            }
        });
    }

    private void startBarcodeScan() {
        startActivityForResult(new Intent(MainActivity.this, ScannerAcitivity.class), SCANNER_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCANNER_ACTIVITY && resultCode == RESULT_OK) {
            String barcode = data.getStringExtra("data");

            if (barcode != null) {
                lastScannedBarcode = barcode;

                startFingerPrint();
            }
        }
    }

    private void startFingerPrint() {
        FingerprintManagerCompat manager = FingerprintManagerCompat.from(this);

        if (manager.isHardwareDetected() && manager.hasEnrolledFingerprints()) {
            showFingerPrintAuth();
        } else {
            Toast.makeText(this, "Fingerprint authentication is not supported!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFingerPrintAuth() {
        final CancellationSignal mCancellationSignal = new CancellationSignal();

        new BiometricPrompt.Builder(this)
                .setTitle("Biometrics")
                .setSubtitle("")
                .setDescription("")
                .setNegativeButton("Cancel", this.getMainExecutor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .build()
                .authenticate(mCancellationSignal, this.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        mCancellationSignal.cancel();
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        sendDataToServer();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        mCancellationSignal.cancel();

                        Toast.makeText(MainActivity.this, "Wrong fingerprint!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void sendDataToServer() {
        String barcode = lastScannedBarcode;

        String deviceKey = KeyManager.get(this).getDeviceKey();
        String totpKey = KeyManager.get(this).getOneTimeKey();

        int middle = barcode.lastIndexOf(';');

        if (middle != -1) {
            final String loginUrl = barcode.substring(0, middle);
            String pairKey = barcode.substring(middle + 1);

            NetworkManager.sendData(this, deviceKey, loginUrl, pairKey, totpKey, new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    if (e == null) {
                        Toast.makeText(MainActivity.this, "Login Success!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Login Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
