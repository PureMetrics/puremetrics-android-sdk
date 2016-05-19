package io.puremetrics.sdk;

import android.content.Context;
import android.content.pm.PackageManager;

class SupportCompatV4 {

  static boolean checkSelfPermission(Context context, String permission) {
    // Catch for rare "Unknown exception code: 1 msg null" exception
    try {
      return context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    } catch (Throwable t) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.ERROR, "Failed to check permission, will return denied");
      return false;
    }
  }
}
