package com.example.musicplayer;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * RecyclerView的适配器
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    private List<Song> mSongList;
    private static final String TAG = "SongAdapter";

    /**
     * 定义接口，传递点击事件时的下标条目
     */
    public interface MyItemClickListener {
        //item的回调方法
        void onItemClick(int position);
    }

    //声明接口
    private MyItemClickListener listener;

    //set方法
    public void setListener(MyItemClickListener listener) {
        this.listener = listener;
    }

    //获取到子项布局的各部件实例
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView songName;
        TextView singer;
        TextView duration;
        ImageView imageView;
        View songView;

        public ViewHolder(View view){
            super(view);
            songView=view;
            songName=(TextView) view.findViewById(R.id.item_song_name);
            singer=(TextView) view.findViewById(R.id.item_song_singer);
            duration=(TextView) view.findViewById(R.id.item_song_duration);
            imageView=(ImageView) view.findViewById(R.id.item_song_image);
        }
    }
    //SongAdapter类的构造函数，用于数据传入
    public SongAdapter(List<Song> songList){
        mSongList=songList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item,parent,false);
        final ViewHolder holder=new ViewHolder(view);
        holder.songView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //调用接口方法传递下标
                listener.onItemClick(holder.getAdapterPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //设置子项数据的显示
        Song song=mSongList.get(position);
        holder.songName.setText(song.getSong());
        holder.singer.setText(song.getSinger());
        String time=MusicUtils.formatTime(song.getDuration());
        holder.duration.setText(time);
        Glide.with(MyApplication.getContext()).load(song.getImage()).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mSongList.size();
    }

}
