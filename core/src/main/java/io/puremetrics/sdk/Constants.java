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


final class Constants {

  private Constants() {
    //constructor intentionally made private
  }

  /**
   * SQLite Database name
   */
  static final String DATABASE_NAME = "pm.db";
  /**
   * Table name for Events
   */
  static final String TABLE_NAME_EVENTS = "events";

  static final String TABLE_NAME_PROPERTIES = "properties";
  /**
   * Column of {@link #TABLE_NAME_EVENTS} table a unique id for the event record
   */
  static final String COLUMN_EVENTS_ID = "_id";
  /**
   * Column of {@link #TABLE_NAME_EVENTS} table which has the session id
   * associated with the event row
   */
  static final String COLUMN_EVENTS_SESSION = "sid";
  /**
   * COLUMN Attribute type for table {@link #TABLE_NAME_PROPERTIES}
   */
  static final String COLUMN_ATTRIBUTE_TYPE = "type";
  /**
   * Attribute type USER for Database table {@link #TABLE_NAME_PROPERTIES}
   */
  static final int ATTRIBUTE_TYPE_USER = 1;
  /**
   * Attribute type DEVICE for Database table {@link #TABLE_NAME_PROPERTIES}
   */
  static final int ATTRIBUTE_TYPE_DEVICE = 2;
  /**
   * Column of {@link #TABLE_NAME_EVENTS} table which has the event information
   */
  static final String COLUMN_EVENTS_EV_JSON_STR = "event_value";
  /**
   * Column of {@link #TABLE_NAME_PROPERTIES} table which has the attribute information
   */
  static final String COLUMN_ATTRIBUTES_JSON_STR = "attr_value";
  /**
   * SharedPreference key used to store and retrieve the last known device id
   */
  static final String PREF_KEY_DEVICE_ID = "key_di";
  /**
   * SharedPreference key used to store and retrieve the last known anonymous id
   */
  static final String PREF_KEY_ANONYMOUS_ID = "key_ai";
  /**
   * SharedPreference key used to store and retrieve the last known linking id
   */
  static final String PREF_KEY_LINKING_ID = "key_li";
  /**
   * SharedPreference key used to store and retrieve the last known session id
   */
  static final String PREF_KEY_LAST_SESSION_ID = "key_l_si";
  /**
   * SharedPreference key used to store and retrieve the last active time of the user
   */
  static final String PREF_KEY_LAST_ACTIVE_TIME = "key_l_ac";
  /**
   * SharedPreference key used to store and retrieve the last session start time
   */
  static final String PREF_KEY_NEW_USER = "key_new_user";
  /**
   * SharedPreference key used to store if the device information has already been collected
   */
  static final String PREF_KEY_DEVICEIFO_COLLECTED = "key_dcollected";
  /**
   * SharedPreference key used to store and retrieve the last known app version
   */
  static final String PREF_KEY_LAST_KNOWN_APP_VERSION = "key_lav";
  /**
   * A constant value which denotes android on PureMetrics
   */
  static final int PLATFORM_ANDROID = 2;
  /**
   * The device make/manufacturer
   */
  static final String DA_MAKE = "ma";
  /**
   * The device brand
   */
  static final String DA_BRAND = "br";
  /**
   * The device model
   */
  static final String DA_MODEL = "mo";
  /**
   * The Operating System version information
   */
  static final String DA_OS_VERSION = "osv";
  /**
   * The Google Advertisement identifier
   */
  static final String DA_GAID = "aid";
  /**
   * Google limited ad tracking enabled
   */
  static final String DA_LAT = "limited_ad_track";
  /**
   * Attribute which denotes the app version name
   */
  static final String ATTR_APP_VERSION_NAME = "avn";
  /**
   * Attribute which denotes the app version code
   */
  static final String ATTR_APP_VERSION_CODE = "avc";
  /**
   * User Id. Its the same as {@link #ATTR_LI}
   */
  static final String UA_USER_ID = "ui";
  /**
   * User first name
   */
  static final String UA_FNAME = "fname";
  /**
   * User last name
   */
  static final String UA_LNAME = "lname";
  /**
   * User age
   */
  static final String UA_AGE = "age";
  /**
   * User gender
   */
  static final String UA_GENDER = "gender";
  /**
   * Gender Male
   */
  static final String UA_GENDER_MALE = "m";
  /**
   * Gender Female
   */
  static final String UA_GENDER_FEMALE = "f";
  /**
   * Phone number of the user
   */
  static final String UA_PHONE = "phone";
  /**
   * Email ID of the user
   */
  static final String UA_EMAIL = "email";
  /**
   * [API Request] JSON Attribute : Device identifier.
   */
  static final String ATTR_DI = "di";
  /**
   * [API Request] JSON Attribute : The anonymous id associated with the user
   */
  static final String ATTR_AI = "ai";
  /**
   * [API Request] JSON Attribute : Linking ID or the ID using which a user can be identified on the client system.
   */
  static final String ATTR_LI = "li";
  /**
   * [API Request] JSON Attribute : The platform
   */
  static final String ATTR_PL = "pl";
  /**
   * [API Request] JSON Attribute : Timestamp, epoch time in milliseconds
   */
  static final String ATTR_TS = "ts";
  /**
   * [API Request] JSON Attribute : The timezone of the device
   */
  static final String ATTR_TZ = "tz";
  /**
   * [API Request] JSON Attribute : An array of session data
   */
  static final String ATTR_SESSION = "s";
  /**
   * [API Request] JSON Attribute : Session Id associated with the particular session
   */
  static final String ATTR_SESSION_ID = "id";
  /**
   * [API Request] JSON Attribute : Session start time for the specific session
   */
  static final String ATTR_SESSION_START = "ss";
  /**
   * [API Request] JSON Attribute : Denotes an array of events
   */
  static final String ATTR_EVENT = "e";
  /**
   * [API Request] JSON Attribute : Name of the event
   */
  static final String ATTR_EVENT_NAME = "ev";
  /**
   * [API Request] JSON Attribute : Event attributes associated with the event
   */
  static final String ATTR_EVENT_ATTR = "attr";
  /**
   * The event referrer. This might be notification, re-targeting, email etc
   */
  static final String EVENT_REFRRER = "referrer";
  /**
   * The type of the event. Only if it is specified
   */
  static final String EVENT_TYPE = "type";
  /**
   * [API Request] JSON Attribute : Which denotes the fields for user attributes
   */
  static final String ATTR_UA = "ua";
  /**
   * [API Request] JSON Attribute : Which denotes the fields for device attributes
   */
  static final String ATTR_DA = "da";
  /**
   * Default session duration is set to 30 Minutes
   */
  static final long DEFAULT_SESSION_DURATION = 1800000L;
  /**
   * Event name for Session Start
   */
  static final String EVENT_NAME_SESSION_START = "ss";
  /**
   * Event name for acquisition
   */
  static final String EVENT_NAME_ACQUISITION = "acq";
  /**
   * Event name for an acquisition of an old user
   */
  static final String EVENT_NAME_EXISTING_USER_ACQ = "eacq";
  /**
   * PREFIX which denotes that the ID value is nothing but the IMEI number
   */
  static final String PREFIX_ID_IMEI = "I-";
  /**
   * PREFIX which denotes that the ID value is nothing but the MAC Address
   */
  static final String PREFIX_ID_MAC_ADDRESS = "M-";
  /**
   * PREFIX which denotes that the ID value is nothing but the Android ID
   */
  static final String PREFIX_ID_ANDROID_ID = "A-";
  /**
   * PREFIX which denotes that the ID value is nothing but a random generated ID
   */
  static final String PREFIX_ID_GENERATED = "R-";
  /**
   * Preference file name for PureMetrics
   */
  static final String SHARED_PREF_NAME = "pm-pref";
  /**
   * Device Attribute Constant denoting device network carrier if it is a phone
   */
  static final String DA_CARRIER = "cn";
  /**
   * Device Attribute Constant denoting device network connectivity type
   */
  static final String ATTR_CONNECTION_TYPE = "conn";
  /**
   * PureMetrics SDK version code
   */
  static final String ATTR_SDK_VERSION = "sdkv";
  /**
   * Device Attribute Constant denoting device screen density
   */
  static final String DA_DENSITY = "dis";
  /**
   * Device Attribute Constant denoting wdith x height
   */
  static final String DA_DISPLAY_DIMENSION = "sd";
  /**
   * Device Attribute Constant denoting minimum device screen dimension
   */
  static final String DA_DISPLAY_MINPX = "sw";
  /**
   * Device Attribute Constant denoting device year class information
   */
  static final String DA_YEAR = "yc";

