package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON parser for driver tracks list
 * Created by kalmanb on 8/1/17.
 */
public class PhotoCollectionParser extends ApiResponseParser<PhotoCollection> {

  private static final String TAG = "TrackPhotosParser";

  @Override
  public PhotoCollection getHolder() {
    return new PhotoCollection();
  }

  public PhotoCollection parse(String json) {
    PhotoCollection collectionData = super.parse(json);
    if (json != null && !json.isEmpty()) {
      try {
        int sequenceId = 0;
        JSONObject obj = new JSONObject(json);
        JSONArray array = obj.getJSONObject("osv").getJSONArray("photos");
        for (int i = 0; i < array.length(); i++) {
          String link = UserDataManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("lth_name");
          String thumbLink = "";
          try {
            thumbLink = UserDataManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("th_name");
          } catch (Exception e) {
            Log.w(TAG, "displaySequence: " + e.getLocalizedMessage());
          }
          sequenceId = array.getJSONObject(i).getInt("sequence_id");
          int index = array.getJSONObject(i).getInt("sequence_index");
          int id = array.getJSONObject(i).getInt("id");
          double lat = array.getJSONObject(i).getDouble("lat");
          double lon = array.getJSONObject(i).getDouble("lng");
          if (lat != 0.0 && lon != 0.0) {
            ImageCoordinate coord = new ImageCoordinate(lat, lon, index);
            collectionData.getNodes().add(new ImageFile(link, thumbLink, index, coord));
            collectionData.getTrack().add(coord);
          }
        }
        Collections.sort(collectionData.getNodes(), (lhs, rhs) -> lhs.index - rhs.index);
        Collections.sort(collectionData.getTrack(), (lhs, rhs) -> lhs.index - rhs.index);
        Log.d(TAG, "listImages: id=" + sequenceId + " length=" + collectionData.getNodes().size());
      } catch (Exception e) {
        Log.e(TAG, "parse: ", e);
      }
    }
    return collectionData;
  }
}
