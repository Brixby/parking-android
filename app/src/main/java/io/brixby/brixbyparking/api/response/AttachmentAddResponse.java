package io.brixby.parking.api.response;

import java.util.List;

import io.brixby.parking.model.Attachment;


public class AttachmentAddResponse extends MppResponse {

    private List<Attachment> attachments;

    public List<Attachment> getAttachments() {
        return attachments;
    }
}
