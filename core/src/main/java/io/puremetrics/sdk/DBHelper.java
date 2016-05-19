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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A java class of the type {@link SQLiteOpenHelper}
 * which helps manage the SQLite DB maintained by PureMetrics
 */
class DBHelper extends SQLiteOpenHelper {

  private static final int DATABASE_VERSION = 1;

  private final Object lock = new Object();

  private final String[] EVENT_COLUMNS = {Constants.COLUMN_EVENTS_ID, Constants.COLUMN_EVENTS_SESSION, Constants.COLUMN_EVENTS_EV_JSON_STR};
  private final String[] COLUMNS_ATTRIBUTES = {Constants.COLUMN_ATTRIBUTES_JSON_STR};

  private static final String CREATE_TABLE_EVENTS = "CREATE TABLE " + Constants.TABLE_NAME_EVENTS
          + "( " + Constants.COLUMN_EVENTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + Constants.COLUMN_EVENTS_SESSION + "  INTEGER, "
          + Constants.COLUMN_EVENTS_EV_JSON_STR + " TEXT );";

  private static final String CREATE_TABLE_PROPERTIES = "CREATE TABLE " + Constants.TABLE_NAME_PROPERTIES
          + "( " + Constants.COLUMN_EVENTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + Constants.COLUMN_ATTRIBUTE_TYPE + "  INTEGER, "
          + Constants.COLUMN_ATTRIBUTES_JSON_STR + " TEXT );";

  public DBHelper(Context context) {
    super(context, Constants.DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_TABLE_EVENTS);
    db.execSQL(CREATE_TABLE_PROPERTIES);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    //not needed as of now
  }

  /**
   * Internal static instance of DBHelper
   */
  private static DBHelper _INSTANCE = null;

  /**
   * Get an instance of the {@link DBHelper}
   * @param appContext An instance of the application {@link Context}
   * @return an instance of {@link DBHelper}
   */
  public static synchronized DBHelper getInstance(Context appContext) {
    if (null == _INSTANCE) {
      _INSTANCE = new DBHelper(appContext);
    }
    return _INSTANCE;
  }

