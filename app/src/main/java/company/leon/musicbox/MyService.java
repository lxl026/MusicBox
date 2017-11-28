package company.leon.musicbox;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class MyService extends Service {
    public static final int  START =1, PAUSE =2, CONTINUE=3, STOP =4, PULL=5, REFRESH=6, CHANGE=7;
    public MyService() {

    }
    private MediaPlayer mediaPlayer ;
    private IBinder myBinder;
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("onBind","onBindExe************");
        return myBinder;
    }

    public class MyBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code){
                case START:
                    initMediaPlayer(data.readString());
                    Log.d("mediaPlayer","start**************************88");
                    break;
                case PAUSE:
                    mediaPlayer.pause();
                    Log.d("mediaPlayer","pause**************************88");
                    break;
                case CONTINUE:
                    mediaPlayer.start();
                    Log.d("mediaPlayer","start**************************88");
                    break;
                case STOP:
                    mediaPlayer.stop();
                    Log.d("mediaPlayer","start**************************88");
                    //mediaPlayer.release();
                    break;
                case PULL:
                    long tmp = data.readLong();
                    int i = (int) tmp;
                    mediaPlayer.seekTo(i);
                    break;
                case REFRESH:
                    reply.writeInt(mediaPlayer.getCurrentPosition());
                    break;
                case CHANGE:
                    String newUrl = data.readString();
                    try{
                        //获取Download文件夹下的音乐文件
                        File file = new File(newUrl);
                        if(file.exists()){
                            mediaPlayer.reset();

                            mediaPlayer.setDataSource(file.getPath());
                            Log.d("mediaPlayer","setPath**************************88");
                            mediaPlayer.prepare();
                            Log.d("mediaPlayer","setLoop**************************88");
                            mediaPlayer.start();
                            mediaPlayer.setLooping(true);
                            reply.writeInt(1);
                        }else {
                            Toast.makeText(MyService.this,"找不到文件",Toast.LENGTH_SHORT).show();
                            reply.writeInt(0);
                            stopSelf();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                default:
                    Log.d("mediaPlayer","TESTTEST**************************88");
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }

    }

    @Override
    public void onCreate() {
        Log.d("Service","onCreate**************************************88");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service","onStartCommand**************************************88");
        myBinder = new MyBinder();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service","onDestroy**************************************88");
        if(mediaPlayer!=null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                Log.d("mediaPlayer","stop**************************88");
            }
            mediaPlayer.reset();
            Log.d("mediaPlayer","reset**************************88");
            mediaPlayer.release();
            Log.d("mediaPlayer","release**************************88");
            mediaPlayer = null;
            Log.d("mediaPlayer","null**************************88");
        }

    }

    private void initMediaPlayer(String filePath) {
        try{
            mediaPlayer = new MediaPlayer();
            Log.d("mediaPlayer","new**************************88");
            //获取Download文件夹下的音乐文件
            File file = new File(filePath);
            if(file.exists()){
                mediaPlayer.setDataSource(file.getPath());
                Log.d("mediaPlayer","setPath**************************88");
                mediaPlayer.prepare();
                Log.d("mediaPlayer","prepare**************************88");
                mediaPlayer.setLooping(true);
                Log.d("mediaPlayer","setLoop**************************88");

                mediaPlayer.start();
            }else {
                Toast.makeText(this,"找不到文件",Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
