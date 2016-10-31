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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import com.facebook.device.yearclass.YearClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A helper class to interface with PureMetrics
 */
public final class PureMetrics {

  /**
   * A string tag used for logging
   */
  private static final String TAG = "PureMetrics";
  /**
   * The singleton instance of {@link PureMetrics}
   */
  static PureMetrics _INSTANCE;
  /**
   * Disables auto tracking of sessions. No session related events will be tracked.
   * It has to be called implemented by the app
   */
  private static boolean AUTO_TRACKING_ENABLED = true;
  /**
   * The session duration
   */
  private static long _SESSION_DURATION = Constants.DEFAULT_SESSION_DURATION;
  /**
   * A boolean which denotes whether upload is in progress or not
   */
  private static boolean _UPLOAD_IN_PROGRESS = false;
  /**
   * Reset user information after upload. This is required when the user logs out
   */
  private static boolean _RESET_AFTER_UPLOAD = false;
  /**
   * An insternal instance of {@link Builder} but this is set to NULL later
   * on since it is not required always
   */
  private static Builder mBuilder;
  /**
   * Currently set logging level for the SDK
   */
  private static LOG_LEVEL logLevel = LOG_LEVEL.WARN;
  /**
   * Maintains a counter for the current active activities
   */
  private static int ACTIVITY_COUNTER = 0;
  /**
   * Temporary boolean used in cases where SDK
   * has not been initialized but the method has been called
   */
  private static boolean oldUser = false;
  /**
   * A mutex used for locking all {@link #preferences} operations
   */
  private final Object lock_sharedPref = new Object();
  /**
   * Current session Id
   */
  long sessionId;
  /**
   * An instance of the application {@link Context}
   */
  private Context appContext;
  /**
   * An instance of the {@link DBHelper}
   */
  private DBHelper databaseHelper;
  /**
   * Authorization Bytes to be added for Http BASIC Auth
   */
  private String authBytes;
  /**
   * The {@link SharedPreferences} which will be used
   */
  private SharedPreferences preferences;
  /**
   * Constructor
   *
   * @param context   An instance of the application context
   * @param appId     a string representing the app id
   * @param appSecret a string representation of the secret associated with the app id
   */
  private PureMetrics(Context context, String appId, String appSecret) {
    //Constructor intentionally made private
    if (null == appId) {
      log(LOG_LEVEL.FATAL, "AppId not provided will silently shutdown");
    }
    if (null == appSecret) {
      log(LOG_LEVEL.FATAL, "AppSecret not provided will silently shutdown");
    }
    String credentials = appId + ":" + appSecret;
    authBytes = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
    //appID change is not a valid scenario
    //If appId changes drop DB
    appContext = context.getApplicationContext();
    preferences = appContext.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    //this has to be on the main thread
    sessionId = getLastKnownSessionId();
    TaskManager.getInstance().warmup();
    databaseHelper = DBHelper.getInstance(appContext);

    mBuilder = null;
    _INSTANCE = this;

    //start of tracking
    registerLifeCycleHandler(appContext);
  }

  static void checkAndTrackSession(HashMap<String, Object> map, boolean override) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    if (AUTO_TRACKING_ENABLED || override) {
      long curTime = System.currentTimeMillis();
      long lastActiveTime = _INSTANCE.getLastActiveTime();
      _INSTANCE.setLastActiveTime();//This is required SINCE CheckAndTrackSession is called more than once

      if ((lastActiveTime + _SESSION_DURATION) < curTime) {
        PureMetrics.log(LOG_LEVEL.DEBUG, "Last known session start: " + _INSTANCE.sessionId + " current time: " + curTime + " | _SESSION_DURATION: " + _SESSION_DURATION);
        _INSTANCE.sessionId = curTime;
        _INSTANCE.saveNewSessionId(curTime);
        //if it a new session and auto tracking is enabled track a session start event
        trackEvent(Constants.Events.SESSION_START, map);
      }
    }

    if (_INSTANCE.isFirstTimeUser()) {
      trackAcquisition();
    }

