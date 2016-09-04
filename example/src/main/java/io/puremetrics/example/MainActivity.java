package io.puremetrics.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.UUID;

import io.puremetrics.sdk.PureMetrics;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    /* In an ideal world your app code would not look like this.*/

    String transactionId = UUID.randomUUID().toString();
    HashMap<String, Object> meta = new HashMap<>();
    meta.put("loyalty", 10);
    meta.put("channel", "ad");

    HashMap<String, Object> productDim = new HashMap<>();
    productDim.put("genre", "hip-hop");
    productDim.put("category", "music");

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
  }
}
