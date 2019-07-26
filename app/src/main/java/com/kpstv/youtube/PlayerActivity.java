package com.kpstv.youtube;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andexert.library.RippleView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.jgabrielfreitas.core.BlurImageView;
import com.kpstv.youtube.models.YTConfig;
import com.kpstv.youtube.utils.HttpHandler;
import com.kpstv.youtube.utils.YTutils;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.LoginException;

import at.huber.youtubeExtractor.Format;
import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;


public class PlayerActivity extends AppCompatActivity {

    String YouTubeUrl; BlurImageView backImage;
    NotificationManagerCompat notificationManager;
    RemoteViews collpaseView, expandedView;

    LinearLayout downloadButton; LinearLayout mainlayout;

    TextView mainTitle,viewCount,currentDuration,totalDuration;

    ImageView mainImageView; public  boolean isplaying=false, isfirst=true;

    ProgressBar mprogressBar;
    FloatingActionButton previousFab,playFab, nextFab;

    IndicatorSeekBar indicatorSeekBar; MediaPlayer mMediaPlayer;

    Notification notification;  NotificationCompat.Builder builder;

    ConnectionQuality connectionQuality = ConnectionQuality.MODERATE;

    private Handler mHandler = new Handler();

    SharedPreferences preferences;

    int total_duration=0; int total_seconds; List<String> yturls; int ytIndex=0;

    ArrayList<YTConfig> ytConfigs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the links loaded using schemes
        Intent appLinkIntent = getIntent();
        Uri appLinkData = appLinkIntent.getData();

        if (appLinkData!=null) {
            String url_link = appLinkData.toString();
            yturls = new ArrayList<>();
            yturls.add(url_link);
        }else {
            Intent intent = getIntent();
            yturls = Arrays.asList(intent.getStringArrayExtra("youtubelink"));
        }

        TextView tms = findViewById(R.id.termsText);

        preferences = getSharedPreferences("settings",MODE_PRIVATE);

        setTitle("");

        if (yturls.size()>0) {
            YouTubeUrl = yturls.get(ytIndex);
        }

        setNotification();

        getAllViews();

        mMediaPlayer = new MediaPlayer();
        ytConfigs = new ArrayList<>();

        playFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePlayBack(!isplaying);
            }
        });
        nextFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });
        previousFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        tms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YTutils.StartURL("https://google.com",PlayerActivity.this);
            }
        });

        indicatorSeekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateTimeTask);
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateTimeTask);

                // forward or backward to certain seconds
                mMediaPlayer.seekTo(10000);

                // update timer progress again
                updateProgressBar();
            }
        });


        downloadButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            100);
                    return;
                } else showListDialog();
            }
        });

        new setData().execute(YTutils.getVideoID(YouTubeUrl));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showListDialog();
                } else {
                    Toast.makeText(PlayerActivity.this,"Permission denied!",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'switch' lines to check for other
            // permissions this app might request
        }
    }

    void playPrevious() {
        if (ytIndex<=0) {
            Toast.makeText(getApplicationContext(),"No previous song in playlist",Toast.LENGTH_SHORT).show();
            return;
        }
        onClear();
        YouTubeUrl = yturls.get(ytIndex-1);
        ytIndex--;
        new setData().execute(YTutils.getVideoID(YouTubeUrl));
    }
    void playNext() {
        if ((ytIndex+1)==yturls.size()) {
           Toast.makeText(getApplicationContext(),"No new song in playlist",Toast.LENGTH_SHORT).show();
            return;
        }
        onClear();
        YouTubeUrl = yturls.get(ytIndex+1);
        ytIndex++;
        new setData().execute(YTutils.getVideoID(YouTubeUrl));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 200) {
            onClear();
            yturls = Arrays.asList(getIntent().getStringArrayExtra("youtubelink"));
            YouTubeUrl = yturls.get(0);
            new setData().execute(YTutils.getVideoID(YouTubeUrl));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getStringExtra("DO");
        Log.e("PRINTING_RESULT","Code: "+action);
        if (action==null) return;
        switch (action) {
            case "play":
                changePlayBack(!isplaying);
                break;
            case "next":
               playNext();
                break;
            case "previous":
              playPrevious();
                break;
            case "add":
              //TODO: Add to play list listener
                break;
        }
    }


    private void setNotification() {
        notificationManager = NotificationManagerCompat.from(this);

        collpaseView = new RemoteViews(getPackageName(),
                R.layout.notification_layout_small);

        expandedView = new RemoteViews(getPackageName(),
                R.layout.notification_layout);

        setListener();

        builder = new NotificationCompat.Builder(this,"channel_01")
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("YTApp")
                .setContent(collpaseView)
                .setCustomBigContentView(expandedView)
                .setOngoing(true);

        notification = builder.build();

        notificationManager.notify(1, notification);
    }



    class setData extends AsyncTask<String, String, Void> {

        String videoTitle,channelTitle,viewCounts,imgUrl;

        @Override
        protected void onPreExecute() {
            if (isfirst) {
                mainlayout.setVisibility(View.GONE);
            }
            playFab.setEnabled(false);
            mprogressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... arg0) {
            String url = "https://www.googleapis.com/youtube/v3/videos?id="+arg0[0]+"&key=AIzaSyBYunDr6xBmBAgyQx7IW2qc770aoYBidLw&part=statistics";
            HttpHandler sh = new HttpHandler();
            String json = sh.makeServiceCall(url);
            try {
                JSONObject statistics = new JSONObject(json).getJSONArray("items")
                        .getJSONObject(0).getJSONObject("statistics");

               // videoTitle = snippets.getString("title");
             //   channelTitle = snippets.getString("channelTitle");
                viewCounts = YTutils.getViewCount(Integer.parseInt(statistics.getString("viewCount")));
           //     imgUrl = snippets.getJSONObject("thumbnails").getJSONObject("medium").getString("url");


            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("PlayerActivity_JSON",e.getMessage());
            }
            return null;
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        protected void onPostExecute(Void aVoid) {

            new YouTubeExtractor(PlayerActivity.this) {


                String link;
                @Override
                protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                    if (ytFiles == null) {
                        showAlert("Failed!","Couldn't get the required audio stream. Try again!",true);
                        return;
                    }

                    YtFile ytaudioFile = getBestStream(ytFiles);
                    link = ytaudioFile.getUrl();

                    videoTitle = videoMeta.getTitle();
                    channelTitle = videoMeta.getAuthor();
                    imgUrl = videoMeta.getMqImageUrl();

                    for (int i = 0, itag; i < ytFiles.size(); i++) {
                        itag = ytFiles.keyAt(i);
                        YtFile ytFile = ytFiles.get(itag);

                        if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                             addFormatToList(videoMeta.getTitle(), ytFile);
                        }
                    }
                }

                @Override
                protected void onPostExecute(SparseArray<YtFile> ytFiles) {
                    super.onPostExecute(ytFiles);
                    mainTitle.setText(videoTitle);
                    collpaseView.setTextViewText(R.id.nTitle,videoTitle);
                    expandedView.setTextViewText(R.id.nTitle,videoTitle);
                    collpaseView.setTextViewText(R.id.nAuthor,channelTitle);
                    expandedView.setTextViewText(R.id.nAuthor,channelTitle);
                    viewCount.setText(viewCounts);
                    Glide.with(getApplicationContext())
                            .load(imgUrl)
                            .addListener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    backImage.setImageDrawable(resource);
                                    backImage.setBlur(5);
                                    mainImageView.setImageDrawable(resource);

                                    Bitmap icon = YTutils.drawableToBitmap(resource);

                                    collpaseView.setImageViewBitmap(R.id.nImage,icon);
                                    expandedView.setImageViewBitmap(R.id.nImage,icon);
                                    mprogressBar.setVisibility(View.GONE);
                                    notificationManager.notify(1,builder.build());

                                    Log.e("ImageUrl",imgUrl+"");
                                    Log.e("RTSP_URL",YTutils.getUrlVideoRTSP(YouTubeUrl)+"");

                                    playFab.setEnabled(true);
                                    try {
                                        if (mMediaPlayer != null) {
                                            try {
                                                Log.e("DataSrcLink",link+"");
                                                mMediaPlayer = new MediaPlayer();
                                                mMediaPlayer.setDataSource(link);
                                                mMediaPlayer.prepare();
                                                mMediaPlayer.start();
                                            }catch (Exception ex) {
                                                Log.e("DataSourceNull",ex.getMessage());
                                                showAlert("Failed!","Couldn't get the required audio stream",true);
                                            }
                                            Log.e("Duration","dur: "+mMediaPlayer.getDuration());
                                            total_duration = mMediaPlayer.getDuration();
                                            total_seconds = total_duration/1000;
                                            totalDuration.setText(YTutils.milliSecondsToTimer(total_duration));

                                            makePause();
                                            isplaying=true;

                                            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                @Override
                                                public void onPrepared(MediaPlayer mp) {
                                                    updateProgressBar();
                                                   // updateDuration();
                                                }
                                            });
                                            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                @Override
                                                public void onCompletion(MediaPlayer mp) {
                                                    makePlay();
                                                    isplaying=false;
                                                }
                                            });
                                        }
                                    } catch (Exception io) {
                                        io.printStackTrace();
                                    }
                                    mainlayout.setVisibility(View.VISIBLE);

                                    if (!preferences.getBoolean("isShownSeeek",false)) {
                                        showSeekBarDialog();
                                    }

                                    // Store video into history
                                    new saveToHistory().execute(YouTubeUrl);

                                    return true;
                                }
                            })
                            .into(backImage);
                }
            }.execute(YouTubeUrl);


            super.onPostExecute(aVoid);
        }
    }

    private class saveToHistory extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String url_link = strings[0];
            SharedPreferences pref = getSharedPreferences("history",MODE_PRIVATE);
            String set = pref.getString("urls","");

            // Get playlist
            ArrayList<String> urls = new ArrayList<>();
            if (!Objects.requireNonNull(set).isEmpty()) {
                urls.addAll(Arrays.asList(set.split(",")));
            }

            // Add to playlist by removing it first
            for (int i=0;i<urls.size();i++) {
                if (urls.get(i).contains(url_link)) {
                    urls.remove(i);
                }
            }
            String formattedDate = YTutils.getTodayDate();
            Log.e("StringtoAdd",url_link+"|"+formattedDate);
            urls.add(0,url_link+"|"+formattedDate);

            // Save playlist
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < urls.size(); i++) {
                sb.append(urls.get(i)).append(",");
            }
            SharedPreferences.Editor prefsEditor = pref.edit();
            prefsEditor.putString("urls", sb.toString());
            prefsEditor.apply();
            return null;
        }
    }

    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {

        connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        int[] itags = new int[]{251, 141, 140, 17};

        if (connectionQuality != null && connectionQuality != ConnectionQuality.UNKNOWN) {
            switch (connectionQuality) {
                case POOR:
                    itags = new int[]{17, 140, 251, 141};
                    break;
                case MODERATE:
                    itags = new int[]{251, 141, 140, 17};
                    break;
                case GOOD:
                case EXCELLENT:
                    itags = new int[]{141, 251, 140, 17};
                    break;
            }
        }

        if (ytFiles.get(itags[0]) != null) {
            return ytFiles.get(itags[0]);
        } else if (ytFiles.get(itags[1]) != null) {
            return ytFiles.get(itags[1]);
        } else if (ytFiles.get(itags[2]) != null) {
            return ytFiles.get(itags[2]);
        }
        return ytFiles.get(itags[3]);
    }

    private void setListener() {
        // Play or Pause listener
        Intent newintent=new Intent(PlayerActivity.this, PlayerActivity.class);
        newintent.putExtra("DO","play");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, newintent, 0);

        expandedView.setOnClickPendingIntent(R.id.nPlay,pendingIntent);
        collpaseView.setOnClickPendingIntent(R.id.nPlay,pendingIntent);

        // Next song Listener
        newintent = new Intent(PlayerActivity.this,PlayerActivity.class);
        newintent.putExtra("DO","next");
        pendingIntent = PendingIntent.getActivity(this,1,newintent,0);

        expandedView.setOnClickPendingIntent(R.id.nForward,pendingIntent);
        collpaseView.setOnClickPendingIntent(R.id.nForward,pendingIntent);

        // Previous song Listener
        newintent = new Intent(PlayerActivity.this,PlayerActivity.class);
        newintent.putExtra("DO","previous");
        pendingIntent = PendingIntent.getActivity(this,2,newintent,0);

        expandedView.setOnClickPendingIntent(R.id.nPrevious,pendingIntent);
        collpaseView.setOnClickPendingIntent(R.id.nPrevious,pendingIntent);

        // Add to playlist Listener
        newintent = new Intent(PlayerActivity.this,PlayerActivity.class);
        newintent.putExtra("DO","add");
        pendingIntent = PendingIntent.getActivity(this,3,newintent,0);

        expandedView.setOnClickPendingIntent(R.id.nAdd,pendingIntent);

    }

    @Override
    protected void onDestroy() {
        notificationManager.cancel(1);
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mHandler.removeCallbacks(mUpdateTimeTask);
        super.onDestroy();
    }

    void onClear() {
        mainlayout.setVisibility(View.GONE);
        mMediaPlayer.stop();
        mHandler.removeCallbacks(mUpdateTimeTask);
        isplaying=false;
        total_duration=0;total_seconds=0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_youtube) {
          YTutils.StartURLIntent(YouTubeUrl,this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        callFinish();
        return true;
    }

    public  void changePlayBack(boolean isplay) {
        Log.e("PlayingState","Playing State: "+isplaying+", isPlay:"+isplay);
        if (isplay) {

            makePause();
            notificationManager.notify(1,builder.build());
            mMediaPlayer.start();
         //   updateDuration();
        } else {

            makePlay();
            notificationManager.notify(1,builder.build());
            mMediaPlayer.pause();
           // mTimer.cancel();
        }
        Log.e("CurrentDur",mMediaPlayer.getCurrentPosition()+"");
        isplaying = isplay;
    }

    void makePlay() {
        playFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        collpaseView.setImageViewResource(R.id.nPlay,R.drawable.ic_play_notify);
        expandedView.setImageViewResource(R.id.nPlay,R.drawable.ic_play_notify);
        notificationManager.notify(1,builder.build());
    }
    void makePause() {
        playFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        collpaseView.setImageViewResource(R.id.nPlay,R.drawable.ic_pause_notify);
        expandedView.setImageViewResource(R.id.nPlay,R.drawable.ic_pause_notify);
        notificationManager.notify(1,builder.build());
    }

    private void getAllViews() {
        downloadButton = findViewById(R.id.downloadlayout);
        mprogressBar = findViewById(R.id.mainprogress);
        mainTitle = findViewById(R.id.maintitle);
        viewCount = findViewById(R.id.mainviews);
        currentDuration = findViewById(R.id.currentDur);
        totalDuration = findViewById(R.id.totalDur);
        mainImageView = findViewById(R.id.mainImage);
        previousFab = findViewById(R.id.rewindButton);
        playFab = findViewById(R.id.play_pause_button);
        nextFab = findViewById(R.id.forwardButton);
        indicatorSeekBar = findViewById(R.id.seekBar);
        mainlayout = findViewById(R.id.mainlayout);
        backImage = findViewById(R.id.background_image);
    }

    void showSeekBarDialog() {
        new AlertDialog.Builder(PlayerActivity.this)
                .setTitle("Audio Channel")
                .setMessage("Due to recent changes in YouTube api, audio seeking is not possible since it's a real-time " +
                        "stream.\n\nIt means you cannot change playback from seek bar control.")

                .setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("isShownSeeek",true);
                        editor.apply();
                    }
                })

                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void showAlert(String title, String message, boolean isalert) {
        int icon = android.R.drawable.ic_dialog_info;
        if (isalert) icon = android.R.drawable.ic_dialog_alert;
        new AlertDialog.Builder(PlayerActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callFinish();
                    }
                })
                .setIcon(icon)
                .show();
    }

    private void addFormatToList(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format

        Format ytFrVideo = ytfile.getFormat();

        String ytText;
        if (ytFrVideo.getHeight() == -1)
            ytText = "Audio " + ytFrVideo.getAudioBitrate() + " kbit/s";
        else
        {
            ytText = (ytFrVideo.getFps() == 60) ? "Video "+ ytFrVideo.getHeight() + "p60" :
                    "Video "+ytFrVideo.getHeight() + "p";
            if (ytfile.getFormat().getAudioBitrate()==-1) {
                ytText+=" (no audio)";
            }
        }

       /* Log.e(ytText,"BitRate: "+ytfile.getFormat().getAudioBitrate()+", Height: "+ytfile.getFormat().getHeight()*//*+", Video codec: "+
                ytfile.getFormat().getVideoCodec()+", isdash: "+ytfile.getFormat().isDashContainer()+", ishls: "+
                ytfile.getFormat().isHlsContent()*//*+", url: "+ytfile.getUrl());*/

        ytConfigs.add(new YTConfig(ytText,ytfile.getUrl(),ytfile.getFormat().getExt(),videoTitle));

       /* Button btn = new Button(this);
        btn.setText(btnText);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String filename;
                if (videoTitle.length() > 55) {
                    filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                } else {
                    filename = videoTitle + "." + ytfile.getFormat().getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
                finish();
            }
        });
        mainLayout.addView(btn);*/
    }

    void callFinish() {
        Intent i = new Intent(PlayerActivity.this, MainActivity.class);
        startActivity(i);
    }

    void showListDialog() {

        ArrayList<String> tmplist = new ArrayList<>();
        final ArrayList<YTConfig> configs = new ArrayList<>();

        for(int i=0;i<ytConfigs.size();i++) {
            String text = ytConfigs.get(i).getText();
            boolean isalreadyadded=false;
            for (int j=0;j<tmplist.size();j++) {
                if (tmplist.get(j).contains(text))
                    isalreadyadded=true;
            }
            if (!isalreadyadded){
                tmplist.add(ytConfigs.get(i).getText());
                configs.add(ytConfigs.get(i));
            }
        }

        final String[] arrays = new String[configs.size()];
        for(int i=0;i<configs.size();i++) {
            arrays[i]=configs.get(i).getText();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
        builder.setTitle("Select Media Codec");

        builder.setItems(arrays, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                YTConfig config = configs.get(which);
                String filename;
                if (config.getText().length() > 55) {
                    filename = config.getTitle().substring(0, 55) + "." + config.getExt();
                } else {
                    filename = config.getTitle() + "." + config.getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                downloadFromUrl(config.getUrl(),config.getTitle(),filename);

                Toast.makeText(PlayerActivity.this,"Download started",
                        Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
       /* AlertDialog.Builder builderSingle = new AlertDialog.Builder(PlayerActivity.this);
        builderSingle.setTitle("Select Media Codec");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(PlayerActivity.this,
                android.R.layout.select_dialog_singlechoice);

        for(YTConfig config : ytConfigs) {
            arrayAdapter.add(config.getText());
        }

        builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                YTConfig config = ytConfigs.get(which);
                String filename;
                if (config.getText().length() > 55) {
                    filename = config.getText().substring(0, 55) + "." + config.getExt();
                } else {
                    filename = config.getText() + "." + config.getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                downloadFromUrl(config.getUrl(),config.getText(),filename);

                FancyToast.makeText(PlayerActivity.this,"Download started",
                        FancyToast.LENGTH_SHORT,FancyToast.INFO,false).show();

            }
        });
        builderSingle.show();*/
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    // New methods...

    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mMediaPlayer.getDuration();
            long currentDur = mMediaPlayer.getCurrentPosition();

            // Displaying time completed playing
            currentDuration.setText(""+YTutils.milliSecondsToTimer(currentDur));

            // Updating progress bar
            int progress = (YTutils.getProgressPercentage(currentDur, totalDuration));
            //Log.d("Progress", ""+progress);
            indicatorSeekBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };
}