  static final String PREF_KEY_OLDUSER = "key_olduser";
  /**
   * Event Name for App Update
   */
  static final String EVENT_UPDATE = "upd";
  /**
   * Event name for Revenue event
   */
  static final String EVENT_REVENUE = "_rev";
  /**
   * Revenue Event: value
   */
  static final String EVENT_REVENUE_VALUE = "_val";
  /**
   * Revenue Event: payment mode
   */
  static final String ATTR_REVENUE_PAYMENT_MODE = "_paym";
  /**
   * Revenue Event: discount code used
   */
  static final String ATTR_REVENUE_DISCOUNT_CODE = "_disc";
  /**
   * Reveune Event: Discount Value
   */
  static final String ATTR_REVENUE_DISCOUNT_VALUE = "_disv";
  /**
   * Revenue Event: Currency
   */
  static final String ATTR_REVENUE_CURRENCY = "_cur";
  /**
   * Revenue Event: Currency conversion value
   */
  static final String ATTR_REVENUE_CURRENCY_CONVERSION_VALUE = "_cconv";
  /**
   * Bundle extras which might have UTM tags
   */
  static final String UTM_EXTRAS = "utm";
  /**
   * App open type. Possible valus: App/Dialog/Widget
   */
  static final String ATTR_TYPE = "type";
  /**
   * Campaign which drove the app open
   */
  static final String ATTR_CAMPAIGN = "cmp";
  /**
   * Source of the campaign. Possible values: The push provider/Ad Network etc
   */
  static final String ATTR_SOURCE = "sr";
  /**
   * Campaign medium: Email/Notifications/Ads
   */
  static final String ATTR_MEDIUM = "md";
  /**
   * Deeplink for the campaign
   */
  static final String ATTR_DEEPLINK = "dl";
  /**
   * Any additional meta data
   */
  static final String ATTR_EXTRAS = "ex";

  static final String HEADER_DEBUG = "debug";
  static final String HEADER_DEBUG_VALUE = "true";
  static final String HEADER_AUTHORIZATION = "Authorization";
  static final String HEADER_CONTENTMD5 = "Content-MD5";
  static final String HEADER_CONTENT_TYPE = "Content-Type";
  static final String HEADER_CONTENT_TYPE_VALUE = "application/json;";
  static final String HEADER_BASIC_AUTH_PREFIX = "basic ";
  static final String REQUEST_METHOD_POST = "POST";
  static final String HEADER_CONNECTION = "Connection";
  static final String HEADER_CLOSE = "close";
}
