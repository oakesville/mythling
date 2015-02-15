/**
 * Copyright 2014 Donald Oakes
 *
 * This file is part of Mythling.
 *
 * Mythling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mythling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mythling.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.oakesville.mythling;

import java.io.EOFException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.ArtworkDescriptor;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.media.Video;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.Reporter;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = ItemDetailFragment.class.getSimpleName();

    private MediaActivity mediaActivity;
    private View detailView;
    private ImageView artworkView;
    private Listable listable;
    private int idx;
    private boolean grabFocus;

    private int[] ratingViewIds = new int[]{R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        idx = getArguments() == null ? 1 : getArguments().getInt("idx");
        grabFocus = getArguments() == null ? false : getArguments().getBoolean("grab");
    }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof MediaPagerActivity) {
            detailView = inflater.inflate(R.layout.detail, container, false);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // always vertical
                detailView = inflater.inflate(R.layout.detail_pane_vertical, container, false);
            } else {
                // poster-type artwork goes horizontal
                if (AppSettings.DEFAULT_ARTWORK_SG.equals(getAppSettings().getArtworkStorageGroup(mediaActivity.getMediaType())))
                    detailView = inflater.inflate(R.layout.detail_pane_horizontal, container, false);
                else
                    detailView = inflater.inflate(R.layout.detail_pane_vertical, container, false);
            }
        }
        return detailView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaActivity.refreshing)
            return;

        populate();
    }

    private void populate() {

        listable = mediaActivity.getItems().get(idx);

        TextView titleView = (TextView) detailView.findViewById(R.id.titleText);

        if (listable instanceof Category) {
            Category category = (Category) listable;

            titleView.setText(category.getName());
            titleView.setMovementMethod(LinkMovementMethod.getInstance());
            Spannable spans = (Spannable) titleView.getText();
            ClickableSpan clickSpan = new ClickableSpan() {
                public void onClick(View v) {
                    ((TextView) v).setBackgroundColor(Color.GRAY);
                    Uri.Builder builder = new Uri.Builder();
                    builder.path(mediaActivity.getPath().length() == 0 ? listable.toString() : mediaActivity.getPath() + "/" + listable.toString());
                    Uri uri = builder.build();
                    startActivity(new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaPagerActivity.class));
                }
            };
            spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
            Drawable folder = getResources().getDrawable(R.drawable.folder);
            artworkView.setImageDrawable(folder);
            artworkView.setClickable(true);
            artworkView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ((ImageView) v).setBackgroundResource(R.drawable.rounded_frame_active);
                    Uri.Builder builder = new Uri.Builder();
                    builder.path(mediaActivity.getPath().length() == 0 ? listable.toString() : mediaActivity.getPath() + "/" + listable.toString());
                    Uri uri = builder.build();
                    startActivity(new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaPagerActivity.class));
                }
            });
        } else if (listable instanceof Item) {
            Item item = (Item) listable;

            titleView.setText(item.getLabel());

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
                TextView tvDir = (TextView) detailView.findViewById(R.id.directorText);
                if (video.getDirector() != null)
                    tvDir.setText("Directed by: " + video.getDirector());
                else
                    tvDir.setVisibility(View.GONE);

                TextView tvAct = (TextView) detailView.findViewById(R.id.actorsText);
                // actors
                if (video.getActors() != null)
                    tvAct.setText("Starring: " + video.getActors());
                else
                    tvAct.setVisibility(View.GONE);

                // summary
                if (video.getSummary() != null) {
                    TextView tv = (TextView) detailView.findViewById(R.id.summaryText);
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
                            TextView tv = (TextView) detailView.findViewById(R.id.customLink);
                            String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
                            tv.setText(Html.fromHtml("<a href='" + url + "'>" + host + "</a>"));
                            tv.setMovementMethod(LinkMovementMethod.getInstance());
                            tv.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    ((TextView) v).setBackgroundColor(Color.GRAY);
                                }
                            });
                        } catch (IOException ex) {
                            if (BuildConfig.DEBUG)
                                Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                        }
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
                            TextView tv = (TextView) detailView.findViewById(R.id.pageLink);
                            String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
                            tv.setText(Html.fromHtml("<a href='" + pageUrl + "'>" + host + "</a>"));
                            tv.setMovementMethod(LinkMovementMethod.getInstance());
                            tv.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    ((TextView) v).setBackgroundColor(Color.GRAY);
                                }
                            });
                        } catch (MalformedURLException ex) {
                            if (BuildConfig.DEBUG)
                                Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                            Toast.makeText(mediaActivity, ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else {
                if (item instanceof TvShow) {
                    ((TextView) detailView.findViewById(R.id.directorText)).setVisibility(View.GONE);
                    ((TextView) detailView.findViewById(R.id.actorsText)).setVisibility(View.GONE);

                    TvShow tvShow = (TvShow) item;
                    TextView tv = (TextView) detailView.findViewById(R.id.summaryText);
                    tv.setText(tvShow.getSummary());
                }
            }

            ImageButton button = (ImageButton) detailView.findViewById(R.id.pagerPlay);
            if (!getAppSettings().isFireTv())
                button.setBackgroundColor(Color.TRANSPARENT);
            button.setVisibility(android.view.View.VISIBLE);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Item item = (Item) listable;
                    item.setPath(mediaActivity.getPath());
                    mediaActivity.playItem(item);
                }
            });
            if (grabFocus)
                button.requestFocus();

            if (getAppSettings().isFireTv())
                detailView.findViewById(R.id.detailScroll).setFocusable(false);;

            String artSg = getAppSettings().getArtworkStorageGroup(item.getType());
            ArtworkDescriptor art = item.getArtworkDescriptor(artSg);
            if (art != null) {
                artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
                try {
                    String filePath = item.getType() + "/" + mediaActivity.getPath() + "/" + art.getArtworkPath();
                    Bitmap bitmap = MediaActivity.getAppData().getImageBitMap(filePath);
                    if (bitmap == null) {
                        URL url = new URL(getAppSettings().getMythTvContentServiceBaseUrl() + "/" + art.getArtworkContentServicePath());
                        new ImageRetrievalTask(item, art).execute(url);
                    } else {
                        artworkView.setImageBitmap(bitmap);
                    }
                    if (!getAppSettings().isFireTv()) {
                        artworkView.setClickable(true);
                        artworkView.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                Item item = (Item) listable;
                                item.setPath(mediaActivity.getPath());
                                mediaActivity.playItem(item);
                            }
                        });
                    } else {
                        artworkView.setFocusable(false);
                    }
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(mediaActivity, ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public class ImageRetrievalTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;
        private String filePath;
        private Bitmap bitmap;
        private Item item;
        private ArtworkDescriptor descriptor;

        ImageRetrievalTask(Item item, ArtworkDescriptor descriptor) {
            this.item = item;
            this.descriptor = descriptor;
        }

        protected Long doInBackground(URL... urls) {
            try {
                filePath = item.getType() + "/" + mediaActivity.getPath() + "/" + descriptor.getArtworkPath();
                bitmap = MediaActivity.getAppData().readImageBitmap(filePath);
                if (bitmap == null) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Loading image from url: " + urls[0]);
                    HttpHelper downloader = new HttpHelper(urls, getAppSettings().getMythTvServicesAuthType(), getAppSettings().getPrefs(), true);
                    downloader.setCredentials(getAppSettings().getMythTvServicesUser(), getAppSettings().getMythTvServicesPassword());
                    try {
                        byte[] imageBytes = downloader.get();
                        MediaActivity.getAppData().writeImage(filePath, imageBytes);
                    } catch (EOFException ex) {
                        // try again
                        byte[] imageBytes = downloader.get();
                        MediaActivity.getAppData().writeImage(filePath, imageBytes);
                    } catch (IOException ex) {
                        // fail silently
                        if (BuildConfig.DEBUG)
                            Log.e(TAG, ex.getMessage(), ex);
                    }
                }

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(mediaActivity, ex.toString(), Toast.LENGTH_LONG).show();
                }
            } else {
                try {
                    artworkView.setImageBitmap(MediaActivity.getAppData().readImageBitmap(filePath));
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }
    }
}
