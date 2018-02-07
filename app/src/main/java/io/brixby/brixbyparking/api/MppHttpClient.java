package io.brixby.parking.api;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

import java.io.IOException;

import javax.inject.Inject;

import io.brixby.parking.api.request.IRequest;


public class MppHttpClient {

    private final OkHttpClient client;

    @Inject
    public MppHttpClient(OkHttpClient client) {
        this.client = client;
    }

    public ResponseBody execute(IRequest request) throws IOException {
        return client.newCall(request.getRequest()).execute().body();
    }
}