    if (!_INSTANCE.isDeviceInfoCollected()) {
      _INSTANCE.collectDeviceInfo();
    }
    Utils.enableNetworkListener(_INSTANCE.appContext);
  }

  /**
   * Track Deeplink attribution for app open or install
   * @param source   Source of any campaign if the app was opened from a campaign
   * @param medium   Medium of the campaign: email/notification/ads
   * @param campaign Campaign name if any
   * @param deeplink Deeplink of the app launch if it was launched from a deeplink
   * @param extras   {@link HashMap} of meta data related to app start. These are custom extras
   */
  public static void trackDeeplinkAttribution(String source, String medium, String campaign, String deeplink, HashMap<String, Object> extras) {
    HashMap<String, Object> attributes = new HashMap<>();
    attributes.put(Constants.Events.Attributes.SOURCE, source);
    attributes.put(Constants.Events.Attributes.MEDIUM, medium);
    attributes.put(Constants.Events.Attributes.CAMPAIGN, campaign);
    attributes.put(Constants.Events.Attributes.DEEPLINK, deeplink);
    attributes.putAll(extras);
    trackEvent(Constants.Events.ATTRIBUTION, attributes);
  }

  /**
   * Get the current instance of {@link PureMetrics}.
   * Will return null if it has not been initialized
   *
   * @return the current instance of {@link PureMetrics} or null if not initialized
   */
  static PureMetrics getInstance() {
    return _INSTANCE;
  }

  /**
   * Helper method to get an instance of the {@link Builder} to configure the SDK
   *
   * @return an instance of the {@link Builder} class
   */
  public static Builder withBuilder() {
    if (null == mBuilder) {
      mBuilder = new Builder();
    }
    return mBuilder;
  }

  /**
   * Logs a message to logcat
   *
   * @param level   The {@link LOG_LEVEL} for which the log
   * @param message The log message
   */
  static void log(LOG_LEVEL level, String message) {
    log(level, message, null);
  }

  /**
   * Logs a message to logcat
   *
   * @param level   The {@link LOG_LEVEL} for which the log
   * @param message The log message
   * @param e       instance of {@link Throwable} which needs to be logged
   */
  static void log(LOG_LEVEL level, String message, Throwable e) {
    if (level.compareTo(logLevel) < 1) {
      if (level == LOG_LEVEL.VERBOSE)
        Log.v(TAG, message, e);
      else if (level == LOG_LEVEL.DEBUG)
        Log.d(TAG, message, e);
      else if (level == LOG_LEVEL.INFO)
        Log.i(TAG, message, e);
      else if (level == LOG_LEVEL.WARN)
        Log.w(TAG, message, e);
      else if (level == LOG_LEVEL.ERROR || level == LOG_LEVEL.FATAL)
        Log.e(TAG, message, e);
    }
  }

  /**
   * Logs an API error for PureMetrics.
   *
   * @param forAPI        error for which API
   * @param statusCode    The HTTP status code returned by the API
   * @param throwable     Instance of {@link Throwable} if any
   * @param errorResponse Error response
   */
  static void logAPIResponse(String forAPI, int statusCode, Throwable throwable, String errorResponse) {
    if (errorResponse != null && LOG_LEVEL.WARN.compareTo(logLevel) < 1) {
      LOG_LEVEL level = statusCode != 200 ? LOG_LEVEL.WARN : LOG_LEVEL.DEBUG;
      log(level, "HTTP code: " + statusCode + " " + forAPI + "\n" + errorResponse + "\n", throwable);
    }
  }

  /**
   * Check to see if the app is running in foreground or background
   *
   * @return true if the application is in foreground else returns false
   */
  public static boolean isActivityInForground() {
    return ACTIVITY_COUNTER > 0;
  }

  /**
   * Call to increment a counter when an activity starts
   */
  static synchronized void startActivity() {
    ACTIVITY_COUNTER++;
  }

  /**
   * Call to increment a counter when an activity stops
   */
  static synchronized void stopActivity() {
    ACTIVITY_COUNTER--;
    if (initialized()) {
      _INSTANCE.setLastActiveTime();
    }
  }

  /**
   * To be called when a user signs out.
   * This moves the user back to an anonymous state.
   */
  public static void resetUserInfo() {
    _RESET_AFTER_UPLOAD = true;
    PureMetrics._INSTANCE.scheduleDataSync();
  }

  /**
   * Track an event and its associated event attribute.
   * Event attributes are additional information which define an event
   *
   * @param eventName  The name of the event
   * @param attributes A {@link HashMap} of the event attributes
   */
  public static void trackEvent(String eventName, HashMap attributes) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    try {
      final JSONObject customEvent = new JSONObject();
      customEvent.put(Constants.RequestAttributes.EVENT_NAME, eventName);
      customEvent.put(Constants.RequestAttributes.TS, System.currentTimeMillis());
      if (null != attributes && attributes.size() > 0) {
        try {
          customEvent.put(Constants.RequestAttributes.EVENT_ATTRS, new JSONObject(attributes));
        } catch (Throwable e) {
          log(LOG_LEVEL.ERROR, "trackEvent", e);
        }
      }
      TaskManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          _INSTANCE.databaseHelper.storeEvents(customEvent.toString());
        }
      });
    } catch (JSONException e) {
      log(LOG_LEVEL.ERROR, "trackEvent", e);
    }
  }

  /**
   * To be called from a Crash listener like the one in
   * CrashlyticsListener#crashlyticsDidDetectCrashDuringPreviousExecution
   *
   * @param meta any additional metadata that you would like to track.
   */
  public static void trackCrash(HashMap<String, Object> meta) {
    trackEvent(Constants.Events.CRASH, meta);
  }


  /**
   * Track a user property/trait. These are user level identifiers
   *
   * @param userProperty  Name of the user property
   * @param propertyValue String Value associated with the user property
   */
  public static void trackUserProperties(final String userProperty, final String propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeUserAttributes(new StringBuilder().append("\"")
                .append(userProperty)
                .append("\"")
                .append(" : ")
                .append("\"")
                .append(propertyValue)
                .append("\"")
                .toString());
      }
    });
  }

  /**
   * Track a user property/trait. These are user level identifiers
   *
   * @param userProperty  Name of the user property
   * @param propertyValue int Value associated with the user property
   */
  public static void trackUserProperties(final String userProperty, final int propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeUserAttributes(new StringBuilder().append("\"")
                .append(userProperty)
                .append("\"")
                .append(" : ")
                .append(propertyValue)
                .toString());
      }
    });
  }

  /**
   * Track a user property/trait. These are user level identifiers
   *
   * @param userProperty  Name of the user property
   * @param propertyValue double Value associated with the user property
   */
  public static void trackUserProperties(final String userProperty, final double propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeUserAttributes(new StringBuilder().append("\"")
                .append(userProperty)
                .append("\"")
                .append(" : ")
                .append(propertyValue)
                .toString());
      }
    });
  }

  /**
   * Track a user property/trait. These are user level identifiers
   *
   * @param userProperty  Name of the user property
   * @param propertyValue boolean Value associated with the user property
   */
  public static void trackUserProperties(final String userProperty, final boolean propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }

    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeUserAttributes(new StringBuilder().append("\"")
                .append(userProperty)
                .append("\"")
                .append(" : ")
                .append(propertyValue)
                .toString());
      }
    });
  }

  /**
   * Track a user property/trait. These are user level identifiers
   *
   * @param deviceProperty Name of the device property
   * @param propertyValue  String Value associated with the user property
   */
  public static void trackDeviceProperties(final String deviceProperty, final String propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeDeviceAttributes(new StringBuilder().append("\"")
                .append(deviceProperty)
                .append("\"")
                .append(" : ")
                .append("\"")
                .append(propertyValue)
                .append("\"")
                .toString());
      }
    });
  }

  /**
   * Track a device property/trait. These are user level identifiers
   *
   * @param deviceProperty Name of the device property
   * @param propertyValue  Integer Value associated with the user property
   */
  public static void trackDeviceProperties(final String deviceProperty, final int propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }

    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeDeviceAttributes(new StringBuilder().append("\"")
                .append(deviceProperty)
                .append("\"")
                .append(" : ")
                .append(propertyValue)
                .toString());
      }
    });
  }

  /**
   * Track a device property/trait. These are user level identifiers
   *
   * @param deviceProperty Name of the device property
   * @param propertyValue  Double Value associated with the user property
   */
  public static void trackDeviceProperties(final String deviceProperty, final double propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeDeviceAttributes(new StringBuilder().append("\"")
                .append(deviceProperty)
                .append("\"")
                .append(":")
                .append(propertyValue)
                .toString());
      }
    });
  }

  /**
   * Track a device  property/trait. These are user level identifiers
   *
   * @param deviceProperty Name of the device property
   * @param propertyValue  Boolean Value associated with the user property
   */
  public static void trackDeviceProperties(final String deviceProperty, final boolean propertyValue) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        _INSTANCE.databaseHelper.storeDeviceAttributes(
                new StringBuilder().append("\"")
                        .append(deviceProperty)
                        .append("\"")
                        .append(":")
                        .append(propertyValue)
                        .toString());
      }
    });
  }

  /**
   * Explicitly track Session start.
   * Call this only when you have set {@link Builder#disableAutoTracking(boolean)} as true
   *
   * @param extras An extras which need to be tracked which gives more insight regarding the source of launch
   */
  public static void trackSessionStart(HashMap<String, Object> extras) {
    checkAndTrackSession(extras, true);
  }

  /**
   * Explicitly track Session start.
   * Call this only when you have set {@link Builder#disableAutoTracking(boolean)} as true
   *
   * @param type     The type of usage, like within app, external etc.
   */
  public static void trackSessionStart(String type) {
    HashMap<String, Object> extras = new HashMap<>();
    extras.put(Constants.Events.Attributes.TYPE, type);
    trackSessionStart(extras);
  }

  /**
   * Track start of a transaction
   * @param order The {@link Order} for which the transaction started
   */
  public static void transactionStarted(@NonNull Order order) {
    if (!initialized()) {
      log(LOG_LEVEL.DEBUG, "Pure Metrics SDK Not initialized yet.");
      return;
    }
    trackEvent(Constants.Events.Transaction.STARTED, order.eventAttrs);
  }

  /**
   * Track start of a transaction
   *
   * @param revenue The {@link Revenue} generated from a successful transaction
   */
  public static void transactionSuccessful(@NonNull Revenue revenue) {
    if (!initialized()) {
      log(LOG_LEVEL.DEBUG, "Pure Metrics SDK Not initialized yet.");
      return;
    }
    trackEvent(Constants.Events.Transaction.SUCCESSFUL, revenue.eventAttrs);
  }

  /**
   * Track a failed transaction
   *
   * @param transaction A populated {@link FailedTransaction} object
   */
  public static void transactionFailed(@NonNull FailedTransaction transaction) {
    if (!initialized()) {
      log(LOG_LEVEL.DEBUG, "PureMetrics SDK not initialized yet");
      return;
    }
    trackEvent(Constants.Events.Transaction.FAILED, transaction.eventAttrs);
  }

  /**
   * Track an acquisition event for the user
   */
  private static void trackAcquisition() {
    if (oldUser || isOldUser()) {
      trackEvent(Constants.Events.EXISTING_USER_ACQ, null);
    } else {
      trackEvent(Constants.Events.ACQUISITION, null);
    }
  }

  /**
   * Set the current user an existing user.
   */
  public static void setExistingUser() {
    oldUser = true;
    if (!initialized()) {
      log(LOG_LEVEL.DEBUG, "Not initialized yet. Will set a variable  and hope its picked up");
      return;
    }
    synchronized (_INSTANCE.lock_sharedPref) {
      _INSTANCE.preferences.edit().putBoolean(Constants.PREF_KEYS.OLDUSER, true).apply();
    }
  }

  /**
   * Check to see if it is an existing user
   *
   * @return true if the user is an existing user, false otherwise
   */
  private static boolean isOldUser() {
    if (!initialized()) {
      return false;
    }
    synchronized (_INSTANCE.lock_sharedPref) {
      return _INSTANCE.preferences.getBoolean(Constants.PREF_KEYS.OLDUSER, false);
    }
  }

  /**
   * Checks if {@link PureMetrics} was initialized or not
   *
   * @return true if it has been initialized else false
   */
  private static boolean initialized() {
    return _INSTANCE != null;
  }

  /**
   * Set the user name
   *
   * @param firstName First Name of the user
   * @param lastName  Last Name of the user
   */
  public static void setUserName(final String firstName, final String lastName) {
    if (!TextUtils.isEmpty(firstName)) {
      trackUserProperties(Constants.UserAttributes.FIRST_NAME, firstName);
    }
    if (!TextUtils.isEmpty(lastName)) {
      trackUserProperties(Constants.UserAttributes.LAST_NAME, lastName);
    }
  }

  /**
   * Set User Birthdate
   *
   * @param birthDate Set User Birthdate
   */
  public static void setUserBirthDate(Date birthDate) {
    String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
            .format(birthDate);
    String compliantString = formatted.substring(0, 22) + ":" + formatted.substring(22);
    trackUserProperties(Constants.UserAttributes.BIRTHDATE, compliantString);
  }

  /**
   * Set user gender
   *
   * @param gender gender value.
   */
  public static void setUserGender(@NonNull final String gender) {
    trackUserProperties(Constants.UserAttributes.GENDER, gender);
  }

  /**
   * Set user User Id. This is the ID using which the user can be uniquely identified on your system
   *
   * @param userId The user id of the user
   */
  public static void setUserId(final String userId) {
    //TODO if unique id changes, we might have to change anonymous id
    if (initialized()) {
      synchronized (_INSTANCE.lock_sharedPref) {
        _INSTANCE.preferences.edit().putString(Constants.PREF_KEYS.LINKING_ID, userId).apply();
      }
    }
    trackUserProperties(Constants.UserAttributes.USER_ID, userId);
  }

  /**
   * Set user primary email id
   *
   * @param emailAddress The primary email address of the user
   */
  public static void setUserEmailAddress(final String emailAddress) {
    if (!TextUtils.isEmpty(emailAddress))
      trackUserProperties(Constants.UserAttributes.EMAIL, emailAddress);
  }

  /**
   * Set User phone number
   *
   * @param phoneNumber The phone number of the user
   */
  public static void setUserPhoneNumber(final String phoneNumber) {
    if (!TextUtils.isEmpty(phoneNumber))
      trackUserProperties(Constants.UserAttributes.PHONE, phoneNumber);
  }

  /**
   * Registers the {@link ActivityLifecycleListener} for the app
   *
   * @param context An instance of the Activity or Application {@link Context}
   */
  private void registerLifeCycleHandler(Context context) {
    this.appContext = context.getApplicationContext();
    ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityLifecycleListener());
  }

  /**
   * Get the last known sessionId
   *
   * @return the last known sessionId
   */
  private long getLastKnownSessionId() {
    synchronized (lock_sharedPref) {
      return preferences.getLong(Constants.PREF_KEYS.LAST_SESSION_ID, 0);
    }
  }

  /**
   * Save new session id
   *
   * @param sessionId The session id which needs to be saved
   */
  private void saveNewSessionId(long sessionId) {
    synchronized (lock_sharedPref) {
      preferences.edit().putLong(Constants.PREF_KEYS.LAST_SESSION_ID, sessionId).apply();
    }
  }

  /**
   * Get a unique device identifier using which every device can be identified
   *
   * @return The unqiue device identifier
   */
  private String getDeviceId() {
    synchronized (lock_sharedPref) {
      String deviceId = preferences.getString(Constants.PREF_KEYS.DEVICE_ID, null);
      if (null == deviceId) {
        deviceId = Utils.getDeviceId(appContext);
        preferences.edit().putString(Constants.PREF_KEYS.DEVICE_ID, deviceId).apply();
      }
      return deviceId;
    }
  }

  /**
   * Get an anonymous id for the user. If no stored id is found, a new id is generated
   *
   * @return The unique anonymous id
   */
  private String getAnonymousId() {
    synchronized (lock_sharedPref) {
      String id = preferences.getString(Constants.PREF_KEYS.ANONYMOUS_ID, null);
      if (null == id) {
        id = Utils.generateRandomId();
        preferences.edit().putString(Constants.PREF_KEYS.ANONYMOUS_ID, id).apply();
      }
      return id;
    }
  }

  /**
   * Check if its a new user or recurring user
   *
   * @return true if it is a new user
   */
  private boolean isFirstTimeUser() {
    synchronized (lock_sharedPref) {
      boolean res = preferences.getBoolean(Constants.PREF_KEYS.IS_NEW_USER, true);
      preferences.edit().putBoolean(Constants.PREF_KEYS.IS_NEW_USER, false).apply();
      return res;
    }
  }

  private boolean isDeviceInfoCollected() {
    synchronized (lock_sharedPref) {
      boolean res = preferences.getBoolean(Constants.PREF_KEYS.DEVICEINFO_COLLECTED, false);
      preferences.edit().putBoolean(Constants.PREF_KEYS.DEVICEINFO_COLLECTED, true).apply();
      return res;
    }
  }

  /**
   * Prepare the JSON payload for the HTTP request
   *
   * @return get the string representation of the request
   */
  private String prepareRequest() {
    try {
      JSONObject requestObject = new JSONObject();
      JSONArray e = PureMetrics._INSTANCE.databaseHelper.getEventsData();
      JSONObject da = PureMetrics._INSTANCE.databaseHelper.getDeviceAttributesIfAny();
      JSONObject ua = PureMetrics._INSTANCE.databaseHelper.getUserAttributesIfAny();
      boolean sendData = false;
      if (null != da) {
        sendData = true;
        requestObject.put(Constants.RequestAttributes.DA, da);
      }
      if (null != da) {
        sendData = true;
        requestObject.put(Constants.RequestAttributes.UA, ua);
      }
      if (null != e) {
        sendData = true;
        requestObject.put(Constants.RequestAttributes.SESSION, e);
      }
      if (!sendData) {
        return null;
      }
      PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
      String versionName = pInfo.versionName;
      int versionCode = pInfo.versionCode;
      requestObject.put(Constants.RequestAttributes.AI, getAnonymousId());
      requestObject.put(Constants.RequestAttributes.DI, getDeviceId());
      requestObject.put(Constants.RequestAttributes.TS, System.currentTimeMillis());
      requestObject.put(Constants.RequestAttributes.TZ, TimeZone.getDefault().getID());
      requestObject.put(Constants.RequestAttributes.PL, Constants.PLATFORM_VALUE);
      requestObject.put(Constants.ATTR_APP_VERSION_CODE, versionCode);
      requestObject.put(Constants.ATTR_APP_VERSION_NAME, versionName);
      requestObject.put(Constants.ATTR_CONNECTION_TYPE, Utils.getNetworkClass(appContext));
      requestObject.put(Constants.ATTR_SDK_VERSION, BuildConfig.VERSION_CODE);
      String li = preferences.getString(Constants.PREF_KEYS.LINKING_ID, null);
      if (!TextUtils.isEmpty(li)) {
        requestObject.put(Constants.RequestAttributes.LI, li);
      }
      return requestObject.toString();
    } catch (Throwable e) {
      log(LOG_LEVEL.FATAL, "prepareRequest for uploading", e);
    }
    return null;
  }

  /**
   * Schedule a data upload
   */
  void scheduleDataSync() {
    if (_UPLOAD_IN_PROGRESS) {
      log(LOG_LEVEL.DEBUG, "Upload is already in progress . . .");
      return;
    }
    _UPLOAD_IN_PROGRESS = true;
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        try {
          ConnectivityManager cm =
                  (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
          NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
          boolean debugBuild = Utils.isDebugBuild(appContext);
          boolean isConnected = activeNetwork != null &&
                  activeNetwork.isConnectedOrConnecting();
          if (!isConnected) {
            log(LOG_LEVEL.DEBUG, "Not connected to Internet. Will schedule sync for later");
            return;
          }
          String payload = prepareRequest();
          if (null != payload) {
            boolean result = Utils.uploadData(authBytes, payload, debugBuild);
            if (result) {
              Utils.disableNetworkListener(appContext);
              databaseHelper.clearData();
            } else if (_RESET_AFTER_UPLOAD) {
              // this is where things go tricky,
              // if upload is pending and user resets in that case
              // we can simply reject the payload
              databaseHelper.clearData();
            } else {
              Utils.enableNetworkListener(appContext);
            }
          } else {
            PureMetrics.log(LOG_LEVEL.DEBUG, "Found Nothing to send");
            Utils.disableNetworkListener(appContext);
          }
        } finally {
          _UPLOAD_IN_PROGRESS = false;
          if (_RESET_AFTER_UPLOAD) {
            resetUserInfoInternal();
            _RESET_AFTER_UPLOAD = false;
          }
        }
      }
    });
  }

  private void resetUserInfoInternal() {
    synchronized (lock_sharedPref) {
      SharedPreferences.Editor editor = preferences.edit();
      editor.remove(Constants.PREF_KEYS.ANONYMOUS_ID);
      editor.remove(Constants.PREF_KEYS.LINKING_ID);
      editor.remove(Constants.PREF_KEYS.IS_NEW_USER);
      editor.remove(Constants.PREF_KEYS.LAST_ACTIVE_TIME);
      editor.remove(Constants.PREF_KEYS.LAST_SESSION_ID);
      editor.apply();
    }
    checkAndTrackSession(null, false);
  }

  /**
   * Collects the device information if it has not been tracked yet.
   */
  private void collectDeviceInfo() {
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        String carrier = Utils.getCarrierName(appContext);
        if (!TextUtils.isEmpty(carrier)) {
          trackDeviceProperties(Constants.DeviceAttributes.CARRIER, carrier);
        }
        trackDeviceProperties(Constants.DeviceAttributes.MAKE, Build.MANUFACTURER);
        trackDeviceProperties(Constants.DeviceAttributes.MODEL, Build.MODEL);
        trackDeviceProperties(Constants.DeviceAttributes.BRAND, Build.BRAND);
        trackDeviceProperties(Constants.DeviceAttributes.OS_VERSION, Build.VERSION.SDK_INT);
        trackDeviceProperties(Constants.RequestAttributes.PL, Constants.PLATFORM_VALUE);
        trackDeviceProperties(Constants.EVENT_TYPE, Utils.getDeviceType(appContext));
        Utils.trackAdvertisementIdIfPossible(appContext);
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        trackDeviceProperties(Constants.DeviceAttributes.DENSITY, dm.densityDpi);
        trackDeviceProperties(Constants.DeviceAttributes.DISPLAY_DIMENSIONS, dm.widthPixels + "x" + dm.heightPixels);
        int year = YearClass.get(appContext);
        trackDeviceProperties(Constants.DeviceAttributes.YEAR_CLASS, year);
        trackDeviceProperties(Constants.DeviceAttributes.LANGUAGE, Utils.getDeviceLanguage());
        trackDeviceProperties(Constants.DeviceAttributes.INSTALLER, Utils.getInstallSource(appContext));
      }
    });
  }

  /**
   * Set the last active time of the user as the current time
   */
  private void setLastActiveTime() {
    synchronized (lock_sharedPref) {
      preferences.edit().putLong(Constants.PREF_KEYS.LAST_ACTIVE_TIME, System.currentTimeMillis()).apply();
    }
  }

  /**
   * Get the last time the user was actibe in the app
   *
   * @return The last active time of the user
   */
  private long getLastActiveTime() {
    synchronized (lock_sharedPref) {
      return preferences.getLong(Constants.PREF_KEYS.LAST_ACTIVE_TIME, 0);
    }
  }

  /**
   * Available logging levels for the SDK
   */
  public enum LOG_LEVEL {
    NONE, FATAL, ERROR, WARN, INFO, DEBUG, VERBOSE
  }

  /**
   * A Builder class for {@link PureMetrics}.
   * It provides a convinient way for setting the various properties of PureMetrics.
   * <p>
   * <strong>Usage:</strong>
   * <pre>
   * <code>
   *     PureMetrics.withBuilder()
   *      .setAppConfiguration("abcdef", "123456")
   *      .init(getApplicationContext());
   * </code>
   * </pre>
   */
  public static class Builder {
    private String appId;
    private String appSecret;
    private boolean loggingLevelSet = false;

    /**
     * Set the Application Id &amp; Application secret associated with the app.
     * As seen on the PureMetrics dashboard
     *
     * @param appId     a string representing the app id
     * @param appSecret a string representation of the secret associated with the app id
     * @return the current instance of {@link Builder}
     */
    public Builder setAppConfiguration(String appId, String appSecret) {
      this.appId = appId;
      this.appSecret = appSecret;
      return this;
    }

    /**
     * Set a custom Session durartion. The default duration is {@value Constants#DEFAULT_SESSION_DURATION}.
     * The value specified cannot be less than or equal to 0
     *
     * @param timeInMillis The session duration to be set. Unit is milliseconds
     * @return the current instance of {@link Builder}
     */
    public Builder setSessionDuration(long timeInMillis) {
      if (timeInMillis > 0) {
        _SESSION_DURATION = timeInMillis;
      }
      return this;
    }

    /**
     * Disable auto session tracking
     *
     * @param disable set true if you want to disable auto session tracking
     * @return the current instance of {@link Builder}
     */
    public Builder disableAutoTracking(boolean disable) {
      AUTO_TRACKING_ENABLED = !disable;
      return this;
    }

    /**
     * Set the Logging level for the SDK
     *
     * @param logLevel The {@link LOG_LEVEL} associated for logging
     * @return the current instance of {@link Builder}
     */
    public Builder setLoggingLevel(LOG_LEVEL logLevel) {
      loggingLevelSet = true;
      PureMetrics.logLevel = logLevel;
      return this;
    }

    /**
     * Initializes the SDK
     *
     * @param context An instance of the application {@link Context}
     * @return A populated instance of PureMetrics.
     */
    public PureMetrics init(Context context) {
      //may be this was called because of config changes
      if (null != _INSTANCE) {
        _INSTANCE.registerLifeCycleHandler(context);
        return _INSTANCE;
      }
      setLoggingLevel(context);
      _INSTANCE = new PureMetrics(context, appId, appSecret);
      if (context instanceof Activity) {
        log(LOG_LEVEL.WARN, "You should be PureMetrics#init() code in your Application class");
      }
      return _INSTANCE;
    }

    /**
     * Sets the logging level for the PureMetrics logging level
     *
     * @param context An instance of the application {@link Context}
     */
    private void setLoggingLevel(Context context) {
      if (!loggingLevelSet) {
        try {
          if ((context.getPackageManager().getPackageInfo(
                  context.getPackageName(), 0).applicationInfo.flags &
                  ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            PureMetrics.logLevel = LOG_LEVEL.NONE;
          }
        } catch (PackageManager.NameNotFoundException e) {
          //intentionally suppressed
        }
      }
    }
  }

  /**
   * A Class that represents a single complete Order
   */
  public static class Order {

    /**
     * Event attributes
     */
    HashMap<String, Object> eventAttrs;

    //constructor intentionally made private
    private Order(HashMap<String, Object> eventAttrs) {
      this.eventAttrs = eventAttrs;
    }

    /**
     * Builder class for {@link Order} Object. Provides a convenient way to set
     * the various fields of a {@link Order} and
     * generate associated payload which PureMetrics SDK can process
     */
    public static class Builder {

      private HashMap<String, Object> meta = null;
      private HashMap<String, Object> params = null;
      private ArrayList<JSONObject> products = null;

      public Builder() {
        params = new HashMap<>();
        products = new ArrayList<>();
      }

      /**
       * Set The transaction Id for the current transaction
       *
       * @param transactionId The transaction id to which the transaction belongs
       * @return {@link PureMetrics.Order.Builder}
       */
      public Builder setTransactionId(String transactionId) {
        params.put(Constants.Events.Attributes.TRANSACTION_ID, transactionId);
        return this;
      }

      /**
       * Add the product information which is being purchased
       *
       * @param productId       The product Id
       * @param dimensions      Additional dimensions for the product, like category id, type etc
       * @param unitPrice       Unit price for the product
       * @param discountedPrice Discounted Price for the product
       * @param units           Number of units of the product being purchased
       * @param currency        Optional field required when your transaction currency
       *                        and product price currency is different
       * @return {@link PureMetrics.Order.Builder}
       */
      public
      @NonNull
      Builder addProduct(String productId, Map<String, Object> dimensions,
                         long unitPrice, long discountedPrice, int units, String currency) {
        try {
          JSONObject product = new JSONObject();
          product.put(Constants.Events.Attributes.PRODUCT_ID, productId);
          if (null != dimensions && dimensions.size() > 0) {
            try {
              product.put(Constants.Events.Attributes.META, new JSONObject(dimensions));
            } catch (Throwable e) {
              log(LOG_LEVEL.ERROR, "addProduct", e);
            }
          }
          product.put(Constants.Events.Attributes.DISCOUNTED_PRICE, discountedPrice);
          product.put(Constants.Events.Attributes.UNIT_PRICE, unitPrice);
          product.put(Constants.Events.Attributes.UNIT_SOLD, units);
          product.put(Constants.Events.Attributes.CURRENCY, currency);
          products.add(product);
        } catch (JSONException e) {
          log(LOG_LEVEL.ERROR, "PM: addProduct", e);
        }
        return this;
      }

      /**
       * Add any additional information which you want to pass
       *
       * @param meta Metadata associated with the revenue event.
       *             Example: name, product, category, location etc
       * @return {@link PureMetrics.Order.Builder}
       */
      public
      @NonNull
      Builder addMeta(HashMap<String, Object> meta) {
        if (null == this.meta) {
          this.meta = new HashMap<>();
        }
        this.meta.putAll(meta);
        return this;
      }

      /**
       * Combine all of the options that have been set and return a new {@link Order} object
       *
       * @return {@link Order}
       */
      public
      @NonNull
      Order build() {
        if (products.size() > 0) {
          params.put(Constants.Events.Attributes.PRODUCTS, products);
        }
        if (null != meta) {
          params.put(Constants.Events.Attributes.META, meta);
        }
        return new Order(params);
      }
    }
  }

  /**
   * A Class that represents a single complete transaction
   */
  public static class Revenue {

    /**
     * Event attributes
     */
    HashMap<String, Object> eventAttrs;

    /**
     * Constructor intentionally made private to enforce builder
     */
    private Revenue(HashMap<String, Object> attrs) {
      this.eventAttrs = attrs;
    }

    /**
     * Builder class for {@link Revenue} Object. Provides a convenient way to set
     * the various fields of a {@link Revenue} and
     * generate associated payload which PureMetrics SDK can process
     */
    public static class Builder {

      private HashMap<String, Object> meta = null;
      private HashMap<String, Object> params = null;
      private ArrayList<JSONObject> payments = null;

      public Builder() {
        params = new HashMap<>();
        payments = new ArrayList<>();
      }

      /**
       * Set The transaction Id for the current transaction
       *
       * @param transactionId The transaction id to which the transaction belongs
       * @return {@link PureMetrics.Revenue.Builder}
       */
      public
      @NonNull
      Builder setTransactionId(@NonNull String transactionId) {
        params.put(Constants.Events.Attributes.TRANSACTION_ID, transactionId);
        return this;
      }

      /**
       * The ID using which the transaction can be identified in the payment provider's system.
       * For example, In case of Stripe this is the 'balance_transaction' id
       * which is returned with a charge response.
       *
       * @param transactionId id generated by the payment provider
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setPaymentProviderTransactionId(String transactionId) {
        params.put(Constants.Events.Attributes.PG_TRANS_ID, transactionId);
        return this;
      }

      /**
       * Set the Transaction Currency. There can be only a single currency for a single transaction.
       *
       * @param currency Currency value has to comply with
       *                 <a href="http://www.iso.org/iso/home/standards/currency_codes.htm">
       *                 ISO4217</a>,
       * @return {@link PureMetrics.Revenue.Builder}
       */
      public
      @NonNull
      Builder setCurrency(@NonNull String currency) {
        params.put(Constants.Events.Attributes.CURRENCY, currency);
        return this;
      }

      /**
       * Add the discount which is applied to the transaction.
       * This is optional but there can be only a single discount per transaction
       *
       * @param discountCode  Discount code used
       * @param discountValue Discount amount if any
       * @return {@link PureMetrics.Revenue.Builder}
       */
      public
      @NonNull
      Builder setDiscount(String discountCode, long discountValue) {
        if (null != discountCode) {
          params.put(Constants.Events.Attributes.REVENUE_DISCOUNT_CODE, discountCode);
        }
        params.put(Constants.Events.Attributes.REVENUE_DISCOUNT_VALUE, discountValue);
        return this;
      }

      /**
       * Add any additional information which you want to pass. This is optional
       *
       * @param meta Metadata associated with the revenue event.
       *             Example: name, product, category, location etc
       * @return {@link PureMetrics.Revenue.Builder}
       */
      public
      @NonNull
      Builder addMeta(HashMap<String, Object> meta) {
        if (null == this.meta) {
          this.meta = new HashMap<>();
        }
        this.meta.putAll(meta);
        return this;
      }

      /**
       * Add the Payment Details. If you are splitting the payment into wallets and cards etc,
       * then add multiple payments with the amount specifically applied in that mode
       *
       * @param mode            Payment Mode
       * @param convertedAmount Payment amount converted in the transaction currency
       * @param fees            fess charged by the payment gateway if any
       * @return {@link PureMetrics.Revenue.Builder}
       */
      public
      @NonNull
      Builder addPayment(@NonNull String mode, long convertedAmount, long fees) {
        try {
          JSONObject payment = new JSONObject();
          payment.put(Constants.Events.Attributes.PAYMENT_MODE, mode);
          payment.put(Constants.Events.Attributes.AMOUNT, convertedAmount);
          payment.put(Constants.Events.Attributes.FEES, fees);
          payments.add(payment);
        } catch (JSONException e) {
          log(LOG_LEVEL.ERROR, "PM: addPayment", e);
        }
        return this;
      }

      /**
       * Combine all of the options that have been set and return a new {@link Revenue} object
       *
       * @return {@link Revenue}
       */
      public
      @NonNull
      Revenue build() {
        if (payments.size() > 0) {
          params.put(Constants.Events.Attributes.PAYMENTS, payments);
        }
        if (null != meta) {
          params.put(Constants.Events.Attributes.META, meta);
        }
        return new Revenue(params);
      }
    }
  }

  /**
   * A Class that represents a single complete transaction
   */
  public static class FailedTransaction {

    /**
     * Event attributes
     */
    HashMap<String, Object> eventAttrs;

    /**
     * Constructor intentionally made private to enforce builder
     */
    private FailedTransaction(HashMap<String, Object> attrs) {
      this.eventAttrs = attrs;
    }

    /**
     * Builder class for {@link FailedTransaction} Object. Provides a convenient way to set
     * the various fields of a {@link FailedTransaction} and
     * generate associated payload which PureMetrics SDK can process
     */
    public static class Builder {

      private HashMap<String, Object> meta = null;
      private HashMap<String, Object> params = null;

      public Builder() {
        params = new HashMap<>();
      }

      /**
       * Set The transaction Id for the current transaction
       *
       * @param transactionId The transaction id to which the transaction belongs
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setTransactionId(@NonNull String transactionId) {
        params.put(Constants.Events.Attributes.TRANSACTION_ID, transactionId);
        return this;
      }

      /**
       * The ID using which the transaction can be identified in the payment provider's system.
       * For example, In case of Stripe this is the 'balance_transaction' id
       * which is returned with a charge response.
       *
       * @param transactionId id generated by the payment provider
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setPaymentProviderTransactionId(String transactionId) {
        params.put(Constants.Events.Attributes.PG_TRANS_ID, transactionId);
        return this;
      }

      /**
       * Set the Transaction Currency. There can be only a single currency for a single transaction.
       *
       * @param currency Currency value has to comply with ISO4217,
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setCurrency(@NonNull String currency) {
        params.put(Constants.Events.Attributes.CURRENCY, currency);
        return this;
      }

      /**
       * Set the transaction for transaction as mentioned by the payment provider
       *
       * @param reason reason for transaction failure
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setFailureReason(@NonNull String reason) {
        params.put(Constants.Events.Attributes.REASON, reason);
        return this;
      }

      /**
       * Set the failed transaction amount
       *
       * @param amount The amount with which transaction was attempted
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder setAmount(long amount) {
        params.put(Constants.Events.Attributes.AMOUNT, amount);
        return this;
      }

      /**
       * Add any additional information which you want to pass
       *
       * @param meta Metadata associated with the revenue event.
       *             Example: name, product, category, location etc
       * @return {@link PureMetrics.FailedTransaction.Builder}
       */
      public
      @NonNull
      Builder addMeta(HashMap<String, Object> meta) {
        if (null == this.meta) {
          this.meta = new HashMap<>();
        }
        this.meta.putAll(meta);
        return this;
      }

      /**
       * Combine all of the options that have been set and return a new {@link FailedTransaction} object
       *
       * @return {@link FailedTransaction}
       */
      public
      @NonNull
      FailedTransaction build() {
        if (null != meta) {
          params.put(Constants.Events.Attributes.META, meta);
        }
        return new FailedTransaction(params);
      }
    }
  }
}
