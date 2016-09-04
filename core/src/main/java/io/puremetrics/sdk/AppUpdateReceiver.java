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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} which is triggered when the application updates.
 * This is also used to figure out, old and new users
 */
public class AppUpdateReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (null == intent || null == context) {
      return;
    }
    PureMetrics.setExistingUser();
    String action = intent.getAction();
    if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
      String forPackage = intent.getPackage();
      String thisPackage = context.getPackageName();
      if (thisPackage.equals(forPackage)) {
        PureMetrics.checkForAppUpdate();
      }
    } else {
      PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "ACTION: " + action);
    }
  }
}