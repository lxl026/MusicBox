package company.leon.musicbox;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int  START =1, PAUSE =2, CONTINUE=3, STOP =4, PULL=5, REFRESH=6, CHANGE=7;

    private java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("mm:ss");//时间格式转换

    List<Mp3Info> mp3Infos;
    int CurSongId = 0;//当前歌曲序号
    long Duration;//音乐长度 ms
    long CurTime;//当前播放位置 ms

    private IBinder mBinder;

    private ServiceConnection mConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = service;
            Log.d("Server","Connection****************************");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Server","Unconnection*****************************");
            mConnection = null;
        }
    };
    private Thread SeekBarRefresh;//seekBar更新线程 0.5s更新一次

    int flag=STOP;//当前状态

    Button PlayPause;
    Button Stop;
    Button Quit;

    ImageView imageView;
    TextView StatusText,LeftText,RightText,SongName_Singer;
    SeekBar seekBar;

    RecyclerView SongList;

    ObjectAnimator animator;//旋转对象

    boolean ShowList = false;



    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case START:
                    animator.start();
                    SongName_Singer.setText(mp3Infos.get(CurSongId).getTitle()+"--"+mp3Infos.get(CurSongId).getArtist());
                    StatusText.setText("Playing");
                    PlayPause.setText("PAUSE");
                    seekBar.setEnabled(true);
                    LeftText.setText("00:00");
                    RightText.setText(time.format(Duration));
                    break;
                case PAUSE:
                    animator.pause();
                    StatusText.setText("Paused");
                    PlayPause.setText("PLAY");
                    break;
                case CONTINUE:
                    animator.resume();
                    StatusText.setText("Playing");
                    PlayPause.setText("PAUSE");
                    break;
                case STOP:
                    animator.end();
                    StatusText.setText("Stopped");
                    LeftText.setText(time.format(0));
                    seekBar.setProgress(0);
                    seekBar.setEnabled(false);
                    PlayPause.setText("PLAY");
                    break;
                case REFRESH:
                    if(Duration>0){
                        long tmp = CurTime*100/Duration;
                        int i = (int) tmp;
                        seekBar.setProgress(i);
                        LeftText.setText(time.format(CurTime));
                    }
                    break;
                case PULL:
                    LeftText.setText(time.format(CurTime));
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //权限申请
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }

        FindSongs finder;
        finder = new FindSongs();
        mp3Infos = finder.getMp3Infos(this.getContentResolver());
        final BaseRecyclerAdapter SongListAdapter;
        SongListAdapter = new BaseRecyclerAdapter<Mp3Info>(this,mp3Infos,R.layout.musiclist){
            @Override
            public void convert(BaseRecyclerHolder holder, Mp3Info item) {
                TextView SongName = holder.getView(R.id.SongName);
                TextView Artist = holder.getView(R.id.artist);
                SongName.setText(item.getTitle());
                Artist.setText(item.getArtist());
            }
        };
        SongListAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener(){
            @Override
            public void onClick(int position) {
                if(flag==STOP){//从停止到启动
                    CurSongId = position;
                    flag = START;

                    // 启动seekbar更新线程
                    SeekBarRefresh = new Thread(new SeekBarThread());
                    SeekBarRefresh.start();

                    //启动服务
                    mBinder=new Binder() ;
                    Intent intent = new Intent(MainActivity.this,MyService.class);
                    startService(intent);
                    Log.d("Main","StartService*******************");
                    //绑定
                    bindService(intent,mConnection,BIND_AUTO_CREATE);
                    Log.d("Main","BindService*******************");

                    //子线程，通信
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //此时服务可能还没建好，先等0.1s，不然信息传不过去，或者信息传过去了，mediaplayer还没初始化，然后就崩了
                            SystemClock.sleep(100);
                            try{
                                int code = START;
                                Parcel data = Parcel.obtain();
                                data.writeString(mp3Infos.get(CurSongId).getUrl());
                                Parcel reply = Parcel.obtain();
                                mBinder.transact(code,data,reply,0);
                                Log.d("Main","mediaPlayer.start*******************");
                            }catch (RemoteException e){
                                e.printStackTrace();
                            }
                            Duration=mp3Infos.get(CurSongId).getDuration();//得到歌曲时间长度
                            //handler通信
                            Message msg = new Message();
                            msg.what = START;
                            handler.sendMessage(msg);
                        }
                    }).start();

                }
                else{
                    boolean isSuc = true;
                    if(position!=CurSongId){//换歌
                        flag = START;
                        CurSongId = position;
                        try{
                            int code = CHANGE;
                            Parcel data = Parcel.obtain();
                            data.writeString(mp3Infos.get(CurSongId).getUrl());
                            Parcel reply = Parcel.obtain();
                            mBinder.transact(code,data,reply,0);
                            if(reply.readInt() ==0) isSuc = false;
                            Log.d("Main","mediaPlayer.start*******************");
                        }catch (RemoteException e){
                            e.printStackTrace();
                        }
                        if(isSuc){
                            Duration=mp3Infos.get(CurSongId).getDuration();//得到歌曲时间长度
                            //handler通信
                            Message msg = new Message();
                            msg.what = START;
                            handler.sendMessage(msg);
                        }
                        else {

                            SongListAdapter.delete(CurSongId);
                        }

                    }
                    else {
                        if(flag==PAUSE){//继续
                            flag = CONTINUE;

                            try{
                                int code = CONTINUE;
                                Parcel data = Parcel.obtain();
                                Parcel reply = Parcel.obtain();
                                mBinder.transact(code,data,reply,0);
                                Log.d("Main","startMediaPlayer*******************");
                            }catch (RemoteException e){
                                e.printStackTrace();
                            }

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Message msg = new Message();
                                    msg.what = CONTINUE;
                                    handler.sendMessage(msg);
                                }
                            }).start();

                        }
                    }
                }

            }

            @Override
            public void onLongClick(int position) {

            }
        } );
        SongList = (RecyclerView) findViewById(R.id.SongRecyclerView);
        SongList.setLayoutManager(new LinearLayoutManager(this));
        SongList.setAdapter(SongListAdapter);
        SongList.setVisibility(View.INVISIBLE);

        seekBar = (SeekBar) findViewById(R.id.seekBar) ;
        seekBar.setEnabled(false);//不可拖动
        PlayPause = (Button) findViewById(R.id.PlayButton);
        Stop = (Button) findViewById(R.id.StopButton);
        Quit = (Button) findViewById(R.id.QuitButton);
        imageView = (ImageView) findViewById(R.id.image);
        SongName_Singer = (TextView) findViewById(R.id.SongName_Singer);
        StatusText = (TextView) findViewById(R.id.Status);
        LeftText = (TextView) findViewById(R.id.LeftText);
        RightText = (TextView) findViewById(R.id.RightText);

        //图片圆形旋转，线性，无限循环，周期为5s
        animator = ObjectAnimator.ofFloat(imageView,"rotation",0f,360f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        animator.setDuration(5000);

        PlayPause.setOnClickListener(this);
        Stop.setOnClickListener(this);
        Quit.setOnClickListener(this);



        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int lastFlag;
            boolean Only;
            //SeekBar开始滚动的回调函数
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                lastFlag = flag;
                Only = true;
                flag = PULL;
            }

            // SeekBar滚动时的回调函数
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CurTime = progress*Duration/100;
                Message msg = new Message();
                msg.what = PULL;
                handler.sendMessage(msg);
            }

            // SeekBar结束滚动时的回调函数
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try{
                    int code = PULL;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeLong(CurTime);
                    mBinder.transact(code,data,reply,0);
                    Log.d("Main","mediaPlayer.pull*******************");
                }catch (RemoteException e){
                    e.printStackTrace();
                }
                if(Only){
                    flag = lastFlag;
                    Only= false;
                }
            }
        });
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.songList:
                if(ShowList){
                    SongList.setVisibility(View.INVISIBLE);
                    ShowList = false;
                    item.setIcon(R.drawable.ic_toc_white_18dp);
                }
                else {
                    SongList.setVisibility(View.VISIBLE);
                    ShowList = true;
                    item.setIcon(R.drawable.ic_arrow_drop_up_white_18dp);
                }
                break;
            default:break;
        }


        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){

                }else {
                    Toast.makeText(this,"拒绝权限无法使用程序",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.PlayButton:
                if(flag==STOP){//从停止到启动
                    flag = START;

                    //启动服务
                    mBinder=new Binder() ;
                    Intent intent = new Intent(this,MyService.class);
                    startService(intent);

                    // 启动seekbar更新线程
                    SeekBarRefresh = new Thread(new SeekBarThread());
                    SeekBarRefresh.start();
                    Log.d("Main","StartService*******************");
                    //绑定
                    bindService(intent,mConnection,BIND_AUTO_CREATE);
                    Log.d("Main","BindService*******************");

                    //子线程，通信
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //此时服务可能还没建好，先等0.1s，不然信息传不过去，或者信息传过去了，mediaplayer还没初始化，然后就崩了
                            SystemClock.sleep(500);
                            try{
                                int code = START;
                                Parcel data = Parcel.obtain();
                                data.writeString(mp3Infos.get(CurSongId).getUrl());
                                Parcel reply = Parcel.obtain();
                                mBinder.transact(code,data,reply,0);
                                Duration=mp3Infos.get(CurSongId).getDuration();//得到歌曲时间长度
                                Log.d("Main","mediaPlayer.start*******************");
                            }catch (RemoteException e){
                                e.printStackTrace();
                            }
                            //handler通信
                            Message msg = new Message();
                            msg.what = START;
                            handler.sendMessage(msg);
                        }
                    }).start();

                }else if(flag==START || flag==CONTINUE){//暂停
                    flag = PAUSE;

                    try{
                        int code = PAUSE;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","mediaPlayer.pause*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = PAUSE;
                            handler.sendMessage(msg);
                        }
                    }).start();

                }else if(flag==PAUSE){//继续
                    flag = CONTINUE;

                    try{
                        int code = CONTINUE;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","startMediaPlayer*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = CONTINUE;
                            handler.sendMessage(msg);
                        }
                    }).start();

                }
                break;
            case R.id.StopButton:
                if(flag!=STOP){//停止
                    flag = STOP;
                    try{
                        int code = STOP;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","mediaPlayer.stop*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = STOP;
                            handler.sendMessage(msg);
                        }
                    }).start();

                    //关闭服务
                    unbindService(mConnection);
                    Log.d("Main","UnBindService*******************");
                    Intent intent = new Intent(this,MyService.class);
                    stopService(intent);
                    Log.d("Main","StopService*******************");
                }
                break;
            case R.id.QuitButton:
                Log.d("Main","ClickQuit*******************");
                if(flag != STOP)
                {
                    //关闭服务
                    unbindService(mConnection);
                    Log.d("Main","UnBindService*******************");
                    Intent intent = new Intent(this,MyService.class);
                    stopService(intent);
                    Log.d("Main","StopService*******************");
                }

                try{
                    MainActivity.this.finish();
                    System.exit(0);
                }catch (Exception e){
                    e.printStackTrace();
                }
        }
    }

    // 自定义的线程
    class SeekBarThread implements Runnable {

        @Override
        public void run() {
            while (flag != STOP){
                while (flag==START || flag==CONTINUE) {
                    try {
                        // 休眠，每500毫秒更新一次
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 将SeekBar位置设置到当前播放位置
                    try{
                        if(flag==START || flag==CONTINUE){
                            int code = REFRESH;
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            mBinder.transact(code,data,reply,0);
                            CurTime=reply.readInt();//得到当前播放位置
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }

                    if(flag != PULL){
                        Message msg = new Message();
                        msg.what = REFRESH;
                        handler.sendMessage(msg);
                    }


                }
            }

        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
