package com.telenav.osv.item;

import java.util.Date;

/**
 * Created by kalmanb on 7/11/17.
 */
public abstract class OnlineSequence extends Sequence {

  public static final String SERVER_STATUS_UPLOADING = "NEW";

  public static final String SERVER_STATUS_UPLOADED = "UPLOAD_FINISHED";

  public static final String SERVER_STATUS_PROCESSED = "PROCESSING_FINISHED";

  public static final String SERVER_STATUS_APPROVED = "APPROVED";

  public static final String SERVER_STATUS_REJECTED = "REJECTED";

  public static final String SERVER_STATUS_TBD = "TBD";

  private static final String TAG = "OnlineSequence";

  public OnlineSequence(int sequenceId, Date date, int originalImageCount, String address, String thumbLink, boolean obd, String platform,
                        String platformVersion, String appVersion, int distance, double value) {
    this.mId = sequenceId;
    this.mDate = date;
    this.mOriginalFrameCount = originalImageCount;
    this.mFrameCount = originalImageCount;
    this.mAddress = address;
    this.mThumbLink = thumbLink;
    this.mHasObd = obd;
    this.mPlatform = platform;
    this.mPlatformVersion = platformVersion;
    this.mAppVersion = appVersion;
    this.mTotalLength = distance;
    this.value = value;
  }

  @Override
  public boolean isOnline() {
    return true;
  }

  @Override
  public boolean isSafe() {
    return true;
  }
}
