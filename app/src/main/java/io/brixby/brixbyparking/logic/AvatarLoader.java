package io.brixby.parking.logic;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.request.AttachmentGetRequest;
import io.brixby.parking.model.Attachment;
import rx.Observable;


public class AvatarLoader {

    private MppApi mppApi;
    private Context context;

    @Inject
    public AvatarLoader(MppApi mppApi, Context context) {
        this.mppApi = mppApi;
        this.context = context;
    }

    // returns file path
    public Observable<String> getAvatar(List<Attachment> attachments) {
        String fileId = getAvatarFileId(attachments);

        File savedFile = getSavedFile(fileId);
        if (savedFile != null) return Observable.just(savedFile.getAbsolutePath());

        return loadAvatar(fileId)
                .map(bytes -> saveToFile(fileId, bytes))
                .map(file -> file != null ? file.getAbsolutePath() : null)
                .onErrorReturn(e -> null);
    }

    private Observable<byte[]> loadAvatar(String fileId) {
        if (fileId == null) return Observable.just(null);
        return mppApi.callBytes(new AttachmentGetRequest(fileId)).onErrorReturn(e -> null);
    }

    @Nullable
    private String getAvatarFileId(List<Attachment> attachments) {
        if (attachments == null) return null;
        for (Attachment attachment : attachments) {
            if (attachment.getType().equalsIgnoreCase("avatar")) {
                return attachment.getAttachment();
            }
        }
        return null;
    }

    @Nullable
    private File getSavedFile(String fileId) {
        if (fileId == null) return null;
        File current = context.getFileStreamPath(fileId);
        return current.exists() ? current : null;
    }

    @Nullable
    private File saveToFile(String fileId, byte[] data) {
        if (fileId == null || data == null) return null;
        try {
            context.openFileOutput(fileId, Context.MODE_PRIVATE).write(data);
            return context.getFileStreamPath(fileId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
