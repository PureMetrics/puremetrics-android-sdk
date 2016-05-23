package io.puremetrics.example;

import android.app.Application;

import io.puremetrics.sdk.PureMetrics;

public class Appliction extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    PureMetrics.withBuilder()
            .setAppConfiguration("jk01u2kq2u6d3i2gf0pa71hf2p1463663065972", "76uehq09i4idr5n8dmqnjun0tm")
            .init(getApplicationContext());
  }
}
