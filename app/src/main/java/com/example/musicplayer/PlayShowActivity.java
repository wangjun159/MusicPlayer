package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class PlayShowActivity extends AppCompatActivity implements View.OnClickListener {

    protected static final float FLIP_DISTANCE = 150;
    private static final String TAG = "PlayShowActivity";
    private MyGestureListener myGestureListener;
    private GestureDetector detector;
    private ImageView imageView;
    private TextView songName;
    private TextView singer;
    public SeekBar seekBar;
    private TextView playTime;
    private TextView duration;
    private ImageButton previous;
    private ImageButton play;
    private ImageButton next;
    private List<Song> songList=new ArrayList<>();
    private static final int UPDATE=0;
    private MusicService.MusicBinder binder;
    private Thread myThread;

    private int pos=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_show);

        songList=MainActivity.getSongList();

        //开启线程
        myThread=new Thread(new MyThread());
        myThread.start();

        //开始绑定服务
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);

        //实例化布局中的各控件
        imageView=(ImageView) findViewById(R.id.playing_image);
        songName=(TextView) findViewById(R.id.playing_song);
        singer=(TextView) findViewById(R.id.playing_singer);
        seekBar=(SeekBar) findViewById(R.id.seekBar);
        playTime=(TextView) findViewById(R.id.playing_time);
        duration=(TextView) findViewById(R.id.playing_duration);
        previous=(ImageButton) findViewById(R.id.playing_previous);
        play=(ImageButton) findViewById(R.id.playing_play);
        next=(ImageButton) findViewById(R.id.playing_next);

        previous.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);

        //实例化SimpleOnGestureListener与GestureDetector对象并设置监听事件
        myGestureListener=new MyGestureListener();
        detector=new GestureDetector(this,myGestureListener);

        //给SeekBar添加监听事件，实现拖动进度条改变音乐进度
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser==true){
                    //获取进度条改变后的位置并播放
                    MusicService.mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                MusicService.mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MusicService.mediaPlayer.start();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    /**
     * 自定义一个MyGestureListener类继承view的GestureListener类，用于监听屏幕的左右滑动手势
     */
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(e2.getX()-e1.getX()>FLIP_DISTANCE){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    finishAfterTransition();
                }else {
                    finish();
                }
            }
            return true;
        }
    }

    //定义一个线程用于帮助handler实时更新UI
    private class MyThread implements Runnable{

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()){
                if(MusicService.mediaPlayer!= null ) {
                    //Log.d(TAG, "线程-----------"+myThread.isAlive());
                    Message message = new Message();
                    message.what = UPDATE;
                    //取得服务正在播放的音乐的position参数
                    message.arg1 = MusicService.getPos();
                    handler.sendMessage(message);
                    try {
                        //每过500毫秒就更新一次
                        Thread.sleep(500);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        }
    }

    //使用handler接收线程发出的消息，通知是否进行数据更新
    @SuppressLint("HandlerLeak")
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE:
                    if(pos!=msg.arg1){
                        pos=msg.arg1;
                        initMediaPlayer();
                    }
                    //设置音乐播放时的实时进度
                    playTime.setText(MusicUtils.formatTime(MusicService.mediaPlayer.getCurrentPosition()));
                    seekBar.setProgress(MusicService.mediaPlayer.getCurrentPosition());
                    break;
                default:
            }
        }
    };

    //绑定服务
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder=(MusicService.MusicBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    //设置点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playing_play:
                binder.playMusic();
                if(MusicService.mediaPlayer.isPlaying()){
                    play.setImageResource(R.drawable.play);
                }else {
                    play.setImageResource(R.drawable.pause);
                }
                break;
            case R.id.playing_previous:
                binder.previousMusic();
                play.setImageResource(R.drawable.play);
                break;
            case R.id.playing_next:
                binder.nextMusic();
                play.setImageResource(R.drawable.play);
                break;
            default:
        }
    }

    /**
     *  调用initMediaPlayer()方法对播放界面的UI进行设置
     */
    private void initMediaPlayer(){
        Glide.with(PlayShowActivity.this).load(songList.get(pos).getImage()).into(imageView);
        songName.setText(songList.get(pos).getSong());
        singer.setText(songList.get(pos).getSinger());
        String time=MusicUtils.formatTime(songList.get(pos).getDuration());
        duration.setText(time);
        seekBar.setMax(songList.get(pos).getDuration());
    }

    /**
     * 活动结束时解除与服务的绑定和停止线程
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        myThread.interrupt();
    }
}
