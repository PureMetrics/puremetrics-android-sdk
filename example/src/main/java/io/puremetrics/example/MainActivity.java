package io.puremetrics.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.HashMap;

import io.puremetrics.sdk.PureMetrics;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PureMetrics.trackTransactionStarted(null, null);
      }
    });

    findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        PureMetrics.trackTransactionFailed(null, "Payment Issue", null);
      }
    });

    findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        HashMap<String, Object> attrs = new HashMap<>();
        attrs.put("productid", 123456);
        attrs.put("category", "music");
        PureMetrics.trackTransactionSuccessful(null, 100f, "usd", "nb", 20, "FLAT20", 1, attrs);
      }
    });
  }
}
