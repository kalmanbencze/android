package com.telenav.osv.manager.playback;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.StringSignature;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.R;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.playback.framesprovider.AbstractFramesProvider;
import com.telenav.osv.manager.playback.framesprovider.data.TrackInfo;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import java.util.ArrayList;
import javax.inject.Inject;
import uk.co.senab.photoview.PhotoView;

/**
 * Concrete implementation of a {@link PlaybackManager} capable of consuming JPEG sequences. The git guisequences source can be either
 * local of online, depending on which {@link AbstractFramesProvider} is used.
 */
public class JpegPlaybackManager implements PlaybackManager, SeekBar.OnSeekBarChangeListener,
    AbstractFramesProvider.OnFramesLoadListener {

  private static final String TAG = "JpegPlaybackManager";

  private static final int OFFSCREEN_LIMIT = 20;

  private static long PLAYBACK_RATE = 250;

  private final Context context;

  private Sequence mSequence;

  private ArrayList<ImageFile> mImages = new ArrayList<>();

  private ArrayList<SKCoordinate> mTrack = new ArrayList<>();

  private Glide mGlide;

  private FullscreenPagerAdapter mPagerAdapter;

  private ScrollDisabledViewPager mPager;

  private SeekBar mSeekbar;

  private Handler mPlayHandler = new Handler(Looper.getMainLooper());

  private boolean mPlaying = false;

  private int mModifier = 1;

  private boolean mImageLoaded = true;

  private ArrayList<PlaybackListener> mPlaybackListeners = new ArrayList<>();

  private Runnable mRunnable = new Runnable() {

    @Override
    public void run() {
      if (mPager != null) {
        if (!mImageLoaded) {
          mPlayHandler.postDelayed(mRunnable, 20);
          return;
        }
        mPager.setCurrentItem(mSequence.getRequestedFrameIndex() + mModifier, false);
        int current = mSequence.getRequestedFrameIndex();
        if (current + mModifier >= 0 || current + mModifier <= mImages.size()) {
          mPlayHandler.postDelayed(mRunnable, PLAYBACK_RATE);
        } else {
          pause();
        }
      }
    }
  };

  private RequestListener<? super String, GlideDrawable> mGlideListener = new RequestListener<String, GlideDrawable>() {

    @Override
    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
      mImageLoaded = true;
      return false;
    }

    @Override
    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache,
                                   boolean isFirstResource) {
      mImageLoaded = true;
      return false;
    }
  };

  private ViewPager.OnPageChangeListener mPageChangedListener;

  private AbstractFramesProvider framesProvider;

  private boolean mPrepared;

  @Inject
  public JpegPlaybackManager(Context context, AbstractFramesProvider framesProvider) {
    this.context = context;
    mGlide = Glide.get(this.context);
    mPagerAdapter = new FullscreenPagerAdapter(this.context);
    this.framesProvider = framesProvider;
    this.framesProvider.setOnFramesLoadListener(this);
  }

  @Override
  public void setSource(Sequence sequence) {
    mSequence = sequence;
  }

  @Override
  public View getSurface() {
    return mPager;
  }

  @Override
  public void setSurface(View surface) {
    Log.d(TAG, "setSurface: " + surface);
    if (mPager != null) {
      mPager.removeOnPageChangeListener(mPageChangedListener);
      mPager.setAdapter(null);
    }
    mPager = (ScrollDisabledViewPager) surface;
    mPager.setOffscreenPageLimit(OFFSCREEN_LIMIT / 2);
    mPager.setAdapter(mPagerAdapter);
    mPager.setCurrentItem(mSequence.getRequestedFrameIndex());
    mPageChangedListener = new ViewPager.OnPageChangeListener() {

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        if (mSeekbar != null) {
          mSeekbar.setProgress(position);
        }
        mSequence.setRequestedFrameIndex(position);
        View view = mPager.getChildAt(0);
        if (view != null) {
          View image = view.findViewById(R.id.image);
          if (image instanceof PhotoView) {
            Matrix matrix = ((PhotoView) image).getDisplayMatrix();
            matrix.setScale(1, 1);
            ((PhotoView) image).setDisplayMatrix(matrix);
          }
        }
        view = mPager.getChildAt(1);
        if (view != null) {
          View image = view.findViewById(R.id.image);
          if (image instanceof PhotoView) {
            Matrix matrix = ((PhotoView) image).getDisplayMatrix();
            matrix.setScale(1, 1);
            ((PhotoView) image).setDisplayMatrix(matrix);
          }
        }
        view = mPager.getChildAt(2);
        if (view != null) {
          View image = view.findViewById(R.id.image);
          if (image instanceof PhotoView) {
            Matrix matrix = ((PhotoView) image).getDisplayMatrix();
            matrix.setScale(1, 1);
            ((PhotoView) image).setDisplayMatrix(matrix);
          }
        }
      }

      @Override
      public void onPageScrollStateChanged(int state) {

      }
    };
    mPager.addOnPageChangeListener(mPageChangedListener);
    if (mSequence.getRequestedFrameIndex() != 0) {
      if (mPager != null) {
        mPager.setCurrentItem(mSequence.getRequestedFrameIndex());
      }
    }
  }

  @Override
  public void prepare() {
    if (!mPrepared) {
      loadFrames();
    }
  }

  @Override
  public void next() {
    pause();
    if (mPager != null) {
      mPager.setCurrentItem(mSequence.getRequestedFrameIndex() + 1);
    }
  }

  @Override
  public void previous() {
    pause();
    if (mPager != null) {
      mPager.setCurrentItem(mSequence.getRequestedFrameIndex() - 1);
    }
  }

  @Override
  public void play() {
    mModifier = 1;
    PLAYBACK_RATE = 250;
    playImpl();
  }

  @Override
  public void pause() {
    if (mPlaying) {
      mPlaying = false;
      mPlayHandler.removeCallbacks(mRunnable);
      for (PlaybackListener pl : mPlaybackListeners) {
        pl.onPaused();
      }
    }
  }

  @Override
  public void stop() {
    mPlaying = false;
    mPlayHandler.removeCallbacks(mRunnable);
    if (mPager != null) {
      mPager.setCurrentItem(0, false);
    }
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onStopped();
    }
  }

  @Override
  public void fastForward() {
    mModifier = 1;
    PLAYBACK_RATE = 125;
    playImpl();
  }

  @Override
  public void fastBackward() {
    mModifier = -1;
    PLAYBACK_RATE = 125;
    playImpl();
  }

  @Override
  public boolean isPlaying() {
    return mPlaying;
  }

  @Override
  public void setSeekBar(SeekBar seekBar) {
    mSeekbar = seekBar;
    if (mSeekbar != null) {
      mSeekbar.setProgress(0);
      mSeekbar.setOnSeekBarChangeListener(this);
    }
    loadFrames();
  }

  @Override
  public int getLength() {
    if (mImages != null) {
      return mImages.size();
    }
    return 0;
  }

  @Override
  public void destroy() {
    stop();
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onExit();
    }
    mPlaybackListeners.clear();
    mGlide.clearMemory();
    framesProvider.setOnFramesLoadListener(null);
  }

  @Override
  public void addPlaybackListener(PlaybackListener playbackListener) {
    if (!mPlaybackListeners.contains(playbackListener)) {
      mPlaybackListeners.add(playbackListener);
    }
  }

  @Override
  public void removePlaybackListener(PlaybackListener playbackListener) {
    mPlaybackListeners.remove(playbackListener);
  }

  @Override
  public boolean isSafe() {
    return true;
  }

  @Override
  public Sequence getSequence() {
    return mSequence;
  }

  public ArrayList<SKCoordinate> getTrack() {
    return mTrack;
  }

  @Override
  public void onSizeChanged() {

  }

  private void loadFrames() {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPreparing();
    }
    framesProvider.fetchFrames(mSequence.getId());
  }

  private void playImpl() {
    try {
      if (mPlayHandler != null && !isPlaying()) {
        mPlaying = true;
        mPlayHandler.post(mRunnable);
        for (PlaybackListener pl : mPlaybackListeners) {
          pl.onPlaying();
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "onConnected: " + Log.getStackTraceString(e));
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (fromUser) {
      mPager.setCurrentItem(progress, false);
    }

    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onProgressChanged(progress);
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {

  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {

  }

  @Override
  public void onFramesLoaded(TrackInfo trackInfo) {
    mPrepared = true;
    mImages.clear();
    mImages.addAll(trackInfo.getNodes());
    mTrack = new ArrayList<>(trackInfo.getTrack());
    mSequence.getPolyline().setNodes(mTrack);
    if (mSeekbar != null) {
      mSeekbar.setMax(mImages.size());
      mSeekbar.setProgress(0);
    }
    mPagerAdapter.notifyDataSetChanged();
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPrepared(true);
    }
    if (mSequence.getRequestedFrameIndex() != 0) {
      if (mPager != null) {
        mPager.setCurrentItem(mSequence.getRequestedFrameIndex());
      }
    }
  }

  @Override
  public void onFramesLoadFailed() {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPrepared(false);
    }
  }

  private class FullscreenPagerAdapter extends PagerAdapter {

    private final LayoutInflater inflater;

    FullscreenPagerAdapter(final Context context) {
      inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
      return mImages.size();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      View view;
      view = inflater.inflate(R.layout.item_image_view_pager, container, false);
      if (view != null) {
        if (position % OFFSCREEN_LIMIT == 0) {
          mGlide.trimMemory(OFFSCREEN_LIMIT);
        }
        try {
          DrawableRequestBuilder<String> builder = Glide.with(context)
              .load((PLAYBACK_RATE < 200 && isPlaying()) ? mImages.get(position).thumb : mImages.get(position).link)
              .fitCenter()
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .animate(new AlphaAnimation(1f, 1f))
              .skipMemoryCache(false)
              .signature(new StringSignature(
                  mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude() + " full"))
              .priority(Priority.NORMAL)
              .error(R.drawable.vector_picture_placeholder)
              .listener(mGlideListener);

          if (PLAYBACK_RATE > 200 || !isPlaying()) {

            if (!"".equals(mImages.get(position).thumb) && mImages.get(position).file == null) {

              builder.thumbnail(Glide.with(context)
                                    .load(mImages.get(position).thumb)
                                    .fitCenter()
                                    .animate(new AlphaAnimation(1f, 1f))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .skipMemoryCache(false)
                                    .signature(
                                        new StringSignature(
                                            mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude()))
                                    .priority(Priority.IMMEDIATE)
                                    .error(R.drawable.vector_picture_placeholder)
                                    .listener(mGlideListener));
            } else if (mImages.get(position).file != null) {
              builder.thumbnail(0.2f).listener(mGlideListener);
            }
          }
          mImageLoaded = false;
          builder.into((ImageView) view);
        } catch (Exception e) {
          Log.w(TAG, "instantiateItem: " + Log.getStackTraceString(e));
        }
      }
      container.addView(view);
      return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      if (object != null) {
        Glide.clear(((View) object));
      }
      container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return object == view;
    }
  }
}
