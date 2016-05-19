package io.puremetrics.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.puremetrics.sdk.PureMetrics;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PureMetrics.setUserName("Abhishek", "Nandi");
  }
}
