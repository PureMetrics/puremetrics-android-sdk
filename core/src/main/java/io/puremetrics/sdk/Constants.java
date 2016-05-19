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

  static final String COLUMN_ATTRIBUTE_TYPE = "type";

  static final int ATTRIBUTE_TYPE_USER = 1;

  static final int ATTRIBUTE_TYPE_DEVICE = 2;
  /**
   * Column of {@link #TABLE_NAME_EVENTS} table which has the event information
   */
  static final String COLUMN_EVENTS_EV_JSON_STR = "event_value";

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

  static final String PREF_KEY_LAST_ACTIVE_TIME = "key_l_ac";
  /**
   * SharedPreference key used to store and retrieve the last session start time
   */
  static final String PREF_KEY_NEW_USER = "key_new_user";
  /**
   * SharedPreference key used to store and retrieve the last known device attributes
   */
  static final String PREF_KEY_DEVICE_ATTRS = "key_device_attrs";
  /**
   * SharedPreference key used to store and retrieve the last known user attributes
   */
  static final String PREF_KEY_USER_ATTRS = "key_user_attrs";
  /**
   * SharedPreference key used to store and retrieve the last known mobile carrier
   */
  static final String PREF_KEY_LAST_CARRIER_NAME = "key_mobile_carrier";

  static final String PREF_KEY_DEVICE_DATA_TRACKED = "key_device_data_col";
  /**
   * A constant value which denotes android on PureMetrics
   */
  static final int PLATFORM_ANDROID = 2;
  /**
   * The device make/manufacturer
   */
  static final String DA_MAKE = "ma";
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
   * Phone number of the user
   */
  static final String UA_PHONE = "phone";
  /**
   * Email ID of the user
   */
  static final String UA_EMAIL = "email";
  /**
   * Device identifier.
   */
  static final String ATTR_DI = "di";
  /**
   * The anonymous id associated with the user
   */
  static final String ATTR_AI = "ai";
  /**
   * Linking ID or the ID using which a user can be identified on the client system.
   */
  static final String ATTR_LI = "li";
  /**
   * The platform
   */
  static final String ATTR_PL = "pl";
  /**
   * Timestamp, epoch time in milliseconds
   */
  static final String ATTR_TS = "ts";
  /**
   * The timezone of the device
   */
  static final String ATTR_TZ = "tz";
  /**
   * An array of session data
   */
  static final String ATTR_SESSION = "s";
  /**
   * Session Id associated with the particular session
   */
  static final String ATTR_SESSION_ID = "id";
  /**
   * The start time for the session
   */
  static final String ATTR_SESSION_START = "ss";
  /**
   * Denotes an array of events
   */
  static final String ATTR_EVENT = "e";
  /**
   * Name of the event
   */
  static final String ATTR_EVENT_NAME = "ev";
  /**
   * Event attributes associated with the event
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
   * JSONAttribute which denotes the fields for user attributes
   */
  static final String ATTR_UA = "ua";
  /**
   * JSON Attribute which denotes the fields for device attributes
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

  static final String SHARED_PREF_NAME = "pm-pref";

  static final String PREF_KEY_SYNC_PENDING = "key_sync";

  static final String DA_CARRIER = "cn";
}
