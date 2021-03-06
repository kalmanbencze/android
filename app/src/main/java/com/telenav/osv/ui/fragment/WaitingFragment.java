package com.telenav.osv.ui.fragment;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.PlayerActivity;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.network.upload.UploadStartedEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.service.UploadJobService;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.telenav.osv.ui.Navigator.SCREEN_UPLOAD_PROGRESS;

/**
 * fragment displaying local tracks
 * Created by alexandra on 7/14/16.
 */
public class WaitingFragment extends OSVFragment {

  public static final String TAG = "WaitingFragment";

  @Inject
  Preferences appPrefs;

  @Inject
  ValueFormatter valueFormatter;

  @Inject
  SequenceDB db;

  private MainActivity activity;

  public View.OnClickListener actionUploadAllListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (!appPrefs.isLoggedIn()) {
        activity.showSnackBar(R.string.login_to_upload_warning, Snackbar.LENGTH_LONG, getString(R.string.login_label), () -> {
          if (Utils.isInternetAvailable(activity)) {
            startActivity(new Intent(activity, LoginActivity.class));
          } else {
            activity.showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
          }
        });

        return;
      }
      if (!NetworkUtils.isInternetAvailable(activity)) {
        activity.showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);

        return;
      }
      if (LocalSequence.getStaticSequences().values().isEmpty()) {
        activity.showSnackBar(R.string.no_local_recordings_message, Snackbar.LENGTH_SHORT);
        return;
      }
      if (!NetworkUtils.isWifiInternetAvailable(activity) &&
          !appPrefs.isDataUploadEnabled()) {
        activity.showSnackBar(R.string.no_wifi_label, Snackbar.LENGTH_SHORT);

        return;
      }

      final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
      builder.setMessage(getString(R.string.upload_all_warning)).setTitle(getString(R.string.upload_all_warning_title))
          .setNegativeButton(R.string.cancel_label, (dialog, which) -> {

          }).setPositiveButton(R.string.upload_all_label, (dialog, which) -> {
        int result = UploadJobService.scheduleImmediateUpload(activity);
        if (result == JobScheduler.RESULT_SUCCESS) {
          activity.openScreen(SCREEN_UPLOAD_PROGRESS);
        }
      }).create().show();
    }
  };

  private Button uploadButton;

  private SequenceListAdapter localSequencesAdapter;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    setupUploadButton();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_waiting, null);
    activity = (MainActivity) getActivity();
    RecyclerView localRecyclerView = view.findViewById(R.id.local_list);
    final ConcurrentHashMap<Integer, LocalSequence> localSequences = LocalSequence.getStaticSequences();
    localSequencesAdapter = new SequenceListAdapter(activity, new ArrayList<>(localSequences.values()));
    localRecyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
    localRecyclerView.setAdapter(localSequencesAdapter);
    uploadButton = view.findViewById(R.id.upload_button);

    ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
        new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

          @Override
          public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
          }

          @Override
          public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //Remove swiped item from list and notify the RecyclerView
            if (appPrefs.shouldShowDeleteConfirmation()) {
              final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
              AlertDialog deleteDialog = builder.setTitle(R.string.delete_track_title).setMessage(R.string.delete_local_track)
                  .setPositiveButton(R.string.delete_label, (dialog, which) -> localSequencesAdapter.onDeleteItem(viewHolder))
                  .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
                  }).setNeutralButton(R.string.delete_track_and_dont_remind_me, (dialogInterface, i) -> {
                    appPrefs.setShouldShowDeleteConfirmation(false);
                    localSequencesAdapter.onDeleteItem(viewHolder);
                  }).setOnDismissListener(dialogInterface -> localSequencesAdapter.notifyItemChanged(viewHolder.getAdapterPosition()))
                  .create();
              deleteDialog.show();
            } else {
              localSequencesAdapter.onDeleteItem(viewHolder);
            }
          }
        };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    itemTouchHelper.attachToRecyclerView(localRecyclerView);
    return view;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    //        localSequencesAdapter.refresh();
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.register(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    int size = LocalSequence.checkDeletedSequences(db);
    if (size <= 0 && activity != null) {
      activity.openScreen(Navigator.SCREEN_MAP);
      return;
    }
    if (localSequencesAdapter != null) {
      localSequencesAdapter.refresh();
    }
    setupUploadButton();
  }

  @Override
  public void onStop() {
    EventBus.unregister(this);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  private void setupUploadButton() {
    Log.d(TAG, "setupUploadButton: ");
    if (uploadButton != null) {
      uploadButton.setText(R.string.upload_all_label);
      uploadButton.setOnClickListener(actionUploadAllListener);
    }
  }

  private void reverseGeocodeAddress(final LocalSequence sequence, final WaitingFragment.ViewHolder holder, boolean retry) {
    if (sequence.getLocation().getLatitude() == 0 || sequence.getLocation().getLongitude() == 0) {
      Log.e(TAG, "reverseGeocodeAddress: lat or lon 0");
      return;
    }
    if (SKReverseGeocoderManager.getInstance() != null) {
      SKSearchResult addr = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(sequence.getLocation());
      if (addr != null) {
        String address = "" + addr.getName();
        String city = "", state = "";
        for (SKSearchResultParent p : addr.getParentsList()) {
          switch (p.getParentType()) {
            case CITY:
              city = p.getParentName();
              break;
            case CITY_SECTOR:
              if ("".equals(city)) {
                city = p.getParentName();
              }
              break;
            case STATE:
              state = p.getParentName();
              break;
          }
        }
        if (!"".equals(city)) {
          address += ", " + city;
        }
        if (!"".equals(state)) {
          address += ", " + state;
        }
        sequence.setAddress(address);
      }
      if (sequence.getAddress().contains("Track ") && retry) {
        new Handler(Looper.myLooper()).postDelayed(() -> reverseGeocodeAddress(sequence, holder, false), 1500);
      }
    } else {
      Log.d(TAG, "reverseGeocodeAddress: not ready");
      if (retry) {
        new Handler(Looper.myLooper()).postDelayed(() -> reverseGeocodeAddress(sequence, holder, false), 1500);
      }
    }

    holder.addressTitle.setText(sequence.getAddress());
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUploadStarted(UploadStartedEvent event) {
    if (activity != null) {
      activity.openScreen(SCREEN_UPLOAD_PROGRESS);
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUploadCancelled(UploadCancelledEvent event) {
    setupUploadButton();
    if (localSequencesAdapter != null) {
      localSequencesAdapter.refresh();
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUploadFinished(UploadFinishedEvent event) {
    setupUploadButton();
    if (localSequencesAdapter != null) {
      localSequencesAdapter.refresh();
    }
  }

  @Subscribe
  public void onRefreshNeeded(SequencesChangedEvent event) {
    if (!event.online && !event.diskChange && localSequencesAdapter != null) {
      localSequencesAdapter.refresh();
    }
    if (localSequencesAdapter != null && localSequencesAdapter.getItemCount() == 0) {
      activity.onBackPressed();
    }
  }

  private static class ViewHolder extends RecyclerView.ViewHolder {

    TextView addressTitle;

    TextView totalImages;

    TextView dateTimeText;

    private TextView totalDistance;

    ViewHolder(View view) {
      super(view);
      addressTitle = view.findViewById(R.id.address_text);
      totalImages = view.findViewById(R.id.total_images_text);
      totalDistance = view.findViewById(R.id.total_distance_text);
      dateTimeText = view.findViewById(R.id.date_time_text);
    }
  }

  private class SequenceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private LayoutInflater inflater;

    private ArrayList<LocalSequence> data;

    private Handler mDeleteHandler = new Handler(Looper.getMainLooper());

    SequenceListAdapter(Context context, ArrayList<LocalSequence> data) {
      this.data = data;
      LocalSequence.order(this.data);
      inflater = LayoutInflater.from(context);
    }

    void refresh() {
      synchronized (LocalSequence.getStaticSequences()) {
        data = (new ArrayList<>(LocalSequence.getStaticSequences().values()));
        LocalSequence.order(data);
      }

      super.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = inflater.inflate(R.layout.item_local_sequence, parent, false);
      ViewHolder holder = new ViewHolder(view);
      view.setTag(holder);
      return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder iholder, int position) {
      if (iholder instanceof WaitingFragment.ViewHolder) {
        WaitingFragment.ViewHolder holder = (WaitingFragment.ViewHolder) iholder;
        final LocalSequence sequence = data.get(position);

        if (sequence.getAddress().contains("Track ")) {
          reverseGeocodeAddress(sequence, holder, true);
        } else {
          holder.addressTitle.setText(sequence.getAddress());
        }
        holder.addressTitle.setTag(sequence);
        int status = sequence.getStatus();
        if (status == LocalSequence.STATUS_NEW || status == LocalSequence.STATUS_INTERRUPTED) {
          if (!"".equals(
              Utils.getExternalStoragePath(activity, appPrefs))
              && sequence.getFolder().getPath().contains(Utils.getExternalStoragePath(activity, appPrefs))) {
            sequence.setExternal(true);
          }
        }
        if (sequence.isExternal()) {
          holder.addressTitle
              .setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.ic_sd_storage_black_18dp)), null,
                                                       null, null);
        }

        String distanceText = "";
        if (sequence.getDistance() >= 0) {
          String[] distance = valueFormatter.formatDistanceFromMeters(sequence.getDistance());
          distanceText = distance[0] + distance[1];
        }

        holder.totalImages.setText(sequence.getOriginalFrameCount() + " IMG");
        holder.totalDistance.setText(distanceText);
        holder.dateTimeText.setText(Utils.numericDateFormat.format(sequence.getDate()));
        if (sequence.getStatus() == LocalSequence.STATUS_NEW || sequence.getStatus() == LocalSequence.STATUS_INDEXING) {
          int color = activity.getResources().getColor(R.color.md_grey_900);
          holder.addressTitle.setTextColor(color);
          holder.totalImages.setTextColor(color);
          Drawable drawable = activity.getResources().getDrawable(R.drawable.ic_camera_small_grey);
          holder.totalImages.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
          holder.totalDistance.setTextColor(color);
          drawable = activity.getResources().getDrawable(R.drawable.ic_distance);
          holder.totalDistance.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
          holder.dateTimeText.setTextColor(color);
          holder.itemView.setOnClickListener(v -> {
            if (sequence.getStatus() == LocalSequence.STATUS_NEW) {
              if (Utils.checkSDCard(activity) || !sequence.isExternal()) {
                if (sequence.isSafe()) {
                  activity.openScreen(Navigator.SCREEN_PREVIEW, sequence);
                } else {
                  Intent intent = new Intent(activity, PlayerActivity.class);
                  intent.putExtra(PlayerActivity.EXTRA_SEQUENCE_ID, sequence.getFolder().getPath());
                  activity.startActivity(intent);
                }
              } else {
                activity.showSnackBar(getString(R.string.sdcard_missing_message), Snackbar.LENGTH_LONG);
              }
            }
          });
        } else if (sequence.getStatus() == LocalSequence.STATUS_INTERRUPTED) {
          int color = activity.getResources().getColor(R.color.gray_summary_primary_text);
          holder.addressTitle.setTextColor(color);
          holder.addressTitle.setText(getString(R.string.partial_interrupted_track_label) + " " + holder.addressTitle.getText());
          holder.totalImages.setTextColor(color);
          Drawable orig = holder.totalImages.getCompoundDrawables()[0];
          Drawable drawable;
          if (orig != null && holder.totalImages.getCompoundDrawables()[0].getConstantState() != null) {
            drawable = holder.totalImages.getCompoundDrawables()[0].getConstantState().newDrawable().mutate();
            DrawableCompat.setTint(drawable, ContextCompat.getColor(activity, R.color.gray_summary_primary_text));
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
            holder.totalImages.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
          }
          holder.totalDistance.setTextColor(color);
          orig = holder.totalDistance.getCompoundDrawables()[0];
          if (orig != null && holder.totalDistance.getCompoundDrawables()[0].getConstantState() != null) {//
            drawable = holder.totalDistance.getCompoundDrawables()[0].getConstantState().newDrawable().mutate();
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, ContextCompat.getColor(activity, R.color.gray_summary_primary_text));
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
            holder.totalDistance.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
          }
          holder.dateTimeText.setTextColor(color);
        }
      }
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getItemCount() {
      return data.size();
    }

    void onDeleteItem(RecyclerView.ViewHolder viewHolder) {
      final int position = viewHolder.getAdapterPosition();
      final LocalSequence sequence = data.get(Math.max(0, position));
      final int sequenceId = sequence.getId();
      data.remove(position);
      activity.showSnackBar(R.string.recording_deleted, 1500, getString(R.string.undo_label), () -> {
        mDeleteHandler.removeCallbacksAndMessages(null);
        data.add(position, sequence);
        notifyItemInserted(position);
      });
      mDeleteHandler.postDelayed(() -> {
        LocalSequence.deleteSequence(sequenceId);
        EventBus.post(new SequencesChangedEvent(false, true));
      }, 1600);
      notifyItemRemoved(position);
    }
  }
}

