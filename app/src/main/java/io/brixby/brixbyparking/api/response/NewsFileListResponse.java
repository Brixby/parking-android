package io.brixby.parking.api.response;

import java.util.List;

import rx.Observable;

public class NewsFileListResponse extends MppResponse {

    private List<FileRow> files;

    public List<File> getFiles() {
        return Observable.from(files)
                .map(fileRow -> fileRow.file)
                .toList().toBlocking().single();
    }

    private class FileRow {
        File file;
    }

    public static class File {

        private String id, name, time;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }
    }
}
