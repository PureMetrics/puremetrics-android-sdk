package io.puremetrics.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import io.puremetrics.sdk.PureMetrics;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PureMetrics.trackSessionStart("app", null);

    PureMetrics.setReferralCode("vfmz10", null);
    PureMetrics.setReferrerCode("vfmz09", null);

    /* In an ideal world your app code would not look like this.*/
    TextView textView = (TextView) findViewById(R.id.textView);
    String language = "Device Language: " + Locale.getDefault().getLanguage();
    textView.setText(language);
    final HashMap<String, Object> extras = new HashMap<>();
    extras.put("txt_length", 50);
    extras.put("has_image", true);
    extras.put("personalized", true);

    PureMetrics.trackDeeplinkAttribution("facebook", "notification", "456789ao", "app://link", extras);

    final String transactionId = UUID.randomUUID().toString();
    HashMap<String, Object> meta = new HashMap<>();
    meta.put("loyalty", 10);
    meta.put("channel", "ad");

    HashMap<String, Object> productDim = new HashMap<>();
    productDim.put("genre", "hip-hop");
    productDim.put("category", "music");

    PureMetrics.trackEvent("ProductViewed", productDim);
    /* Building the order */
    final PureMetrics.Order.Builder orderBuilder = new PureMetrics.Order.Builder()
            .addProduct("SKUID1", productDim, 200, 200, 1, "USD")
            .setTransactionId(transactionId)
            .addMeta(meta);

    Button startTransaction = (Button) findViewById(R.id.button1);
    if (null != startTransaction) {
      startTransaction.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          /* Tracking transaction started*/
          PureMetrics.transactionStarted(orderBuilder.build());
        }
      });
    }

    /* Building a failed transaction data */
    final PureMetrics.FailedTransaction.Builder ftBuilder = new PureMetrics.FailedTransaction.Builder()
            .setTransactionId(transactionId)
            .setPaymentProviderTransactionId("sk_tok20890")
            .setCurrency("USD")
            .setAmount(200)
            .setFailureReason("INVALID CARD")
            .addMeta(meta);

    Button failedTransaction = (Button) findViewById(R.id.button2);
    if (null != failedTransaction) {
      failedTransaction.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          /* Tracking transaction failed*/
          PureMetrics.transactionFailed(ftBuilder.build());
        }
      });
    }

    /* Building a successful transaction object */
    final PureMetrics.Revenue.Builder revBuilder = new PureMetrics.Revenue.Builder()
            .setTransactionId(transactionId)
            .setPaymentProviderTransactionId("sk_tok20890")
            .setCurrency("USD")
            .setDiscount("APP1", 1)
            .addPayment("stripe", 199, 0)
            .addMeta(meta);

    Button successfulTransaction = (Button) findViewById(R.id.button3);
    if (null != successfulTransaction) {
      successfulTransaction.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          /* Tracking transaction successful*/
          PureMetrics.transactionSuccessful(revBuilder.build());
        }
      });
    }

    Button logout = (Button) findViewById(R.id.button4);
    logout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        PureMetrics.resetUserInfo();
      }
    });

    Button trackCustomEvent = (Button) findViewById(R.id.button5);
    trackCustomEvent.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("Meta", "Some Metadata");
        PureMetrics.trackEvent("customEvent", attrs);
      }
    });

    Button trackOnboarding = (Button) findViewById(R.id.button6);
    trackOnboarding.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        PureMetrics.trackOnboardingStep("test", 1, 1, "StepName", null, null);
      }
    });

    Button transactionCancelled = (Button) findViewById(R.id.button7);
    transactionCancelled.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        PureMetrics.trackOrderCancellation(transactionId, 0, null, extras);
      }
    });
  }
}
