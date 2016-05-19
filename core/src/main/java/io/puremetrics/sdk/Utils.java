/**
 * Modified MIT License
 * <p/>
 * Copyright 2016 PureMetrics
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by PureMetrics.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.puremetrics.sdk;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

final class Utils {

  private static final List<String> INVALID_PHONE_IDS = Arrays.asList("", "0", "unknown", "739463", "000000000000000", "111111111111111", "352005048247251", "012345678912345", "012345678901237",
          "88508850885050", "0123456789abcde", "004999010640000", "862280010599525", "52443443484950", "355195000000017", "001068000000006", "358673013795895", "355692547693084", "004400152020000",
          "8552502717594321", "113456798945455", "012379000772883", "111111111111119", "358701042909755", "358000043654134", "345630000000115", "356299046587760", "356591000000222");


  static String getCarrierName(Context appContext) {
    TelephonyManager manager = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
    String carrierName = manager.getNetworkOperatorName();
    return "".equals(carrierName) ? null : carrierName;
  }

  /**
   * Get a device identifier
   *
   * @param appContext An instance of the application {@link Context}
   * @return A string which uniquely identifies the device
   */
  static String getDeviceId(Context appContext) {
    String deviceId = null;
    if (isPhone(appContext)) {
      deviceId = getWifiMac(appContext);
      if (null == deviceId) {
        deviceId = getIMEI(appContext);
      }
    }
    if (null == deviceId) {
      deviceId = getAndroidId(appContext);
      if (null == deviceId) {
        deviceId = generateRandomId();
      }
    }

    return deviceId;
  }

  static String getIMEI(Context appContext) {
    if (SupportCompatV4.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE)) {
      try {
        TelephonyManager manager = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneId = manager.getDeviceId();
        if (phoneId != null && phoneId.length() > 0 && !INVALID_PHONE_IDS.contains(phoneId))
          return Constants.PREFIX_ID_IMEI + phoneId;
        return phoneId;
      } catch (Throwable e) {

      }
    }
    return null;
  }

  static boolean isPhone(Context appContext) {
    PackageManager packageManager = appContext.getPackageManager();
    return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
  }

  static String getWifiMac(Context appContext) {
    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      try {
        if (SupportCompatV4.checkSelfPermission(appContext, Manifest.permission.ACCESS_WIFI_STATE)) {
          String macAddress = ((WifiManager) appContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
          if (null != macAddress && macAddress.length() > 0) {
            return Constants.PREFIX_ID_MAC_ADDRESS + macAddress;
          }
        }
      } catch (RuntimeException e) {
      }
    }
    return null;
  }

  static String getAndroidId(Context appContext) {
    try {
      final String androidId = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
      // see http://code.google.com/p/android/issues/detail?id=10603 for info on this 'dup' id.
      if (null != androidId && androidId.length() > 0 && !androidId.equals("9774d56d682e549c")) {
        return Constants.PREFIX_ID_ANDROID_ID + androidId;
      }
    } catch (RuntimeException e) {
    }
    return null;
  }

  static String generateRandomId() {
    return Constants.PREFIX_ID_GENERATED + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
  }

  /**
   * Internal method to convert bytes to hex string
   *
   * @param bytes the bytes
   * @return the string
   */
  static String bytesToHexString(byte[] bytes) {
    final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'};
    char[] hexChars = new char[bytes.length * 2];
    int v;
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  static boolean uploadData(String authBytes, final String data) {

    try {
      if (null == data) {
        return true;
      }

      // MessageDigest.getInstance(String) is not threadsafe on Android.
      // See https://code.google.com/p/android/issues/detail?id=37937
      // Use MD5 implementation from http://org.rodage.com/pub/java/security/MD5.java
      // This implementation does not throw NoSuchAlgorithm exceptions.
      MessageDigest messageDigest = new MD5();

      byte[] digestedData = messageDigest.digest(data.getBytes("UTF-8"));

      // Create Hex String
      StringBuilder hexString = new StringBuilder();
      for (byte aMessageDigest : digestedData) {
        String h = Integer.toHexString(0xFF & aMessageDigest);
        while (h.length() < 2)
          h = "0" + h;
        hexString.append(h);
      }
      String checksumString = hexString.toString();
      PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "RequestBody: " + data + " | Checksum: " + checksumString + " | " + authBytes);
      return uploadDataInternal(authBytes, checksumString, data.getBytes("UTF-8"), 0);
    } catch (Throwable e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "Failed to upload data", e);
    }
    return false;
  }

  static boolean uploadDataInternal(String authBytes, String checksumString, byte[] data, int retryCount) throws IOException {
    boolean result = false;
    URL url = new URL("https://api.puremetrics.io/v1/track");
    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    urlConnection.setRequestProperty("Authorization", "basic " + authBytes);
    urlConnection.setRequestProperty("Content-MD5", checksumString);
    urlConnection.setRequestProperty("Content-Type", "application/json;");
    urlConnection.setRequestMethod("POST");
    urlConnection.setRequestProperty("Connection", "close");

    urlConnection.setDoOutput(true);
    urlConnection.setUseCaches(false);
    urlConnection.setConnectTimeout(13000);
    urlConnection.setReadTimeout(13000);

    OutputStream outputStream = urlConnection.getOutputStream();
    outputStream.write(data);
    int responseCode = urlConnection.getResponseCode();
    String responseMessage = urlConnection.getResponseMessage();
    if (responseCode == 200) {
      PureMetrics.logAPIResponse("Upload API", responseCode, null, responseMessage);
      return true;
    } else if (responseCode == 412) {
      PureMetrics.logAPIResponse("Upload API", responseCode, null, responseMessage);
      if (retryCount < 2) {
        result = uploadDataInternal(authBytes, checksumString, data, ++retryCount);
      }
    }
    return result;
  }
}
