package cz.yozozchomutova.qrscanner;

import static android.content.Context.CAMERA_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.AttributeSet;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;

public class QrCameraView extends SurfaceView implements SurfaceHolder.Callback {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private CameraManager cameraManager;
    private String backCameraId;

    private Size[] sizesForJPEG;

    private QrScannerResult qrCallback;

    private BarcodeScannerOptions options;

    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private CameraCaptureSession session;

    //Flashlight data
    private boolean flashlightEnabled = false;

    public QrCameraView(Context context) {
        super(context);
        init();
    }

    public QrCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QrCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public QrCameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        Activity activity = unwrap(getContext());

        options = new BarcodeScannerOptions.Builder().setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_DATA_MATRIX)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient();

        try {
            cameraManager = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);
            backCameraId = getBackfacingCameraId(cameraManager);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;

            cameraManager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId);
                        StreamConfigurationMap config = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        sizesForJPEG = config.getOutputSizes(ImageFormat.JPEG);

                        int bestFitJPEG = 0;
                        for (int i = 0; i < sizesForJPEG.length; i++) {
                            if (sizesForJPEG[i].getWidth() <= 500 && sizesForJPEG[i].getHeight() <= 500) {
                                bestFitJPEG = i;
                                break;
                            }
                        }
                        imageReader = ImageReader.newInstance(sizesForJPEG[bestFitJPEG].getWidth(), sizesForJPEG[bestFitJPEG].getHeight(), ImageFormat.JPEG, 3);

                        List<Surface> surfaces = new ArrayList<>();
                        surfaces.add(getHolder().getSurface());
                        surfaces.add(imageReader.getSurface());

                        camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    QrCameraView.this.session = session;

                                    captureRequestBuilder = session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, flashlightEnabled ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
                                    captureRequestBuilder.addTarget(imageReader.getSurface());
                                    captureRequestBuilder.addTarget(getHolder().getSurface());

                                    session.setRepeatingRequest(captureRequestBuilder.build(), null, null);

                                    imageReader.setOnImageAvailableListener(imgReader -> {
                                        Image image = imgReader.acquireLatestImage();

                                        //Process image
                                        if (image != null) {
                                            InputImage inputImage = InputImage.fromMediaImage(image, getRotationCompensation(backCameraId, activity, false));
                                            Task<List<Barcode>> result = scanner.process(inputImage)
                                                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                                        @Override
                                                        public void onSuccess(List<Barcode> barcodes) {
                                                            if (barcodes.size() > 0)
                                                                qrCallback.onSuccess(barcodes.get(0).getRawValue());
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {

                                                        }
                                                    });
                                            image.close();
                                        }
                                    }, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                System.out.println("S: ");
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    System.out.println("D");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    System.out.println("E");
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        ConstraintLayout.LayoutParams surfaceViewLayoutParams = (ConstraintLayout.LayoutParams) getLayoutParams();
        surfaceViewLayoutParams.width = sizesForJPEG[0].getWidth();
        surfaceViewLayoutParams.height = sizesForJPEG[0].getHeight();
        setLayoutParams(surfaceViewLayoutParams);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    //Callback
    public void setQrCallback(QrScannerResult qrScannerResult) {
        this.qrCallback = qrScannerResult;
    }

    //Flashlight
    public void setFlashlightEnabled(boolean enabled) {
        flashlightEnabled = enabled;
        updateFlashlight();
    }

    public void toggleFlashlight() {
        flashlightEnabled = !flashlightEnabled;
        updateFlashlight();
    }

    public boolean isFlashlightEnabled() {
        return flashlightEnabled;
    }

    public void updateFlashlight() {
        try {
            captureRequestBuilder = session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, flashlightEnabled ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.addTarget(getHolder().getSurface());

            session.abortCaptures();
            session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //imported
    private String getBackfacingCameraId(CameraManager cameraManager){
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (int i = 0; i < ids.length; i++) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(ids[i]);
                int cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
                    return ids[i];
                }
            }
            return null;
        }
        catch(CameraAccessException ce){
            ce.printStackTrace();
            return null;
        }
    }

    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing) {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        try {
            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            int rotationCompensation = ORIENTATIONS.get(deviceRotation);

            // Get the device's sensor orientation.
            CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
            int sensorOrientation = cameraManager
                    .getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION);

            if (isFrontFacing) {
                rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
            } else { // back-facing
                rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
            }
            return rotationCompensation;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static Activity unwrap(Context context) {
        while (!(context instanceof Activity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        return (Activity) context;
    }
}
