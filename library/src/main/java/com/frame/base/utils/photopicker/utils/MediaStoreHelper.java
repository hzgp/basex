package com.frame.base.utils.photopicker.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentActivity;


import com.frame.base.R;
import com.frame.base.utils.photopicker.entity.Photo;
import com.frame.base.utils.photopicker.entity.PhotoDirectory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.SIZE;

/**
 * Created by donglua on 15/5/31.
 */
public class MediaStoreHelper {

    public final static int INDEX_ALL_PHOTOS = 0;

    public static class FetchMediaThread extends Thread {
        WeakReference<Context> contextWeakReference;
        PhotosResultCallback resultCallback;
        int type;
        boolean showGif;

        public FetchMediaThread(Context context, boolean showGif, int type, PhotosResultCallback resultCallback) {
            this.contextWeakReference = new WeakReference<>(context);
            this.resultCallback = resultCallback;
            this.type = type;
            this.showGif = showGif;
        }

        @Override
        public void run() {
            if (contextWeakReference.get() == null)
                return;
            ContentResolver contentResolver = contextWeakReference.get().getContentResolver();
            ArrayList<Cursor> cursorArrayList = new ArrayList<>();
            Cursor videoCursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    , new String[]{MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.BUCKET_ID,
                            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Video.Media.DATE_ADDED,
                            MediaStore.Video.Media.SIZE,
                            MediaStore.Video.Media.WIDTH,
                            MediaStore.Video.Media.HEIGHT,
                            MediaStore.Video.Media.DURATION,
                            MediaStore.Video.Media.MIME_TYPE}
                    , MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? "
                    , new String[]{"video/mpeg", "video/mp4", "video/3gpp", "video/avi"}
                    , MediaStore.Images.Media.DATE_ADDED + " DESC");
            if ((PhotoPickerConstants.TYPE_VIDEO & type) == PhotoPickerConstants.TYPE_VIDEO) {
                cursorArrayList.add(videoCursor);
            }

            String[] selectionArgs = showGif ? new String[]{"image/jpeg", "image/png", "image/jpg", "image/gif"} : new String[]{"image/jpeg", "image/png", "image/jpg"};
            Cursor imageCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    , new String[]{MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media.BUCKET_ID,
                            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Images.Media.DATE_ADDED,
                            MediaStore.Images.Media.SIZE,
                            MediaStore.Images.Media.WIDTH,
                            MediaStore.Images.Media.HEIGHT,
                            MediaStore.Images.Media.MIME_TYPE}
                    , MIME_TYPE + "=? or " + MIME_TYPE + "=? or " + MIME_TYPE + "=? "
                    , selectionArgs
                    , MediaStore.Images.Media.DATE_ADDED + " DESC");
            if ((PhotoPickerConstants.TYPE_IMAGE & type) == PhotoPickerConstants.TYPE_IMAGE) {
                cursorArrayList.add(imageCursor);
            }
            Cursor[] cursors = cursorArrayList.toArray(new Cursor[0]);
            MergeCursor data = new MergeCursor(cursors);
            List<PhotoDirectory> directories = new ArrayList<>();
            if (contextWeakReference.get() == null)
                return;
            PhotoDirectory photoDirectoryAll = new PhotoDirectory();
            photoDirectoryAll.setName(contextWeakReference.get().getString(R.string.picker_all_image));
            photoDirectoryAll.setId(PhotoPickerConstants.TYPE_IMAGE);

            PhotoDirectory videoDirectoryAll = new PhotoDirectory();
            videoDirectoryAll.setName(contextWeakReference.get().getString(R.string.picker_all_video));
            videoDirectoryAll.setId(PhotoPickerConstants.TYPE_VIDEO);
            while (data.moveToNext()) {

                int imageId = data.getInt(data.getColumnIndexOrThrow(_ID));
                int bucketId = data.getInt(data.getColumnIndexOrThrow(BUCKET_ID));
                String name = data.getString(data.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
                String path = data.getString(data.getColumnIndexOrThrow(DATA));
                long size = data.getLong(data.getColumnIndexOrThrow(SIZE));
                String mimeType = data.getString(data.getColumnIndexOrThrow(MIME_TYPE));
                //  int width = data.getInt(data.getColumnIndexOrThrow(WIDTH));
                //    int height = data.getInt(data.getColumnIndexOrThrow(HEIGHT));
                long addDate = data.getInt(data.getColumnIndexOrThrow(DATE_ADDED));
                if (size < 1) continue;
                Photo photo = new Photo(imageId, path);
                photo.setAdddate(addDate);
                photo.setMimetype(mimeType);
                if (mimeType.contains("video")) {
                    long duration = data.getLong(data.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION));
                    photo.setDuration(duration);
                    videoDirectoryAll.addPhoto(photo);
                }

                PhotoDirectory photoDirectory = null;
                for (PhotoDirectory dir :
                        directories) {
                    if (dir.getId() == bucketId) {
                        photoDirectory = dir;
                        break;
                    }
                }
                if (photoDirectory == null) {
                    photoDirectory = new PhotoDirectory();
                    photoDirectory.setId(bucketId);
                    photoDirectory.setName(name);
                    photoDirectory.setCoverPath(path);
                    photoDirectory.setDateAdded(data.getLong(data.getColumnIndexOrThrow(DATE_ADDED)));
                    directories.add(photoDirectory);
                }
                photoDirectory.addPhoto(photo);
                photoDirectoryAll.addPhoto(photo);
            }
            data.close();
            Collections.sort(photoDirectoryAll.getPhotos(), (lhs, rhs) -> {
                return lhs.getAdddate() >= rhs.getAdddate() ? -1 : 1;//按照添加时间进行降序排序
            });
            if (photoDirectoryAll.getPhotoPaths().size() > 0) {
                photoDirectoryAll.setCoverPath(photoDirectoryAll.getPhotoPaths().get(0));
            }
            if ((PhotoPickerConstants.TYPE_IMAGE & type) == PhotoPickerConstants.TYPE_IMAGE)
                directories.add(INDEX_ALL_PHOTOS, photoDirectoryAll);
            if (!videoDirectoryAll.getPhotos().isEmpty() && (PhotoPickerConstants.TYPE_VIDEO & type) == PhotoPickerConstants.TYPE_VIDEO) {
                videoDirectoryAll.setCoverPath(videoDirectoryAll.getPhotoPaths().get(0));
                directories.add(INDEX_ALL_PHOTOS + 1, videoDirectoryAll);
            }
            if (resultCallback != null) {
                resultCallback.onResultCallback(directories);
            }
        }
    }


    public static void getPhotoDirs(final FragmentActivity activity, boolean showGif, int type, final PhotosResultCallback resultCallback) {
        new FetchMediaThread(activity, showGif, type, resultCallback).start();
    }


    public interface PhotosResultCallback {
        void onResultCallback(List<PhotoDirectory> directories);
    }

}
