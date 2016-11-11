package io.puremetrics.example;

import android.app.Application;

import io.puremetrics.sdk.PureMetrics;


public class Appliction extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    PureMetrics.withBuilder()
            .setAppConfiguration("8mscch353r78tevl563c44hfcb1464000940180", "v6mtbjhasegmr4eu1sdcpk6sb1")
            .setLoggingLevel(PureMetrics.LOG_LEVEL.DEBUG)
            //.disableAutoTracking(true)
            .init(getApplicationContext());
  }
}
