package io.puremetrics.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;

import io.puremetrics.sdk.PureMetrics;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PureMetrics.trackRevenue(100f, "usd", "nb", 0, null, 1, null);
    HashMap<String, Object> attrs = new HashMap<>();
    attrs.put("productId", 123456);
    attrs.put("city", "aha");
    PureMetrics.trackRevenue(100f, "usd", "nb", 0, null, 1, attrs);
    Bundle extras = new Bundle();
    extras.putString("referrer", "IDE");
    extras.putString("campaign", "test");
    PureMetrics.setAppOpenExtras(null, null, "test", null, "https://www.puremetrics.io", attrs);
  }
}