  /**
   * Stores event data
   * @param event_data Event information which needs to be stored
   */
  public void storeEvents(String event_data) {
    synchronized (lock) {
      long result = -1;
      try {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_EVENTS_SESSION, PureMetrics.getInstance().sessionId);
        contentValues.put(Constants.COLUMN_EVENTS_EV_JSON_STR, event_data);
        result = db.insertWithOnConflict(
                Constants.TABLE_NAME_EVENTS,
                null,
                contentValues,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        if (result == -1) {
          PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Failed to insert record");
        } else {
          PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Stored record: " + event_data);
        }
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.ERROR, "While inserting events", e);
      } finally {
        close();
      }
    }
  }

  /**
   * Stores the attribute data to the table
   * @param data The attribute data which needs to be saved
   * @param type The type of attribute.
   *             it can be of the following types:
   *             {@link Constants#ATTRIBUTE_TYPE_DEVICE}
   *             {@link Constants#ATTRIBUTE_TYPE_USER}
   */
  private void storeAttributes(String data, int type) {
    synchronized (lock) {
      long result = -1;
      try {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_ATTRIBUTE_TYPE, type);
        contentValues.put(Constants.COLUMN_ATTRIBUTES_JSON_STR, data);
        result = db.insertWithOnConflict(
                Constants.TABLE_NAME_PROPERTIES,
                null,
                contentValues,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        if (result == -1) {
          PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Failed to insert record");
        } else {
          PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Stored record: " + data);
        }
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.ERROR, "While inserting ATTRIBUTES", e);
      } finally {
        close();
      }
    }
  }

  /**
   * Store user attributes
   * @param data The user attribute data to be stored
   */
  public void storeUserAttributes(String data) {
    storeAttributes(data, Constants.ATTRIBUTE_TYPE_USER);
  }

  /**
   * Store device attribute
   * @param data The device attribute data to be stored
   */
  public void storeDeviceAttributes(String data) {
    storeAttributes(data, Constants.ATTRIBUTE_TYPE_DEVICE);
  }

  /**
   * Get Event Data
   * @return returns a {@link JSONArray} representing the events data
   */
  public JSONArray getEventsData() {
    synchronized (lock) {
      Cursor dataset = null;
      try {
        SQLiteDatabase db = getReadableDatabase();
        dataset = db.query(
                Constants.TABLE_NAME_EVENTS,
                EVENT_COLUMNS,
                null, null,
                null,
                null,
                Constants.COLUMN_EVENTS_SESSION + " ASC");

        if (dataset.getCount() > 0) {
          long sessionId = -1;
          JSONArray eventsArray = new JSONArray();
          JSONArray sessionArray = new JSONArray();
          JSONObject sessionObject = new JSONObject();
          if (dataset.moveToFirst()) {
            while (!dataset.isAfterLast()) {
              long curSession = dataset.getLong(1);
              if (sessionId == -1) {
                sessionId = curSession;
              }
              if (curSession != sessionId) {
                sessionObject.put(Constants.ATTR_EVENT, eventsArray);
                sessionObject.put(Constants.ATTR_SESSION_ID, String.valueOf(sessionId));
                sessionArray.put(sessionObject);

                sessionObject = new JSONObject();
                eventsArray = new JSONArray();
                sessionId = curSession;
              }

              JSONObject event = new JSONObject(dataset.getString(2));
              eventsArray.put(event);
              dataset.moveToNext();
            }
            sessionObject.put(Constants.ATTR_SESSION_ID, String.valueOf(sessionId));
            sessionObject.put(Constants.ATTR_EVENT, eventsArray);
            sessionArray.put(sessionObject);
          }
          if (sessionArray.length() > 0) {
            return sessionArray;
          } else {
            PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Session Data: nothing to send");
            return null;
          }
        }
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "getEventsData", e);
      } finally {
        if (null != dataset && !dataset.isClosed()) {
          dataset.close();
        }
        close();
      }
    }
    return null;
  }

  /**
   * Get the User attributes to send
   * @return a populated {@link JSONObject} or null
   */
  public JSONObject getUserAttributesIfAny() {
    synchronized (lock) {
      Cursor dataset = null;
      try {
        SQLiteDatabase db = getReadableDatabase();
        dataset = db.query(
                Constants.TABLE_NAME_PROPERTIES,
                COLUMNS_ATTRIBUTES,
                Constants.COLUMN_ATTRIBUTE_TYPE + "=?",
                new String[]{String.valueOf(Constants.ATTRIBUTE_TYPE_USER)},
                null, null, null);
        int len = dataset.getCount();
        if (dataset.getCount() > 0) {
          StringBuilder builder = new StringBuilder();
          builder.append("{");
          while (dataset.moveToNext()) {
            len--;
            builder.append(dataset.getString(0));
            if (len != 0) {
              builder.append(",");
            }
          }
          builder.append("}");
          return new JSONObject(builder.toString());
        }
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "getUserAttributesIfAny", e);
      } finally {
        if (null != dataset && !dataset.isClosed()) {
          dataset.close();
        }
        close();
      }
    }
    return null;
  }

  /**
   * Get the Device attributes to send
   * @return a populated {@link JSONObject} or null
   */
  public JSONObject getDeviceAttributesIfAny() {
    synchronized (lock) {
      Cursor dataset = null;
      try {
        SQLiteDatabase db = getReadableDatabase();
        dataset = db.query(
                Constants.TABLE_NAME_PROPERTIES,
                COLUMNS_ATTRIBUTES,
                Constants.COLUMN_ATTRIBUTE_TYPE + "=?",
                new String[]{String.valueOf(Constants.ATTRIBUTE_TYPE_DEVICE)}
                , null, null, null);
        int len = dataset.getCount();
        if (dataset.getCount() > 0) {
          StringBuilder builder = new StringBuilder();
          builder.append("{");
          while (dataset.moveToNext()) {
            len--;
            builder.append(dataset.getString(0));
            if (len != 0) {
              builder.append(",");
            }
          }
          builder.append("}");
          return new JSONObject(builder.toString());
        }
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "getDeviceAttributesIfAny", e);
      } finally {
        if (null != dataset && !dataset.isClosed()) {
          dataset.close();
        }
        close();
      }
    }
    return null;
  }

  /**
   * Deletes all information after it has been sent
   */
  void clearData() {
    synchronized (lock) {
      try {
        SQLiteDatabase db = getReadableDatabase();
        db.delete(Constants.TABLE_NAME_EVENTS, null, null);
        db.delete(Constants.TABLE_NAME_PROPERTIES, null, null);
      } catch (Throwable e) {
        PureMetrics.log(PureMetrics.LOG_LEVEL.FATAL, "clearData", e);
      } finally {
        close();
      }
    }
  }
}
