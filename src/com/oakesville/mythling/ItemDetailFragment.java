/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling;

import java.io.EOFException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.Reporter;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import io.oakesville.media.ArtworkDescriptor;
import io.oakesville.media.Category;
import io.oakesville.media.Item;
import io.oakesville.media.Listable;
import io.oakesville.media.MediaSettings.MediaType;
import io.oakesville.media.Recording;
import io.oakesville.media.TvShow;
import io.oakesville.media.Video;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = ItemDetailFragment.class.getSimpleName();

    private MediaActivity mediaActivity;
    private View detailView;
    private ImageView artworkView;
    private Listable listable;
    private int idx;
    public void setIdx(int idx) { this.idx = idx; }

    private final int[] ratingViewIds = new int[]{R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5};

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mediaActivity = (MediaActivity) activity;
    }

    @Override
    public void onDetach() {
        mediaActivity = null;
        super.onDetach();
    }

    private AppSettings getAppSettings() {
        return mediaActivity.getAppSettings();
    }

    private boolean isMediaPagerActivity() {
        return !isAdded() || getActivity() instanceof MediaPagerActivity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isMediaPagerActivity()) {
            detailView = inflater.inflate(R.layout.detail, container, false);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // always vertical
                detailView = inflater.inflate(R.layout.detail_pane_vertical, container, false);
            } else {
                // orientation depends on artwork selection
                String artSg = getAppSettings().getArtworkStorageGroup(getAppSettings().getMediaSettings().getType());
                if (AppSettings.ARTWORK_SG_COVERART.equals(artSg) || AppSettings.ARTWORK_SG_FANART.equals(artSg))
                    detailView = inflater.inflate(R.layout.detail_pane_horizontal, container, false);
                else
                    detailView = inflater.inflate(R.layout.detail_pane_vertical, container, false);
            }
        }
        return detailView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // MediaPagerActivity sets idx directly
        if (!isMediaPagerActivity()) {
            idx = getArguments() == null ? 1 : getArguments().getInt(MediaActivity.SEL_ITEM_INDEX);
        }
        if (mediaActivity.getListables().size() > idx) // otherwise may not be loaded
            populate();
    }

    private void populate() {
        try {
            listable = mediaActivity.getListables().get(idx);

            TextView titleView = (TextView) detailView.findViewById(R.id.title_text);
            boolean grabFocus = getArguments() == null ? false : getArguments().getBoolean(MediaActivity.GRAB_FOCUS);

            if (listable instanceof Category) {
                Category category = (Category) listable;

                titleView.setText(category.getName());
                titleView.setMovementMethod(LinkMovementMethod.getInstance());
                Spannable spans = (Spannable) titleView.getText();
                ClickableSpan clickSpan = new ClickableSpan() {
                    public void onClick(View v) {
                        v.setBackgroundColor(Color.GRAY);
                        String path = mediaActivity.getPath().length() == 0 ? listable.toString() : mediaActivity.getPath() + "/" + listable.toString();
                        Uri uri = new Uri.Builder().path(path).build();
                        mediaActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaPagerActivity.class));
                    }
                };
                spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
                Drawable folder = mediaActivity.getResources().getDrawable(R.drawable.folder);
                artworkView.setImageDrawable(folder);
                if (!getAppSettings().isTv()) {
                    artworkView.setClickable(true);
                    artworkView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            v.setBackgroundResource(R.drawable.folder_frame);
                            String path = mediaActivity.getPath().length() == 0 ? listable.toString() : mediaActivity.getPath() + "/" + listable.toString();
                            Uri uri = new Uri.Builder().path(path).build();
                            mediaActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaPagerActivity.class));
                        }
                    });
                }
            } else if (listable instanceof Item) {
                Item item = (Item) listable;

                titleView.setText(item.getTitle());

                String subLabel = item.getSubLabel();
                TextView subTitleView = (TextView) detailView.findViewById(R.id.subtitle_text);
                if (subLabel == null) {
                    subTitleView.setVisibility(View.GONE);
                } else {
                    subTitleView.setText(subLabel);
                    subTitleView.setVisibility(View.VISIBLE);
                }

                // rating
                if (item.getRating() > 0) {
                    for (int i = 0; i < 5; i++) {
                        ImageView star = (ImageView) detailView.findViewById(ratingViewIds[i]);
                        if (i <= item.getRating() - 1)
                            star.setImageResource(R.drawable.rating_full);
                        else if (i < item.getRating())
                            star.setImageResource(R.drawable.rating_half);
                        else
                            star.setImageResource(R.drawable.rating_empty);
                        star.setVisibility(View.VISIBLE);
                    }
                }

                if (item instanceof Video) {
                    Video video = (Video) item;
                    // director
                    TextView tvDir = (TextView) detailView.findViewById(R.id.director_text);
                    if (video.getDirector() != null)
                        tvDir.setText(mediaActivity.getString(R.string.directed_by_) + video.getDirector());
                    else
                        tvDir.setVisibility(View.GONE);

                    TextView tvAct = (TextView) detailView.findViewById(R.id.actors_text);
                    // actors
                    if (video.getActors() != null)
                        tvAct.setText(mediaActivity.getString(R.string.starring_) + video.getActors());
                    else
                        tvAct.setVisibility(View.GONE);

                    // summary
                    if (video.getSummary() != null) {
                        TextView tv = (TextView) detailView.findViewById(R.id.summary_text);
                        String summary = video.getSummary();
                        tv.setText(summary);
                    }

                    if (getAppSettings().deviceSupportsWebLinks()) {
                        // custom link (only for movies and tv series)
                        if ((item.isMovie() || item.isTvSeries())
                                && getAppSettings().getCustomBaseUrl() != null && !getAppSettings().getCustomBaseUrl().isEmpty()) {
                            try {
                                String encodedTitle = URLEncoder.encode(item.getTitle(), "UTF-8");
                                URL url = new URL(getAppSettings().getCustomBaseUrl() + mediaActivity.getPath() + "/" + encodedTitle);
                                TextView tv = (TextView) detailView.findViewById(R.id.custom_link);
                                tv.setVisibility(View.VISIBLE);
                                String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
                                tv.setText(Html.fromHtml("<a href='" + url + "'>" + host + "</a>"));
                                tv.setMovementMethod(LinkMovementMethod.getInstance());
                                tv.setOnClickListener(new OnClickListener() {
                                    public void onClick(View v) {
                                        v.setBackgroundColor(Color.GRAY);
                                    }
                                });
                            } catch (IOException ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                if (getAppSettings().isErrorReportingEnabled())
                                    new Reporter(ex).send();
                            }
                        } else {
                            detailView.findViewById(R.id.custom_link).setVisibility(View.GONE); // no gap before std link
                        }

                        // page link
                        if (video.getPageUrl() != null || video.getInternetRef() != null) {
                            try {
                                String pageUrl = video.getPageUrl();
                                if (pageUrl == null || pageUrl.isEmpty()) {
                                    String baseUrl = MediaActivity.getAppData().getMediaList().getMediaType() == MediaType.tvSeries ? getAppSettings().getTvBaseUrl() : getAppSettings().getMovieBaseUrl();
                                    String ref = video.getInternetRef();
                                    int lastUnderscore = ref.lastIndexOf('_');
                                    if (lastUnderscore >= 0 && lastUnderscore < ref.length() - 1)
                                        ref = ref.substring(lastUnderscore + 1);
                                    pageUrl = baseUrl + ref;
                                }
                                URL url = new URL(pageUrl);
                                TextView tv = (TextView) detailView.findViewById(R.id.page_link);
                                tv.setVisibility(View.VISIBLE);
                                String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
                                tv.setText(Html.fromHtml("<a href='" + pageUrl + "'>" + host + "</a>"));
                                tv.setMovementMethod(LinkMovementMethod.getInstance());
                                tv.setOnClickListener(new OnClickListener() {
                                    public void onClick(View v) {
                                        v.setBackgroundColor(Color.GRAY);
                                    }
                                });
                            } catch (MalformedURLException ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                if (getAppSettings().isErrorReportingEnabled())
                                    new Reporter(ex).send();
                            }
                        } else {
                            detailView.findViewById(R.id.page_link).setVisibility(View.GONE);
                        }
                    }
                } else {
                    if (item instanceof TvShow) {
                        detailView.findViewById(R.id.director_text).setVisibility(View.GONE);
                        detailView.findViewById(R.id.actors_text).setVisibility(View.GONE);

                        TvShow tvShow = (TvShow) item;
                        TextView tv = (TextView) detailView.findViewById(R.id.summary_text);
                        tv.setText(tvShow.getSummary());
                    }
                }

                // status icons
                ImageView transcodedIcon = (ImageView) detailView.findViewById(R.id.item_transcoded);
                transcodedIcon.setVisibility(item.isTranscoded() ? View.VISIBLE : View.GONE);
                ImageView downloadedIcon = (ImageView) detailView.findViewById(R.id.item_downloaded);
                downloadedIcon.setVisibility(item.isDownloaded() ? View.VISIBLE : View.GONE);

                ImageButton playBtn = (ImageButton) detailView.findViewById(R.id.btn_play);
                ImageButton transcodeBtn = (ImageButton) detailView.findViewById(R.id.btn_transcode);
                ImageButton downloadBtn = (ImageButton) detailView.findViewById(R.id.btn_download);
                ImageButton deleteBtn = (ImageButton) detailView.findViewById(R.id.btn_delete);

                if (getAppSettings().isFireTv()) {
                    playBtn.setBackgroundResource(R.drawable.firetv_button);
                    transcodeBtn.setBackgroundResource(R.drawable.firetv_button);
                    downloadBtn.setBackgroundResource(R.drawable.firetv_button);
                    deleteBtn.setBackgroundResource(R.drawable.firetv_button);
                }
                if (getAppSettings().isTv()) {
                    if (mediaActivity.getListView() != null) {
                        // split view
                        playBtn.setOnKeyListener(new OnKeyListener() {
                            public boolean onKey(View v, int keyCode, KeyEvent event) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                                        mediaActivity.getListView().requestFocus();
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                    }
                } else {
                    playBtn.setBackgroundColor(Color.TRANSPARENT);
                    downloadBtn.setBackgroundColor(Color.TRANSPARENT);
                    deleteBtn.setBackgroundColor(Color.TRANSPARENT);
                    transcodeBtn.setBackgroundColor(Color.TRANSPARENT);
                }
                playBtn.setVisibility(android.view.View.VISIBLE);
                downloadBtn.setVisibility(android.view.View.VISIBLE);
                deleteBtn.setVisibility(item.isRecording() ? android.view.View.VISIBLE : android.view.View.GONE);
                transcodeBtn.setVisibility(android.view.View.VISIBLE);
                playBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Item item = (Item) listable;
                        if (getArguments() != null)
                            getArguments().putBoolean(MediaActivity.GRAB_FOCUS, false);
                        mediaActivity.playItem(item);
                    }
                });
                deleteBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Recording recording = (Recording) listable;
                        int size = mediaActivity.getListables().size();
                        if (size == 1 || size == mediaActivity.getSelItemIndex() + 1)
                            mediaActivity.setSelItemIndex(mediaActivity.getSelItemIndex() - 1);
                        mediaActivity.deleteRecording(recording);
                    }
                });
                transcodeBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Item item = (Item) listable;
                        mediaActivity.transcodeItem(item);
                    }
                });
                downloadBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Item item = (Item) listable;
                        mediaActivity.downloadItem(item);
                    }
                });

                if (grabFocus)
                    playBtn.requestFocus();

                if (getAppSettings().isTv())
                    detailView.findViewById(R.id.detailScroll).setFocusable(false);

                String artSg = getAppSettings().getArtworkStorageGroup(item.getType());
                if (!AppSettings.ARTWORK_NONE.equals(artSg)) {
                    ArtworkDescriptor art = item.getArtworkDescriptor(artSg);
                    if (art != null) {
                        artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
                        try {
                            String filePath = item.getType() + "/" + mediaActivity.getPath() + "/" + art.getArtworkPath();
                            Bitmap bitmap = MediaActivity.getAppData().getImageBitMap(filePath);
                            if (bitmap == null) {
                                URL url = new URL(getAppSettings().getMythTvContentServiceBaseUrl() + "/" + art.getArtworkContentServicePath());
                                String filepath = item.getType() + "/" + mediaActivity.getPath() + "/" + art.getArtworkPath();
                                new ImageRetrievalTask(filepath, getAppSettings().isErrorReportingEnabled()).execute(url);
                            } else {
                                artworkView.setImageBitmap(bitmap);
                            }
                            if (!getAppSettings().isTv()) {
                                artworkView.setClickable(true);
                                artworkView.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        Item item = (Item) listable;
                                        mediaActivity.playItem(item);
                                    }
                                });
                            } else {
                                artworkView.setFocusable(false);
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                            Toast.makeText(mediaActivity, ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(mediaActivity, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    class ImageRetrievalTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;
        private Bitmap bitmap;
        private final String filepath;
        private final boolean reportErrors;

        ImageRetrievalTask(String filepath, boolean reportErrors) {
            this.filepath = filepath;
            this.reportErrors = reportErrors;
        }

        protected Long doInBackground(URL... urls) {
            try {
                bitmap = MediaActivity.getAppData().readImageBitmap(filepath);
                if (bitmap == null && mediaActivity != null) { // media activity could be null in this thread
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Loading image from url: " + urls[0]);
                    HttpHelper downloader = new HttpHelper(urls, getAppSettings().getMythTvServicesAuthType(), getAppSettings().getPrefs(), true);
                    downloader.setCredentials(getAppSettings().getMythTvServicesUser(), getAppSettings().getMythTvServicesPassword());
                    try {
                        byte[] imageBytes = downloader.get();
                        MediaActivity.getAppData().writeImage(filepath, imageBytes);
                    } catch (EOFException ex) {
                        // try again
                        byte[] imageBytes = downloader.get();
                        MediaActivity.getAppData().writeImage(filepath, imageBytes);
                    } catch (IOException ex) {
                        // fail silently
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                }

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (reportErrors)
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (reportErrors)
                        new Reporter(ex).send();
                    if (getActivity() != null) // activity might be null in this thread
                        Toast.makeText(getActivity(), ex.toString(), Toast.LENGTH_LONG).show();
                }
            } else {
                try {
                    artworkView.setImageBitmap(MediaActivity.getAppData().readImageBitmap(filepath));
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }
    }
}
