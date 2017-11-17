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
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public static final int  START =1, PAUSE =2, CONTINUE=3, STOP =4, PULL=5, REFRESH=6;
    private java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("mm:ss");
    int Duration;
    int CurTime;
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
    private Thread thread;
    int flag=STOP;
    Button PlayPause;
    Button Stop;
    Button Quit;
    ImageView imageView;
    TextView StatusText,LeftText,RightText;
    SeekBar seekBar;
    ObjectAnimator animator;
    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case START:
                    StatusText.setText("Playing");
                    PlayPause.setText("PAUSE");
                    animator.start();
                    RightText.setText(time.format(Duration));
                    seekBar.setEnabled(true);
                    break;
                case PAUSE:
                    StatusText.setText("Paused");
                    PlayPause.setText("PLAY");
                    animator.pause();
                    break;
                case CONTINUE:
                    StatusText.setText("Playing");
                    PlayPause.setText("PAUSE");
                    animator.start();
                    break;
                case STOP:
                    StatusText.setText("Stopped");
                    PlayPause.setText("PLAY");
                    animator.end();
                    seekBar.setProgress(0);
                    seekBar.setEnabled(false);
                    break;
                case REFRESH:
                    seekBar.setProgress(CurTime*100/Duration);
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
        seekBar = (SeekBar) findViewById(R.id.seekBar) ;
        seekBar.setEnabled(false);
        PlayPause = (Button) findViewById(R.id.PlayButton);
        Stop = (Button) findViewById(R.id.StopButton);
        Quit = (Button) findViewById(R.id.QuitButton);
        imageView = (ImageView) findViewById(R.id.image);
        animator = ObjectAnimator.ofFloat(imageView,"rotation",0f,360f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        animator.setDuration(5000);
        PlayPause.setOnClickListener(this);
        Stop.setOnClickListener(this);
        Quit.setOnClickListener(this);
        StatusText = (TextView) findViewById(R.id.Status);
        LeftText = (TextView) findViewById(R.id.LeftText);
        RightText = (TextView) findViewById(R.id.RightText);

        thread = new Thread(new SeekBarThread());
        // 启动线程
        thread.start();


        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }else{

        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // SeekBar滚动时的回调函数
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CurTime = progress*Duration/100;
            }

            //SeekBar开始滚动的回调函数
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                flag = PULL;
            }

            // SeekBar结束滚动时的回调函数
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try{
                    int code = PULL;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInt(CurTime);
                    mBinder.transact(code,data,reply,0);
                    Log.d("Main","mediaPlayer.pull*******************");
                }catch (RemoteException e){
                    e.printStackTrace();
                }
                flag=CONTINUE;
            }
        });
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
                if(flag==STOP){//启动
                    //启动服务
                    mBinder=new Binder() ;
                    Intent intent = new Intent(this,MyService.class);
                    startService(intent);
                    Log.d("Main","StartService*******************");
                    bindService(intent,mConnection,BIND_AUTO_CREATE);
                    Log.d("Main","BindService*******************");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(100);
                            try{

                                int code = START;
                                Parcel data = Parcel.obtain();
                                Parcel reply = Parcel.obtain();
                                mBinder.transact(code,data,reply,0);
                                Duration=reply.readInt();
                                System.out.println(Duration);
                                //Log.d("reply",)
                                Log.d("Main","mediaPlayer.start*******************");
                            }catch (RemoteException e){
                                e.printStackTrace();
                            }
                            Message msg = new Message();
                            msg.what = START;
                            handler.sendMessage(msg);
                            flag = START;
                        }
                    }).start();

                }else if(flag==START || flag==CONTINUE){//暂停
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = PAUSE;
                            handler.sendMessage(msg);
                            flag = PAUSE;
                        }
                    }).start();
                    try{
                        int code = PAUSE;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","mediaPlayer.pause*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }else if(flag==PAUSE){//继续
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = CONTINUE;
                            handler.sendMessage(msg);
                            flag = CONTINUE;
                        }
                    }).start();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(flag == START || flag == CONTINUE){
                                SystemClock.sleep(500);
                                try{
                                    int code = REFRESH;
                                    Parcel data = Parcel.obtain();
                                    Parcel reply = Parcel.obtain();
                                    mBinder.transact(code,data,reply,0);
                                    CurTime=reply.readInt();
                                    Log.d("Main","REFRESH.start*******************");
                                }catch (RemoteException e){
                                    e.printStackTrace();
                                }
                                Message msg = new Message();
                                msg.what = REFRESH;
                                handler.sendMessage(msg);
                            }

                        }
                    }).start();
                    try{
                        int code = CONTINUE;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","mediaPlayer.start*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.StopButton:
                if(flag!=STOP){//停止
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = STOP;
                            handler.sendMessage(msg);
                            flag = STOP;
                        }
                    }).start();

                    try{
                        int code = STOP;
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        mBinder.transact(code,data,reply,0);
                        Log.d("Main","mediaPlayer.stop*******************");
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
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
                //finish();
        }
    }

    // 自定义的线程
    class SeekBarThread implements Runnable {

        @Override
        public void run() {
            while (true){
                while (flag==START || flag==CONTINUE) {
                    // 将SeekBar位置设置到当前播放位置
                    try {
                        // 每100毫秒更新一次位置
                        Thread.sleep(1000);
                        //播放进度
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try{
                        if(flag==START || flag==CONTINUE){
                            int code = REFRESH;
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            mBinder.transact(code,data,reply,0);
                            CurTime=reply.readInt();
                            System.out.println(CurTime);
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                    Message msg = new Message();
                    msg.what = REFRESH;
                    handler.sendMessage(msg);
                }
            }

        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
