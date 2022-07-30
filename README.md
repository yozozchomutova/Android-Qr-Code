# Android-Qr-Code

[![](https://jitpack.io/v/yozozchomutova/Android-Qr-Code.svg)](https://jitpack.io/#yozozchomutova/Android-Qr-Code)

Library for scanning QR codes, Barcodes, Data matrixes, etc... Library uses ```android.hardware.camera2.*``` for camera and ```Google ML Kit``` dependency for processing camera output. Library is written in Java, however it should work for even for Kotlin and Compose.

This project is new and still needs to be alot improved.

## How to implement
1. Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
    ...
      maven { url 'https://jitpack.io' }
    }
}    
```    
    
2. Add dependency:
``` 
dependencies {
    implementation 'com.github.yozozchomutova:Android-Qr-Code:Tag'
}
``` 

## Usage
1. Add *QrCameraView* to your layout
```xml
<cz.yozozchomutova.qrscanner.QrCameraView
        android:id="@+id/qr_camera_view"
        android:layout_width="match_parent"
        android:layout_height="300dp" />
``` 

2. Fetch and modify your *QrCameraView* in your layout class
```java
QrCameraView qr_camera_view = findViewById(R.id.qr_camera_view);

//Modify your QrCameraView here. For ex.: qr_camera_view.setFlashlightEnabled(true);

qr_camera_view.setQrCallback(new QrScannerResult() {
      @Override public void onSuccess(String result) {
          System.out.println(result);
      }

      @Override public void onFail() {
          //TODO implement
      }
});
```

And that's it!

## Example
For example, visit [here](https://github.com/yozozchomutova/Android-Qr-Code/tree/main/app/src/main/java/cz/yozozchomutova/qrscannertest).
