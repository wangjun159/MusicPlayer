package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {

    public  static MediaPlayer mediaPlayer;
    private static final String TAG = "MusicService";
    private MusicBinder musicBinder=new MusicBinder();

    private static int pos;
    private  List<Song> songList=new ArrayList<>();

    private RemoteViews views;
    private Notification notification;
    private NotificationManager notificationManager;
    private BroadcastReceiver musicReceiver;

    //定义播放模式
    private boolean singlePlay=false;//单曲循环
    private boolean randomPlay=false;//随机播放
    private boolean sequencePlay=false;//顺序播放

    public static int getPos() {
        return pos;
    }

    public  class MusicBinder extends Binder{

        //设置单曲循环
        public void setSinglePlay(){
            singlePlay=true;
            randomPlay=false;
            sequencePlay=false;
        }

        //设置随机播放
        public void setRandomPlay(){
            singlePlay=false;
            randomPlay=true;
            sequencePlay=false;
        }

        //设置顺序播放
        public void setSequencePlay(){
            singlePlay=false;
            randomPlay=false;
            sequencePlay=true;
        }

        //调用该方法时传入list和position两个参数，并开始播放
        public void setMediaPlayer(int position){
            try {
                pos=position;
                //重置MediaPlayer
                mediaPlayer.reset();
                mediaPlayer.setDataSource(songList.get(pos).getPath());
                //让MediaPlayer处于准备状态,异步准备资源防止卡顿
                mediaPlayer.prepareAsync();
                //调用音频的监听方法，音频准备完毕后响应该方法进行音乐播放
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                    }
                });
                //对音乐播放状态进行监听，如果播放完毕就播放下一首
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if(sequencePlay==true) {
                            nextMusic();
                        }else if(singlePlay==true){
                            setMediaPlayer(pos);
                        }else if(randomPlay==true){
                            Random random=new Random();
                            int i=random.nextInt(songList.size());
                            setMediaPlayer(i);
                        }else {
                            nextMusic();
                        }
                    }
                });
                updateNotification();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //playMusic()方法控制音乐的播放与暂停
        public void playMusic(){
            if(mediaPlayer!=null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                updateNotification();
            }
        }

        //nextMusic()方法控制音乐播放下一曲
        public void nextMusic(){
            if(mediaPlayer!=null) {
                if (pos == songList.size() - 1) {
                    pos = 0;
                } else {
                    pos++;
                }
                setMediaPlayer(pos);
                updateNotification();
            }
        }

        //previousMusic()方法控制音乐播放上一曲
        public void previousMusic(){
            if(mediaPlayer!=null) {
                if (pos == 0) {
                    pos = songList.size() - 1;
                } else {
                    pos--;
                }
                setMediaPlayer(pos);
                updateNotification();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer=new MediaPlayer();
        this.songList=MainActivity.getSongList();
        //恢复上次退出时的音乐状态
//        SharedPreferences preferences = getSharedPreferences("Last Information", MODE_PRIVATE);
//        try {
//            if (preferences.getInt("currentPosition", 0) != 0) {
//                pos=preferences.getInt("position",0);
//                mediaPlayer.setDataSource(preferences.getString("path", null));
//                mediaPlayer.prepare();
//                mediaPlayer.seekTo(preferences.getInt("currentPosition", 0));
//            }
//        }catch (IOException e){
//            e.printStackTrace();
//        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {



        // 设置点击通知结果
        Intent clickIntent=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,clickIntent,0);


        //自定义标题栏布局
        views=new RemoteViews(this.getPackageName(),R.layout.notification_layout);
        //通过广播的方式实现自定义标题栏按键功能实现
        //上一首
        Intent intentPrevious=new Intent("previousMusic");
        PendingIntent previousPi=PendingIntent.getBroadcast(this,1,intentPrevious,0);
        views.setOnClickPendingIntent(R.id.notification_previous,previousPi);

        //播放和暂停
        Intent intentPlay=new Intent("playMusic");
        PendingIntent playPi=PendingIntent.getBroadcast(this,2,intentPlay,0);
        views.setOnClickPendingIntent(R.id.notification_play,playPi);

        //下一首
        Intent intentNext=new Intent("nextMusic");
        PendingIntent nextPi=PendingIntent.getBroadcast(this,3,intentNext,0);
        views.setOnClickPendingIntent(R.id.notification_next,nextPi);

        //退出
        Intent intentQuit=new Intent("quit");
        PendingIntent quitPi=PendingIntent.getBroadcast(this,4,intentQuit,0);
        views.setOnClickPendingIntent(R.id.notification_quit,quitPi);

        //实例化NotificationManager
        notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //判断系统版本
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            String CHANNEL_ID="MusicPlayer";
            String CHANNEL_NAME = "音乐";
            NotificationChannel channel=new NotificationChannel(CHANNEL_ID,CHANNEL_NAME,NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
            notification=new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.drawable.small)
                    .setCustomBigContentView(views)
                    .setContent(views)
                    .setContentIntent(pi)
                    .setWhen(System.currentTimeMillis())
                    .build();
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.small)
                    .setCustomBigContentView(views)
                    .setContent(views)
                    .setContentIntent(pi)
                    .setWhen(System.currentTimeMillis())
                    .build();
        }

        startForeground(111,notification);

        //注册广播接收器
        musicReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()){
                    case "previousMusic":
                        musicBinder.previousMusic();
                        break;
                    case "playMusic":
                        musicBinder.playMusic();
                        break;
                    case "nextMusic":
                        musicBinder.nextMusic();
                        break;
                    case "quit":
                        stopForeground(true);
                        stopSelf();
                        break;
                    default:
                }
            }
        };
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction("previousMusic");
        intentFilter.addAction("playMusic");
        intentFilter.addAction("nextMusic");
        intentFilter.addAction("quit");
        registerReceiver(musicReceiver,intentFilter);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新Notification界面显示
     */
    private void updateNotification(){
        if(views!=null){
            views.setTextViewText(R.id.notification_song,songList.get(pos).getSong());
            views.setTextViewText(R.id.notification_singer,songList.get(pos).getSinger());
            views.setImageViewBitmap(R.id.notification_image,songList.get(pos).getImage());
            if(mediaPlayer.isPlaying()){
                views.setImageViewResource(R.id.notification_play,R.drawable.play);
            }else {
                views.setImageViewResource(R.id.notification_play,R.drawable.pause);
            }
        }
        //刷新
        notificationManager.notify(111,notification);
    }

    /**
     * saveLastMusic()方法用于保存退出服务时当前播放歌曲的播放进度
     */
//    public void saveLastMusic(){
//        SharedPreferences.Editor editor=getSharedPreferences("Last Information",MODE_PRIVATE).edit();
//        editor.putInt("position",pos);
//        editor.putInt("currentPosition",mediaPlayer.getCurrentPosition());
//        Log.d(TAG, "saveLastMusic: --------------------"+songList.get(pos).getPath());
//        editor.putString("path",songList.get(pos).getPath());
//        editor.apply();
//    }

    /**
     * 在服务销毁时，释放相关资源，保存音乐状态信息
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        stopForeground(true);
        if(notificationManager!=null){
            notificationManager.cancel(111);
        }
        if(musicReceiver!=null) {
            unregisterReceiver(musicReceiver);
        }
    }
}