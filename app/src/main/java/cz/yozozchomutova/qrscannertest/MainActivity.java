package cz.yozozchomutova.qrscannertest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Button testBtn;
    public static TextView resultTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testBtn = findViewById(R.id.test);
        resultTV = findViewById(R.id.result);

        testBtn.setOnClickListener(v -> {
            DialogQrScanner d = new DialogQrScanner(MainActivity.this);
            d.show();
        });
    }
}