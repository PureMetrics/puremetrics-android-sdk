/**
 * Modified MIT License
 * <p>
 * Copyright 2016 PureMetrics
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by PureMetrics.
 * <p>
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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    if (hasTelephoneFeature(appContext)) {
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
        return null;
      } catch (Throwable e) {
        //intentionally suppressed
      }
    }
    return null;
  }

  static boolean hasTelephoneFeature(Context appContext) {
    PackageManager packageManager = appContext.getPackageManager();
    return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
  }

  @SuppressWarnings({"MissingPermission"})
  static String getWifiMac(Context appContext) {
    try {
      if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        if (SupportCompatV4.checkSelfPermission(appContext, Manifest.permission.ACCESS_WIFI_STATE)) {
          String macAddress = ((WifiManager) appContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
          if (null != macAddress && macAddress.length() > 0) {
            return Constants.PREFIX_ID_MAC_ADDRESS + macAddress;
          }
        }
      }
    } catch (RuntimeException e) {
      //intentionally suppressed
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
      //intentionally suppressed
    }
    return null;
  }

  static String generateRandomId() {
    return Constants.PREFIX_ID_GENERATED + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
  }

  static boolean uploadData(String authBytes, final String data, boolean isDebug) {

    try {
      if (null == data) {
        return true;
      }

      // MessageDigest.getInstance(String) is not threadsafe on Android.
      // See https://code.google.com/p/android/issues/detail?id=37937
      // Use MD5 implementation from http://org.rodage.com/pub/java/security/MD5.java
      // This implementation does not throw NoSuchAlgorithm exceptions.
      MessageDigest messageDigest = new MD5();
      byte[] undigested = data.getBytes("UTF-8");
      byte[] digestedData = messageDigest.digest(undigested);

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
      return uploadDataInternal(authBytes, checksumString, undigested, 0, isDebug);
    } catch (Throwable e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "Failed to upload data", e);
    }
    return false;
  }

  static boolean uploadDataInternal(String authBytes, String checksumString, byte[] data, int retryCount, boolean isDebug) throws IOException {
    boolean result = false;
    URL url = new URL("https://api.puremetrics.io/v1/track");
    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    urlConnection.setRequestProperty(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BASIC_AUTH_PREFIX + authBytes);
    urlConnection.setRequestProperty(Constants.HEADER_CONTENTMD5, checksumString);
    urlConnection.setRequestProperty(Constants.HEADER_CONTENT_TYPE, Constants.HEADER_CONTENT_TYPE_VALUE);
    urlConnection.setRequestProperty(Constants.HEADER_CONNECTION, Constants.HEADER_CLOSE);
    if (isDebug) {
      urlConnection.setRequestProperty(Constants.HEADER_DEBUG, Constants.HEADER_DEBUG_VALUE);
    }
    urlConnection.setRequestMethod(Constants.REQUEST_METHOD_POST);

    urlConnection.setDoOutput(true);
    urlConnection.setUseCaches(false);
    urlConnection.setConnectTimeout(13000);
    urlConnection.setReadTimeout(13000);

    OutputStream outputStream = urlConnection.getOutputStream();
    outputStream.write(data);
    int responseCode = urlConnection.getResponseCode();
    String responseMessage = urlConnection.getResponseMessage();
    if (responseCode == 200 || responseCode == 204) {
      PureMetrics.logAPIResponse("Upload API", responseCode, null, responseMessage);
      return true;
    } else if (responseCode == 412) {
      PureMetrics.logAPIResponse("Upload API", responseCode, null, responseMessage);
      if (retryCount < 2) {
        result = uploadDataInternal(authBytes, checksumString, data, ++retryCount, isDebug);
      }
    }
    return result;
  }

  static void trackAdvertisementIdIfPossible(Context appContext) {
    // This should not be called on the main thread.
    try {
      Class AdvertisingIdClient = Class
              .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
      Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo",
              Context.class);
      Object advertisingInfo = getAdvertisingInfo.invoke(null, appContext);
      Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod(
              "isLimitAdTrackingEnabled");
      Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled
              .invoke(advertisingInfo);
      if (limitAdTrackingEnabled != null && limitAdTrackingEnabled == Boolean.TRUE) {
        PureMetrics.trackDeviceProperties(Constants.DA_LAT, true);
      } else {
        PureMetrics.trackDeviceProperties(Constants.DA_LAT, false);
      }
      Method getId = advertisingInfo.getClass().getMethod("getId");
      PureMetrics.trackDeviceProperties(Constants.DA_GAID, (String) getId.invoke(advertisingInfo));
    } catch (ClassNotFoundException e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.WARN, "Google Play Services SDK not found!");
    } catch (InvocationTargetException e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.WARN, "Google Play Services not available");
    } catch (Throwable e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.WARN, "Encountered an error connecting to Google Play Services", e);
    }
  }

  /**
   * Get the class of connectivity
   *
   * @param context Application {@link Context}
   * @return a string representing the network class
   */
  public static String getNetworkClass(Context context) {
    try {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo info = cm.getActiveNetworkInfo();
      if (info == null || !info.isConnected())
        return "not_connected"; //not connected
      if (info.getType() == ConnectivityManager.TYPE_WIFI)
        return "wifi";
      if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
        int networkType = info.getSubtype();
        switch (networkType) {
          case TelephonyManager.NETWORK_TYPE_GPRS:
          case TelephonyManager.NETWORK_TYPE_EDGE:
          case TelephonyManager.NETWORK_TYPE_CDMA:
          case TelephonyManager.NETWORK_TYPE_1xRTT:
          case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
            return "2g";
          case TelephonyManager.NETWORK_TYPE_UMTS:
          case TelephonyManager.NETWORK_TYPE_EVDO_0:
          case TelephonyManager.NETWORK_TYPE_EVDO_A:
          case TelephonyManager.NETWORK_TYPE_HSDPA:
          case TelephonyManager.NETWORK_TYPE_HSUPA:
          case TelephonyManager.NETWORK_TYPE_HSPA:
          case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
          case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
          case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
            return "3g";
          case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
            return "4g";
          default:
            return "unknown";
        }
      }
    } catch (Throwable e) {
      //intentionally suppressed
    }
    return "unknown";
  }

  /**
   * Enable the network change listener component.
   * This is required when some data is pending to be synced.
   *
   * @param appContext An instance of the application {@link Context}
   */
  static void enableNetworkListener(Context appContext) {
    ComponentName receiver = new ComponentName(appContext, NetworkAvailableReceiver.class);
    PackageManager pm = appContext.getPackageManager();
    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Enabling network change listener");
  }

  /**
   * Disables the network change listener component.
   * This is required when everything has been synced and nothing is pending to be synced
   *
   * @param appContext An instance of the application {@link Context}
   */
  static void disableNetworkListener(Context appContext) {
    ComponentName receiver = new ComponentName(appContext, NetworkAvailableReceiver.class);
    PackageManager pm = appContext.getPackageManager();
    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Disabling network change listener");
  }

  /**
   * Determince whether this is DEBUG build or production build
   *
   * @param context An instance of the application {@link Context}
   * @return true if the app is in debug mode, false otherwise
   */
  static boolean isDebugBuild(Context context) {
    return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
  }

  /**
   * Determine the device type
   *
   * @param context An instance of the application {@link Context}
   * @return returns "tablet" or "phone"
   */
  static String getDeviceType(Context context) {
    return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE ? "tablet" : "phone";
  }
}
