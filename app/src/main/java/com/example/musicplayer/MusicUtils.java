package com.example.musicplayer;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐工具类
 */
public class MusicUtils {
    private static final String TAG = "MusicUtils";

    /**
     * getMusicData()方法扫描系统里的歌曲，放在list集合中
     */
    public static List<Song> getMusicData(){
        List<Song> list=new ArrayList<>();
        //查询媒体库并将结果放在cursor对象中
        Cursor cursor=MyApplication.getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,null,null,MediaStore.Audio.AudioColumns.IS_MUSIC);
        if(cursor!=null){
            while (cursor.moveToNext()){
                Song song=new Song();

                //替换音乐名称的后缀.mp3
                String s=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                song.setSong(s.replace(".mp3",""));
                song.setSinger(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                song.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                song.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                song.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)));
                //String albumId=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                String uri=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                song.setImage(getAlbum(uri));

                //分离出歌曲名和歌手。因为本地媒体库读取的歌曲信息不规范
                if(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))>1000*60){
                    if(song.getSong().contains("-")){
                        String[] str=song.getSong().split("-");
                        song.setSong(str[0]);
                        song.setSinger(str[1]);
                    }
                    list.add(song);
                }
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 定义formatTime()方法用来格式化获取到的时间，因为取得的时间是以毫秒为单位的
     */
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return time / 1000 / 60 + ":0" + time / 1000 % 60;
        } else {
            return time / 1000 / 60 + ":" + time / 1000 % 60;
        }
    }

//    /**
//     * getAlbum()方法根据图片id获取图片
//     */
//    public static Bitmap getAlbum(String str){
//        String uriAlbums="content://media/external/audio/albums";
//        //album_art字段存储的是音乐图片的路径
//        String[] projection = new String[]{"album_art"};
//        Cursor cursor=MyApplication.getContext().getContentResolver().query(Uri.parse(uriAlbums+"/"+str),
//                projection,null,null,null);
//        String album_art=null;
//        if(cursor.getCount()>0 && cursor.getColumnCount()>0){
//            cursor.moveToNext();
//            album_art=cursor.getString(0);
//        }
//        cursor.close();
//
//        //判断album_art是否为空，不为空就将该歌曲的专辑图片地址传进去，为空就使用默认专辑图片
//        Bitmap bitmap;
//        if(album_art != null){
//            bitmap= BitmapFactory.decodeFile(album_art);
//        }else {
//            bitmap=BitmapFactory.decodeResource(MyApplication.getContext().getResources(),R.drawable.moren);
//        }
//        return bitmap;
//    }

    /**
     * 加载封面
     * @param mediaUri MP3文件路径
     */
    public static Bitmap getAlbum(String mediaUri) {
        MediaMetadataRetriever mediaMetadataRetriever=new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mediaUri);
        byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();
        Bitmap bitmap ;
        bitmap= BitmapFactory.decodeByteArray(picture,0,picture.length);
        if(bitmap!=null) {
            return bitmap;
        }else {
            bitmap=BitmapFactory.decodeResource(MyApplication.getContext().getResources(),R.drawable.moren);
            return bitmap;
        }
    }
}
