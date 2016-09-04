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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import com.facebook.device.yearclass.YearClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * A helper class to interface with PureMetrics
 */
public final class PureMetrics {

  /**
   * An instance of the application {@link Context}
   */
  private Context appContext;
  /**
   * An instance of the {@link DBHelper}
   */
  private DBHelper databaseHelper;
  /**
   * Current session Id
   */
  long sessionId;
  /**
   * Authorization Bytes to be added for Http BASIC Auth
   */
  private String authBytes;

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
    checkForAppUpdate();
  }

  static void checkAndTrackSession(HashMap<String, Object> map, boolean override) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    if (null != _INSTANCE.sessionExtras && _INSTANCE.sessionExtras.size() > 0) {
      if (null == map) {
        map = new HashMap<>();
      }
      map.putAll(_INSTANCE.sessionExtras);
      //No need to track the same values again
      _INSTANCE.sessionExtras = null;
    }
    long curTime = System.currentTimeMillis();
    long lastActiveTime = _INSTANCE.getLastActiveTime();
    _INSTANCE.setLastActiveTime();//This is required SINCE CheckAndTrackSession is called more than once

    if (_INSTANCE.isFirstTimeUser()) {
      trackAcquisition();
    }

    if (!_INSTANCE.isDeviceInfoCollected()) {
      _INSTANCE.collectDeviceInfo();
    }
    Utils.enableNetworkListener(_INSTANCE.appContext);

    if ((lastActiveTime + _SESSION_DURATION) < curTime) {
      PureMetrics.log(LOG_LEVEL.DEBUG, "Last known session start: " + _INSTANCE.sessionId + " current time: " + curTime + " | _SESSION_DURATION: " + _SESSION_DURATION);
      _INSTANCE.sessionId = curTime;
      _INSTANCE.saveNewSessionId(curTime);
      //if it a new session and auto tracking is enabled track a session start event
      if (AUTO_TRACKING_ENABLED || override) {
        trackEvent(Constants.EVENT_NAME_SESSION_START, map);
      }
    }
  }

  /**
   * A {@link HashMap} which has the session start extras which were passed by the app
   */
  private HashMap<String, Object> sessionExtras = null;

  /**
   * Add meta data to the session stating details of the app launch like,
   * whether it was launched from a deep link present in a campaign, notification etc
   *
   * @param source   Source of any campaign if the app was opened from a campaign
   * @param medium   Medium of the campaign: email/notification/ads
   * @param type     Type of session like if this is from the app or a floating bubble on screen
   * @param campaign Campaign name if any
   * @param deeplink Deeplink of the app launch if it was launched from a deeplink
   * @param extras   {@link HashMap} of meta data related to app start. These are custom extras
   */
  public static void setAppOpenExtras(String source, String medium,
                                      String type, String campaign,
                                      String deeplink, HashMap<String, Object> extras) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    _INSTANCE.sessionExtras = new HashMap<>();
    if (null != source) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_SOURCE, source);
    }

    if (null != medium) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_MEDIUM, medium);
    }

    if (null != type) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_TYPE, type);
    }

    if (null != campaign) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_CAMPAIGN, campaign);
    }

    if (null != deeplink) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_DEEPLINK, deeplink);
    }

    if (null != extras) {
      _INSTANCE.sessionExtras.put(Constants.ATTR_META, extras);
    }
  }

  /**
   * Check and see if its a new session or is an old session
   */
  static void checkAndTrackSession(Activity activity) {
    HashMap<String, Object> map = null;
    Intent intent = activity.getIntent();
    if (null != intent) {
      Bundle extras = intent.getExtras();
      if (null != extras) {
        Set<String> keySet = extras.keySet();
        map = new HashMap<>();
        for (String key : keySet) {
          if (null == key) {
            continue;
          }
          Object extraValue = extras.get(key);
          if ((null != extraValue && extraValue instanceof String) || key.contains(Constants.UTM_EXTRAS)) {
            map.put(key, extras.get(key));
          }
        }
      }
    }
    checkAndTrackSession(map, false);
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
   * The singleton instance of {@link PureMetrics}
   */
  static PureMetrics _INSTANCE;

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
   * Available logging levels for the SDK
   */
  public enum LOG_LEVEL {
    NONE, FATAL, ERROR, WARN, INFO, DEBUG, VERBOSE
  }

  /**
   * An insternal instance of {@link Builder} but this is set to NULL later
   * on since it is not required always
   */
  private static Builder mBuilder;

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
   * Disables auto tracking of sessions. No session related events will be tracked.
   * It has to be called implemented by the app
   */
  static boolean AUTO_TRACKING_ENABLED = true;

  /**
   * The session duration
   */
  static long _SESSION_DURATION = Constants.DEFAULT_SESSION_DURATION;
  /**
   * A string tag used for logging
   */
  private static final String TAG = "PureMetrics";
  /**
   * Currently set logging level for the SDK
   */
  private static LOG_LEVEL logLevel = LOG_LEVEL.WARN;

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
   * Maintains a counter for the current active activities
   */
  private static int ACTIVITY_COUNTER = 0;

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
   * Track an event which was performed by the user
   *
   * @param eventName The name of the event
   */
  static void trackEvent(String eventName) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    trackEvent(eventName, null);
  }

  /**
   * Track an event and its associated event attribute.
   * Event attributes are additional information which define an event
   *
   * @param eventName  The name of the event
   * @param attributes A {@link HashMap} of the event attributes
   */
  static void trackEvent(String eventName, HashMap<String, Object> attributes) {
    if (!initialized()) {
      log(LOG_LEVEL.FATAL, "PureMetrics was not initialized. " +
              "Please add PureMetrics.withBuilder().setAppConfiguration().init(context)");
      return;
    }
    try {
      final JSONObject customEvent = new JSONObject();
      customEvent.put(Constants.ATTR_EVENT_NAME, eventName);
      customEvent.put(Constants.ATTR_TS, System.currentTimeMillis());
      if (null != attributes && attributes.size() > 0) {
        try {
          customEvent.put(Constants.ATTR_EVENT_ATTR, new JSONObject(attributes));
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
   */
  public static void trackSessionStart() {
    trackSessionStart(null);
  }

  /**
   * Explicitly track Session start.
   * Call this only when you have set {@link Builder#disableAutoTracking(boolean)} as true
   *
   * @param extras An extras which need to be tracked which gives more insight regarding the source of launch
   */
  public static void trackSessionStart(HashMap extras) {
    checkAndTrackSession(extras, true);
  }

  /**
   * Explicitly track Session start.
   * Call this only when you have set {@link Builder#disableAutoTracking(boolean)} as true
   *
   * @param referrer The referrer for the session. This can be a notification, launcher, default
   * @param type     The type of usage, like within app, external etc.
   */
  public static void trackSessionStart(String referrer, String type) {
    HashMap<String, String> extras = new HashMap<>();
    extras.put(Constants.EVENT_REFRRER, referrer);
    extras.put(Constants.EVENT_TYPE, type);
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
    trackEvent(Constants.EVENT_TRANSACTION_STARTED, order.eventAttrs);
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
    trackEvent(Constants.EVENT_TRANSACTION_SUCCESSFUL, revenue.eventAttrs);
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
    trackEvent(Constants.EVENT_TRANSACTION_FAILED, transaction.eventAttrs);
  }

  /**
   * Track an acquisition event for the user
   */
  private static void trackAcquisition() {
    if (oldUser || isOldUser()) {
      trackEvent(Constants.EVENT_NAME_EXISTING_USER_ACQ, null);
    } else {
      trackEvent(Constants.EVENT_NAME_ACQUISITION, null);
    }
  }

  /**
   * Temporary boolean used in cases where SDK
   * has not been initialized but the method has been called
   */
  private static boolean oldUser = false;

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
      _INSTANCE.preferences.edit().putBoolean(Constants.PREF_KEY_OLDUSER, true).apply();
    }
  }

  /**
   * Check to see if it is an existing user
   *
   * @return true if the user is an existing user, false otherwise
   */
  static boolean isOldUser() {
    if (!initialized()) {
      return false;
    }
    synchronized (_INSTANCE.lock_sharedPref) {
      return _INSTANCE.preferences.getBoolean(Constants.PREF_KEY_OLDUSER, false);
    }
  }

  /**
   * Checks if {@link PureMetrics} was initialized or not
   *
   * @return true if it has been initialized else false
   */
  static boolean initialized() {
    return _INSTANCE != null;
  }

  /**
   * A mutex used for locking all {@link #preferences} operations
   */
  private final Object lock_sharedPref = new Object();

  /**
   * The {@link SharedPreferences} which will be used
   */
  private SharedPreferences preferences;

  /**
   * Get the last known sessionId
   *
   * @return the last known sessionId
   */
  long getLastKnownSessionId() {
    synchronized (lock_sharedPref) {
      return preferences.getLong(Constants.PREF_KEY_LAST_SESSION_ID, 0);
    }
  }

  /**
   * Save new session id
   *
   * @param sessionId The session id which needs to be saved
   */
  void saveNewSessionId(long sessionId) {
    synchronized (lock_sharedPref) {
      preferences.edit().putLong(Constants.PREF_KEY_LAST_SESSION_ID, sessionId).apply();
    }
  }

  /**
   * Get a unique device identifier using which every device can be identified
   *
   * @return The unqiue device identifier
   */
  String getDeviceId() {
    synchronized (lock_sharedPref) {
      String deviceId = preferences.getString(Constants.PREF_KEY_DEVICE_ID, null);
      if (null == deviceId) {
        deviceId = Utils.getDeviceId(appContext);
        preferences.edit().putString(Constants.PREF_KEY_DEVICE_ID, deviceId).apply();
      }
      return deviceId;
    }
  }

  /**
   * Get an anonymous id for the user. If no stored id is found, a new id is generated
   *
   * @return The unique anonymous id
   */
  String getAnonymousId() {
    synchronized (lock_sharedPref) {
      String id = preferences.getString(Constants.PREF_KEY_ANONYMOUS_ID, null);
      if (null == id) {
        id = Utils.generateRandomId();
        preferences.edit().putString(Constants.PREF_KEY_ANONYMOUS_ID, id).apply();
      }
      return id;
    }
  }

  /**
   * Check if its a new user or recurring user
   *
   * @return true if it is a new user
   */
  boolean isFirstTimeUser() {
    synchronized (lock_sharedPref) {
      boolean res = preferences.getBoolean(Constants.PREF_KEY_NEW_USER, true);
      preferences.edit().putBoolean(Constants.PREF_KEY_NEW_USER, false).apply();
      return res;
    }
  }

  boolean isDeviceInfoCollected() {
    synchronized (lock_sharedPref) {
      boolean res = preferences.getBoolean(Constants.PREF_KEY_DEVICEIFO_COLLECTED, false);
      preferences.edit().putBoolean(Constants.PREF_KEY_DEVICEIFO_COLLECTED, true).apply();
      return res;
    }
  }

  /**
   * Prepare the JSON payload for the HTTP request
   *
   * @return get the string representation of the request
   */
  String prepareRequest() {
    try {
      JSONObject requestObject = new JSONObject();
      JSONArray e = PureMetrics._INSTANCE.databaseHelper.getEventsData();
      JSONObject da = PureMetrics._INSTANCE.databaseHelper.getDeviceAttributesIfAny();
      JSONObject ua = PureMetrics._INSTANCE.databaseHelper.getUserAttributesIfAny();
      boolean sendData = false;
      if (null != da) {
        sendData = true;
        requestObject.put(Constants.ATTR_DA, da);
      }
      if (null != da) {
        sendData = true;
        requestObject.put(Constants.ATTR_UA, ua);
      }
      if (null != e) {
        sendData = true;
        requestObject.put(Constants.ATTR_SESSION, e);
      }
      if (!sendData) {
        return null;
      }
      PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
      String versionName = pInfo.versionName;
      int versionCode = pInfo.versionCode;
      requestObject.put(Constants.ATTR_AI, getAnonymousId());
      requestObject.put(Constants.ATTR_DI, getDeviceId());
      requestObject.put(Constants.ATTR_TS, System.currentTimeMillis());
      requestObject.put(Constants.ATTR_TZ, TimeZone.getDefault().getID());
      requestObject.put(Constants.ATTR_PL, Constants.PLATFORM_ANDROID);
      requestObject.put(Constants.ATTR_APP_VERSION_CODE, versionCode);
      requestObject.put(Constants.ATTR_APP_VERSION_NAME, versionName);
      requestObject.put(Constants.ATTR_CONNECTION_TYPE, Utils.getNetworkClass(appContext));
      requestObject.put(Constants.ATTR_SDK_VERSION, BuildConfig.VERSION_CODE);
      String li = preferences.getString(Constants.PREF_KEY_LINKING_ID, null);
      if (!TextUtils.isEmpty(li)) {
        requestObject.put(Constants.ATTR_LI, li);
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
            } else {
              Utils.enableNetworkListener(appContext);
            }
          } else {
            PureMetrics.log(LOG_LEVEL.DEBUG, "Found Nothing to send");
            Utils.disableNetworkListener(appContext);
          }
        } finally {
          _UPLOAD_IN_PROGRESS = false;
        }
      }
    });
  }

  /**
   * Collects the device information if it has not been tracked yet.
   */
  void collectDeviceInfo() {
    TaskManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        String carrier = Utils.getCarrierName(appContext);
        if (!TextUtils.isEmpty(carrier)) {
          trackDeviceProperties(Constants.DA_CARRIER, carrier);
        }
        trackDeviceProperties(Constants.DA_MAKE, Build.MANUFACTURER);
        trackDeviceProperties(Constants.DA_MODEL, Build.MODEL);
        trackDeviceProperties(Constants.DA_BRAND, Build.BRAND);
        trackDeviceProperties(Constants.DA_OS_VERSION, Build.VERSION.SDK_INT);
        trackDeviceProperties(Constants.ATTR_PL, Constants.PLATFORM_ANDROID);
        trackDeviceProperties(Constants.EVENT_TYPE, Utils.getDeviceType(appContext));
        Utils.trackAdvertisementIdIfPossible(appContext);
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        trackDeviceProperties(Constants.DA_DENSITY, dm.densityDpi);
        trackDeviceProperties(Constants.DA_DISPLAY_DIMENSION, dm.widthPixels + "x" + dm.heightPixels);
        //TODO THE next attribute needs to be removed after 5 releases or 5 months. we will move to the new display dimension attribute
        trackDeviceProperties(Constants.DA_DISPLAY_MINPX, dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels);
        int year = YearClass.get(appContext);
        trackDeviceProperties(Constants.DA_YEAR, year);
      }
    });
  }

  /**
   * Set the last active time of the user as the current time
   */
  void setLastActiveTime() {
    synchronized (lock_sharedPref) {
      preferences.edit().putLong(Constants.PREF_KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply();
    }
  }

  /**
   * Get the last time the user was actibe in the app
   *
   * @return The last active time of the user
   */
  long getLastActiveTime() {
    synchronized (lock_sharedPref) {
      return preferences.getLong(Constants.PREF_KEY_LAST_ACTIVE_TIME, 0);
    }
  }

  /**
   * Set the user name
   *
   * @param firstName First Name of the user
   * @param lastName  Last Name of the user
   */
  public static void setUserName(final String firstName, final String lastName) {
    if (!TextUtils.isEmpty(firstName)) {
      trackUserProperties(Constants.UA_FNAME, firstName);
    }
    if (!TextUtils.isEmpty(lastName)) {
      trackUserProperties(Constants.UA_LNAME, lastName);
    }

  }

  /**
   * Set the user age
   *
   * @param age Age of the user
   */
  public static void setUserAge(int age) {
    trackUserProperties(Constants.UA_AGE, age);
  }

  /**
   * User Gender identifier
   */
  public enum GENDER {
    FEMALE, MALE
  }

  /**
   * Set user gender
   *
   * @param gender gender value. Possible values {@link GENDER#MALE} and {@link GENDER#FEMALE}
   */
  public static void setUserGender(final GENDER gender) {
    if (gender.compareTo(GENDER.FEMALE) == 0) {
      trackUserProperties(Constants.UA_GENDER, Constants.UA_GENDER_FEMALE);
    } else {
      trackUserProperties(Constants.UA_GENDER, Constants.UA_GENDER_MALE);
    }
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
        _INSTANCE.preferences.edit().putString(Constants.PREF_KEY_LINKING_ID, userId).apply();
      }
    }
    trackUserProperties(Constants.UA_USER_ID, userId);
  }

  /**
   * Set user primary email id
   *
   * @param emailAddress The primary email address of the user
   */
  public static void setUserEmailAddress(final String emailAddress) {
    if (!TextUtils.isEmpty(emailAddress)) trackUserProperties(Constants.UA_EMAIL, emailAddress);
  }

  /**
   * Set User phone number
   *
   * @param phoneNumber The phone number of the user
   */
  public static void setUserPhoneNumber(final String phoneNumber) {
    if (!TextUtils.isEmpty(phoneNumber)) trackUserProperties(Constants.UA_PHONE, phoneNumber);
  }

  /**
   * Sets the current users as an activated user
   *
   * @param attributes Optionally pass any meta data for activation
   */
  static void setActivated(HashMap<String, Object> attributes) {
    //TODO add logic here also make it public when done
  }

  static void setAcquisitionData() {
    //TODO pass any acquisition information here like install referrer also make it public when done
  }

  static void trackFeature() {
    //TODO plan for feature tracking for growth metrics
  }

  /**
   * A boolean which denotes whether upload is in progress or not
   */
  static boolean _UPLOAD_IN_PROGRESS = false;

  /**
   * Checks with the version matches the previous version and tracks an update event
   */
  static void checkForAppUpdate() {
    if (!initialized()) {
      return;
    }
    synchronized (_INSTANCE.lock_sharedPref) {
      try {
        int lastVersion = _INSTANCE.preferences.getInt(Constants.PREF_KEY_LAST_KNOWN_APP_VERSION, -1);
        int curVersion = _INSTANCE.appContext
                .getPackageManager()
                .getPackageInfo(_INSTANCE.appContext.getPackageName(), 0)
                .versionCode;
        if (curVersion != lastVersion) {
          _INSTANCE.preferences.edit().putInt(
                  Constants.PREF_KEY_LAST_KNOWN_APP_VERSION,
                  curVersion).apply();
          //it was never stored, its our 1st presence, lets store it
          if (lastVersion != -1) {
            trackEvent(Constants.EVENT_UPDATE, null);
          }
        }
      } catch (PackageManager.NameNotFoundException e) {
        //can never happen
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
        params.put(Constants.ATTR_TRANSACTION_ID, transactionId);
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
          product.put(Constants.ATTR_PRODUCT_ID, productId);
          if (null != dimensions && dimensions.size() > 0) {
            try {
              product.put(Constants.ATTR_META, new JSONObject(dimensions));
            } catch (Throwable e) {
              log(LOG_LEVEL.ERROR, "addProduct", e);
            }
          }
          product.put(Constants.ATTR_DISCOUNTED_PRICE, discountedPrice);
          product.put(Constants.ATTR_UNIT_PRICE, unitPrice);
          product.put(Constants.ATTR_UNIT_SOLD, units);
          product.put(Constants.ATTR_CURRENCY, currency);
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
          params.put(Constants.ATTR_PRODUCTS, products);
        }
        if (null != meta) {
          params.put(Constants.ATTR_META, meta);
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
        params.put(Constants.ATTR_TRANSACTION_ID, transactionId);
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
        params.put(Constants.ATTR_PG_TRANS_ID, transactionId);
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
        params.put(Constants.ATTR_CURRENCY, currency);
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
          params.put(Constants.ATTR_REVENUE_DISCOUNT_CODE, discountCode);
        }
        params.put(Constants.ATTR_REVENUE_DISCOUNT_VALUE, discountValue);
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
          payment.put(Constants.ATTR_PAYMENT_MODE, mode);
          payment.put(Constants.ATTR_AMOUNT, convertedAmount);
          payment.put(Constants.ATTR_FEES, fees);
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
          params.put(Constants.ATTR_PAYMENTS, payments);
        }
        if (null != meta) {
          params.put(Constants.ATTR_META, meta);
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
        params.put(Constants.ATTR_TRANSACTION_ID, transactionId);
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
        params.put(Constants.ATTR_PG_TRANS_ID, transactionId);
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
        params.put(Constants.ATTR_CURRENCY, currency);
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
        params.put(Constants.ATTR_REASON, reason);
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
        params.put(Constants.ATTR_AMOUNT, amount);
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
          params.put(Constants.ATTR_META, meta);
        }
        return new FailedTransaction(params);
      }
    }
  }
}
