package cz.yozozchomutova.qrscanner;

public interface QrScannerResult {
    void onSuccess(String result);

    void onFail();
}
