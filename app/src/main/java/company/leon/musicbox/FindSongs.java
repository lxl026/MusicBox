package company.leon.musicbox;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leon on 2017/11/28.
 */

public class FindSongs {

    public List<Mp3Info> getMp3Infos(ContentResolver contentResolver) {
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>();
        for (int i = 0; i < cursor.getCount(); i++) {
            Mp3Info mp3Info = new Mp3Info();                               //新建一个歌曲对象,将从cursor里读出的信息存放进去,直到取完cursor里面的内容为止.
            cursor.moveToNext();




            int isMusic = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));//是否为音乐

            long duration = cursor.getLong(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION));//时长


            if (isMusic != 0 && duration/(1000 * 60) >= 1) {     //只把1分钟以上的音乐添加到集合当中
                long id = cursor.getLong(cursor
                        .getColumnIndex(MediaStore.Audio.Media._ID));   //音乐id

                String title = cursor.getString((cursor
                        .getColumnIndex(MediaStore.Audio.Media.TITLE)));//音乐标题

                String artist = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Audio.Media.ARTIST));//艺术家





                long size = cursor.getLong(cursor
                        .getColumnIndex(MediaStore.Audio.Media.SIZE));  //文件大小

                String url = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Audio.Media.DATA));  //文件路径

                String album = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Audio.Media.ALBUM)); //唱片图片

                long album_id = cursor.getLong(cursor
                        .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)); //唱片图片ID
                mp3Info.setId(id);
                mp3Info.setTitle(title);
                mp3Info.setArtist(artist);
                mp3Info.setDuration(duration);
                mp3Info.setSize(size);
                mp3Info.setUrl(url);
                mp3Info.setAlbum(album);
                mp3Info.setAlbum_id(album_id);
                mp3Infos.add(mp3Info);
            }
        }
        cursor.close();
        return mp3Infos;
    }

}