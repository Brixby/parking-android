package io.brixby.parking.logic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.trello.rxlifecycle.android.ActivityEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.brixby.parking.R;
import io.brixby.parking.Utils;
import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.request.MppRequest;
import io.brixby.parking.api.request.NewsFileGetRequest;
import io.brixby.parking.api.request.NewsFileListRequest;
import io.brixby.parking.api.response.MppResponse;
import io.brixby.parking.api.response.NewsFileListResponse;
import io.brixby.parking.ui.screens.SettingsFragment;
import io.brixby.parking.utils.DateUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static io.brixby.parking.utils.Logger.log;


@Singleton
public class NewsManager {

    private final NewsPreferences newsPreferences;
    private final LanguageManager languageManager;
    private final MppApi mppApi;

    private WeakReference<Context> contextRef;

    @Inject
    public NewsManager(NewsPreferences newsPreferences, LanguageManager languageManager, MppApi mppApi) {
        this.newsPreferences = newsPreferences;
        this.languageManager = languageManager;
        this.mppApi = mppApi;
    }

    public void updateNews(RxAppCompatActivity context) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
        boolean showNews = prefs.getBoolean(SettingsFragment.SETTING_NEWS, true);
        if (!showNews) return;

        contextRef = new WeakReference<>(context);
        mppApi.call(new NewsFileListRequest(), NewsFileListResponse.class)
                .compose(context.bindUntilEvent(ActivityEvent.STOP))
                .filter(MppResponse::isOk)
                .flatMapIterable(NewsFileListResponse::getFiles)
                .filter(file -> "news.xml".equalsIgnoreCase(file.getName()))
                .filter(file -> file.getTime() != null && !file.getTime().equals(newsPreferences.getNewsDate()))
                .flatMap(file -> getFile(file.getId()).map(bytes -> processFileData(bytes, file.getTime())))
                .onErrorReturn(e -> null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processResponse);
    }

    private Observable<byte[]> getFile(String fileId) {
        MppRequest request = new NewsFileGetRequest(fileId);
        return mppApi.callBytes(request).onErrorReturn(e -> new byte[0]);
    }

    private String processFileData(byte[] data, String time) {
        try {
            String responseString = new String(data);
            InputStream is = new ByteArrayInputStream(data);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document dom = builder.parse(is);
            Element response = dom.getDocumentElement();

            int status = Integer.parseInt(response.getAttribute("status"));
            if (status == 1) {
                newsPreferences.saveNews(responseString, time);
                return responseString;
            } else {
                return null;
            }
        } catch (Exception e) {
            log("NewsManager get file error", e);
            return null;
        }
    }

    private void processResponse(String result) {
        if (result != null) {
            processNews(result);
        } else if (newsPreferences.getNews() != null) {
            processNews(newsPreferences.getNews());
        }
    }

    private void processNews(String responseString) {
        try {
            InputStream is = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document dom = builder.parse(is);
            Element response = dom.getDocumentElement();
            NodeList newsList = response.getElementsByTagName("news");

            Context context = contextRef.get();
            if (context == null) return;
            boolean isRu = languageManager.isRu(context);

            for (int k = 0; k < newsList.getLength(); k++) {
                Element news = (Element) newsList.item(k);
                String id = news.getAttribute("id");

                List<String> readNews = newsPreferences.getReadNews();

                if (!readNews.contains(id)) {
                    NodeList linkList = news.getElementsByTagName("link");
                    String link = linkList.getLength() > 0 ? linkList.item(0).getTextContent() : null;

                    Element showDate = (Element) news.getElementsByTagName("show_date").item(0);
                    String fromString = showDate.getElementsByTagName("from").item(0).getTextContent();
                    String toString = showDate.getElementsByTagName("to").item(0).getTextContent();

                    DateTimeFormatter formatter = getDateFormatter();
                    long from = ZonedDateTime.parse(fromString, formatter).toEpochSecond();
                    long to = ZonedDateTime.parse(toString, formatter).toEpochSecond();
                    long currentTime = ZonedDateTime.now().toEpochSecond();
                    if (from < currentTime && to > currentTime) {
                        String message = news.getElementsByTagName(isRu ? "text_ru" : "text_en").item(0).getTextContent();
                        String title = news.getElementsByTagName(isRu ? "title_ru" : "title_en").item(0).getTextContent();
                        showNews(context, title, message, link);
                        newsPreferences.addReadNews(id);
                    }
                }
            }
        } catch (Exception e) {
            log("NewsManager processNews error", e);
        }
    }

    private void showNews(Context context, String title, String message, String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(message)
                .setTitle(title)
                .setNeutralButton(R.string.dialog_ok, null);

        if (!TextUtils.isEmpty(link)) {
            builder.setPositiveButton(R.string.dialog_read_more, (d, i) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                }
            });
        }
        builder.show();
    }

    private DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(DateUtils.ZONE);
    }
}
