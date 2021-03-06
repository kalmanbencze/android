package com.telenav.osv.item.network;

import com.telenav.osv.item.Sequence;
import java.util.ArrayList;

/**
 * Created by kalmanb on 7/5/17.
 */
public class TrackCollection extends ApiResponse {

  private int totalFilteredItems;

  private ArrayList<Sequence> trackList = new ArrayList<>();

  public int getTotalFilteredItems() {
    return totalFilteredItems;
  }

  public void setTotalFilteredItems(int totalFilteredItems) {
    this.totalFilteredItems = totalFilteredItems;
  }

  public ArrayList<Sequence> getTrackList() {
    return trackList;
  }

  public void setTrackList(ArrayList<Sequence> trackList) {
    this.trackList = trackList;
  }
}
