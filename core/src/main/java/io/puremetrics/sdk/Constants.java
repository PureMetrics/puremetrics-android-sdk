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
   * A constant value which denotes android on PureMetrics
   */
  static final int PLATFORM_VALUE = 2;
  /**
   * Attribute which denotes the app version name
   */
  static final String ATTR_APP_VERSION_NAME = "avn";
  /**
   * Attribute which denotes the app version code
   */
  static final String ATTR_APP_VERSION_CODE = "avc";
  /**
   * The event referrer. This might be notification, re-targeting, email etc
   */
  static final String EVENT_REFRRER = "referrer";
  /**
   * The type of the event. Only if it is specified
   */
  static final String EVENT_TYPE = "type";
  /**
   * Default session duration is set to 30 Minutes
   */
  static final long DEFAULT_SESSION_DURATION = 1800000L;
  /**
   * Preference file name for PureMetrics
   */
  static final String SHARED_PREF_NAME = "pm-pref";
  /**
   * Device Attribute Constant denoting device network connectivity type
   */
  static final String ATTR_CONNECTION_TYPE = "conn";
  /**
   * PureMetrics SDK version code
   */
  static final String ATTR_SDK_VERSION = "sdkv";
  /**
   * Http Method Post
   */
  static final String REQUEST_METHOD_POST = "POST";
  static final String REPLACEMENT_CHAR = "�";
  static final String UNKNOWN_VALUE = "unknown";

  private Constants() {
    //constructor intentionally made private
  }

  /**
   * SharedPreference keys for properties which need to be retained by the SDK
   */
  interface PREF_KEYS {
    /**
     * SharedPreference key used to store and retrieve the last known device id
     */
    String DEVICE_ID = "key_di";
    /**
     * SharedPreference key used to store and retrieve the last known anonymous id
     */
    String ANONYMOUS_ID = "key_ai";
    /**
     * SharedPreference key used to store and retrieve the last known linking id
     */
    String LINKING_ID = "key_li";
    /**
     * SharedPreference key used to store and retrieve the last known session id
     */
    String LAST_SESSION_ID = "key_l_si";
    /**
     * SharedPreference key used to store and retrieve the last active time of the user
     */
    String LAST_ACTIVE_TIME = "key_l_ac";
    /**
     * SharedPreference key used to store and retrieve the last session start time
     */
    String IS_NEW_USER = "key_new_user";
    /**
     * SharedPreference key used to store if the device information has already been collected
     */
    String DEVICEINFO_COLLECTED = "key_dcollected";
    /**
     * Shared Preference key used to store state if it is an existing user or new user
     */
    String OLDUSER = "olduser";
  }

  /**
   * Device Attributes
   */
  interface DeviceAttributes {
    /**
     * The device make/manufacturer
     */
    String MAKE = "ma";
    /**
     * The device brand
     */
    String BRAND = "br";
    /**
     * The device model
     */
    String MODEL = "mo";
    /**
     * The Operating System version information
     */
    String OS_VERSION = "osv";
    /**
     * The Google Advertisement identifier
     */
    String GAID = "aid";
    /**
     * Google limited ad tracking enabled
     */
    String LAT = "limited_ad_track";
    /**
     * Network carrier if it is a phone
     */
    String CARRIER = "cn";
    /**
     * Device Attribute Constant denoting device screen density
     */
    String DENSITY = "dis";
    /**
     * Device Attribute Constant denoting wdith x height
     */
    String DISPLAY_DIMENSIONS = "sd";
    /**
     * Device Attribute Constant denoting device year class information
     */
    String YEAR_CLASS = "yc";
    /**
     * Device Language
     */
    String LANGUAGE = "lang";
    /**
     * App installer package name/id
     */
    String INSTALLER = "installer";
  }

  /**
   * Known User Attributes which can be tracked for a specific user
   */
  interface UserAttributes {
    /**
     * User Id. Its the same as {@link RequestAttributes#LI}
     */
    String USER_ID = "ui";
    /**
     * User first name
     */
    String FIRST_NAME = "fname";
    /**
     * User last name
     */
    String LAST_NAME = "lname";
    /**
     * User Birthdate
     */
    String BIRTHDATE = "bday";
    /**
     * User gender
     */
    String GENDER = "gender";
    /**
     * Phone number of the user
     */
    String PHONE = "phone";
    /**
     * Email ID of the user
     */
    String EMAIL = "email";
  }

  interface PREFIX {
    /**
     * PREFIX which denotes that the ID value is nothing but the IMEI number
     */
    String ID_IMEI = "I-";
    /**
     * PREFIX which denotes that the ID value is nothing but the MAC Address
     */
    String ID_MAC = "M-";
    /**
     * PREFIX which denotes that the ID value is nothing but the Android ID
     */
    String ID_ANDROID = "A-";
    /**
     * PREFIX which denotes that the ID value is nothing but a random generated ID
     */
    String ID_RANDOM = "R-";
  }

  interface RequestAttributes {
    /**
     * [API Request] JSON Attribute : Device identifier.
     */
    String DI = "di";
    /**
     * [API Request] JSON Attribute : The anonymous id associated with the user
     */
    String AI = "ai";
    /**
     * [API Request] JSON Attribute : Linking ID or the ID using which a user can be identified on the client system.
     */
    String LI = "li";
    /**
     * [API Request] JSON Attribute : The platform
     */
    String PL = "pl";
    /**
     * [API Request] JSON Attribute : Timestamp, epoch time in milliseconds
     */
    String TS = "ts";
    /**
     * [API Request] JSON Attribute : The timezone of the device
     */
    String TZ = "tz";
    /**
     * [API Request] JSON Attribute : An array of session data
     */
    String SESSION = "s";
    /**
     * [API Request] JSON Attribute : Session Id associated with the particular session
     */
    String SESSION_ID = "id";
    /**
     * [API Request] JSON Attribute : Session start time for the specific session
     */
    String SESSION_START_TIME = "ss";
    /**
     * [API Request] JSON Attribute : Denotes an array of events
     */
    String EVENT = "e";
    /**
     * [API Request] JSON Attribute : Name of the event
     */
    String EVENT_NAME = "ev";
    /**
     * [API Request] JSON Attribute : Event attributes associated with the event
     */
    String EVENT_ATTRS = "attr";
    /**
     * [API Request] JSON Attribute : Which denotes the fields for user attributes
     */
    String UA = "ua";
    /**
     * [API Request] JSON Attribute : Which denotes the fields for device attributes
     */
    String DA = "da";
  }

  /**
   * HTTP Headers
   */
  interface Headers {
    /**
     * DEBUG Header
     */
    String DEBUG = "debug";
    /**
     * Debug header value
     */
    String VALUE_DEBUG = "true";
    /**
     * Authorization header
     */
    String AUTHORIZATION = "Authorization";
    /**
     * Checksum Header
     */
    String CONTENTMD5 = "Content-MD5";
    /**
     * Content Type
     */
    String CONTENT_TYPE = "Content-Type";
    /**
     * Content type value application/json
     */
    String VALUE_APPLICATION_JSON = "application/json;";
    /**
     * Basic Authorization value prefix
     */
    String BASIC_AUTH_PREFIX = "basic ";
    /**
     * Connection header
     */
    String CONNECTION = "Connection";
    /**
     * Connection header value close
     */
    String CLOSE = "close";
  }

  /**
   * Network type constants
   */
  interface NetworkType {
    /**
     * Wifi
     */
    String WIFI = "wifi";
    /**
     * 4G Network
     */
    String FOUR_G = "4g";
    /**
     * 3G Network
     */
    String THREE_G = "3g";
    /**
     * Not connected to any network
     */
    String NOT_CONNECTED = "not_connected";
    /**
     * 2G Network
     */
    String TWO_G = "2g";
  }

  static class Events {

    /**
     * Event name for Session Start
     */
    static final String SESSION_START = "ss";
    /**
     * Event name for acquisition
     */
    static final String ACQUISITION = "acq";
    /**
     * Event name for an acquisition of an old user
     */
    static final String EXISTING_USER_ACQ = "eacq";
    /**
     * Denotes a crash event
     */
    static final String CRASH = "_crash";
    /**
     * Attribution or better know as the Interaction event
     */
    static final String ATTRIBUTION = "_tp";

    private Events() {
    }

    interface Transaction {
      /**
       * Event name for Successful Transaction
       */
      String SUCCESSFUL = "_trnss";

      /**
       * Event name for Transaction Started
       */
      String STARTED = "_trnst";
      /**
       * Event name for Transaction failed
       */
      String FAILED = "_trnf";
    }

    interface Attributes {
      /**
       * Revenue Event: value
       */
      String AMOUNT = "amt";
      /**
       * Revenue Event: Product list
       */
      String PRODUCTS = "prod";
      /**
       * Revenue Event: Product Id
       */
      String PRODUCT_ID = "pid";
      /**
       * Revenue Event: Discounted price a product
       */
      String DISCOUNTED_PRICE = "dp";
      /**
       * Revenue Event: Attribute for Unit price of a product
       */
      String UNIT_PRICE = "up";
      /**
       * Revenue Event: Attribute for Units Sold
       */
      String UNIT_SOLD = "ut";
      /**
       * Revenue Event: payment mode
       */
      String PAYMENT_MODE = "md";
      /**
       * Revenue event attribute which has the payment mode information
       */
      String PAYMENTS = "pay";
      /**
       * Fees charged by the payment gateway
       */
      String FEES = "fees";
      /**
       * Revenue Event: discount code used
       */
      String REVENUE_DISCOUNT_CODE = "disc";
      /**
       * Reveune Event: Discount Value
       */
      String REVENUE_DISCOUNT_VALUE = "disv";
      /**
       * Additional meta data
       */
      String META = "meta";
      /**
       * Revenue Event: Currency
       */
      String CURRENCY = "cur";
      /**
       * Revenue Event: TransactionId
       */
      String TRANSACTION_ID = "tid";
      /**
       * Revenue event: Payment provider transaction id
       */
      String PG_TRANS_ID = "pgtid";
      /**
       * Revenue Event: Transaction Failed reason
       */
      String REASON = "rsn";
      /**
       * App open type. Possible valus: App/Dialog/Widget
       */
      String TYPE = "type";
      /**
       * Campaign which drove the app open
       */
      String CAMPAIGN = "cmp";
      /**
       * Source of the campaign. Possible values: The push provider/Ad Network etc
       */
      String SOURCE = "sr";
      /**
       * Campaign medium: Email/Notifications/Ads
       */
      String MEDIUM = "md";
      /**
       * Deeplink for the campaign
       */
      String DEEPLINK = "dl";
    }
  }
}
