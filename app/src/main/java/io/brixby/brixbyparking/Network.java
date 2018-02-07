package io.brixby.parking.parking;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import io.brixby.parking.utils.Logger;


@Deprecated
public class Network {

    public static String requestString(String requestUrl, String method, String body) throws IOException {
        return requestString(requestUrl, method, body, 10000);
    }

    public static String requestString(String requestUrl, String method, String body, int timeout) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(timeout /* milliseconds */);
            conn.setConnectTimeout(30000 /* milliseconds */);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod(method);
            conn.setUseCaches(false);
            conn.setDoOutput(body != null);
            conn.setDoInput(true);
            conn.connect();
            if (body != null) {
                OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                osw.write(body);
                osw.flush();
                osw.close();
            }
            int response = conn.getResponseCode();

            if (response == HttpURLConnection.HTTP_OK)
                is = conn.getInputStream();
            else if (response == HttpURLConnection.HTTP_NOT_FOUND) {
                is = conn.getErrorStream();
                Logger.log("CODE " + response);
            } else {
                Logger.log("CODE " + response);
            }
            if (is != null) {
                Reader reader = new InputStreamReader(is, "UTF-8");
                StringBuilder data = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    data.append((char) c);
                }
                return data.toString();
            } else {
                return "";
            }

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static String google(String request) throws IOException {
        URL url = new URL(request);
        URLConnection conn = url.openConnection();
        Reader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
        StringBuilder data = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            data.append((char) c);
        }
        return data.toString();
    }

    public static InputStream requestStream(String requestUrl, String method, String body) throws IOException {
        InputStream is = null;
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestMethod(method);
        conn.setUseCaches(false);
        conn.setDoOutput(body != null);
        conn.setDoInput(true);
        conn.connect();
        if (body != null) {
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            osw.write(body);
            osw.flush();
            osw.close();
        }
        int response = conn.getResponseCode();
        if (response == 200)
            is = conn.getInputStream();
        else
            is = conn.getErrorStream();
        return is;
    }


}

