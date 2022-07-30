package cz.yozozchomutova.qrscannertest;

import android.app.Dialog;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.yozozchomutova.qrscanner.QrCameraView;
import cz.yozozchomutova.qrscanner.QrScannerResult;

public class DialogQrScanner extends Dialog {

    private QrCameraView qrc;

    public DialogQrScanner(@NonNull Context context) {
        super(context);
    }

    public DialogQrScanner(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected DialogQrScanner(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_qrscanner);

        setCanceledOnTouchOutside(false);

        qrc = findViewById(R.id.qcv);

        //Views
        findViewById(R.id.closeDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        findViewById(R.id.flsh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrc.toggleFlashlight();
            }
        });

        //Callback
        qrc.setQrCallback(new QrScannerResult() {
            @Override
            public void onSuccess(String result) {
                MainActivity.resultTV.setText(result);
                hide();
            }

            @Override
            public void onFail() {
                MainActivity.resultTV.setText("DialogQrScanner.java - Failed task...");
                hide();
            }
        });
    }
}
