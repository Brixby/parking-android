package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Network;
import io.brixby.parking.R;
import io.brixby.parking.Utils;
import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.Response;
import io.brixby.parking.api.request.AccountCheckRequest;
import io.brixby.parking.api.request.AttachmentAddRequest;
import io.brixby.parking.api.request.FeedbackRequest;
import io.brixby.parking.api.request.FinesRequest;
import io.brixby.parking.api.request.MppRequest;
import io.brixby.parking.api.request.ParkingCheckRequest;
import io.brixby.parking.api.request.ParkingExtendRequest;
import io.brixby.parking.api.request.ParkingListRequest;
import io.brixby.parking.api.request.ParkingStartRequest;
import io.brixby.parking.api.request.ParkingStopRequest;
import io.brixby.parking.api.request.ParkingTicketRequest;
import io.brixby.parking.api.request.TimeDeltaRequest;
import io.brixby.parking.api.response.AccountCheckResponse;
import io.brixby.parking.api.response.AttachmentAddResponse;
import io.brixby.parking.api.response.FinesResponse;
import io.brixby.parking.api.response.ParkingCheckResponse;
import io.brixby.parking.api.response.ParkingListResponse;
import io.brixby.parking.api.response.ParkingStartResponse;
import io.brixby.parking.api.response.TimeDeltaResponse;
import io.brixby.parking.debug.DebugFines;
import io.brixby.parking.model.CardPaymentParams;
import io.brixby.parking.model.FeedbackHistory;
import io.brixby.parking.model.Fine;
import io.brixby.parking.model.Person;
import io.brixby.parking.push.PushManager;
import rx.Observable;

import static io.brixby.parking.utils.Logger.log;


@Singleton
public class PersonManager {

    private SharedPreferences preferences;
    private String phone;
    private String pin;
    private volatile boolean isLogin;

    private volatile Person person;
    private PersonUpdater personUpdater;

    private String error;

    private final PushManager pushManager;
    private final MppApi mppApi;
    private final AvatarLoader avatarLoader;
    private final RecurrentChecker recurrentChecker;
    private final KlcChecker klcChecker;
    private final GeofenceManager geofenceManager;
    private final ParkingInitLocationManager parkingInitLocationManager;

    private final Gson gson = new Gson();

    @Inject
    public PersonManager(Context context, PushManager pushManager, MppApi mppApi, AvatarLoader avatarLoader,
                         RecurrentChecker recurrentChecker, KlcChecker klcChecker, GeofenceManager geofenceManager,
                         ParkingInitLocationManager parkingInitLocationManager) {
        preferences = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
        phone = preferences.getString("phone", null);
        pin = preferences.getString("pin", null);
        isLogin = false;
        error = context.getString(R.string.error);
        person = new Person();

        this.pushManager = pushManager;
        this.mppApi = mppApi;
        this.avatarLoader = avatarLoader;
        this.recurrentChecker = recurrentChecker;
        this.klcChecker = klcChecker;
        this.geofenceManager = geofenceManager;
        this.parkingInitLocationManager = parkingInitLocationManager;

        mppApi.setLogin(phone, pin);
    }

    public PersonManager initBackground(Context context) {
        personUpdater = new PersonUpdater(this, context);
        if (phone != null && pin != null) {
            updatePerson().toBlocking().single();
            pushManagerLogin(context, phone);
        }
        return this;
    }

    public Observable<String> login(Context context, String phone, String pin, boolean save) {
        this.phone = phone;
        this.pin = pin;
        mppApi.setLogin(phone, pin);

        return updatePerson()
                .map(str -> {
                    pushManagerLogin(context, phone);
                    if (isLogin()) {
                        SharedPreferences.Editor editor = preferences.edit().putString("phone", phone);
                        if (save) editor.putString("pin", pin);
                        editor.apply();
                    }
                    return str;
                });
    }

    private Observable<AccountCheckResponse> accountCheck() {
        return mppApi.call(new AccountCheckRequest(), AccountCheckResponse.class);
    }

    public Observable<ParkingListResponse> parkingList() {
        return mppApi.call(new ParkingListRequest(), ParkingListResponse.class);
    }

    private Observable<TimeDeltaResponse> timeDelta() {
        return mppApi.call(new TimeDeltaRequest(), TimeDeltaResponse.class);
    }

    public Observable<String> updatePerson() {
        return Observable.zip(accountCheck(), timeDelta(), parkingList(),
                (accountCheckResponse, timeDeltaResponse, parkingListResponse) -> {
                    if (accountCheckResponse.isOk()) {
                        isLogin = true;
                        accountCheckResponse.fillPerson(person);
                        person.setRecurrent(recurrentChecker.isRecurrent(accountCheckResponse.getParking()));
                        person.setKLC(klcChecker.isKlcClient(phone, accountCheckResponse));
                        avatarLoader.getAvatar(accountCheckResponse.getAttachments()).subscribe(person::setAvatarUrl);
                        if (timeDeltaResponse.isOk()) {
                            person.setTimeDelta(timeDeltaResponse.getDelta());
                        }
                        if (parkingListResponse.isOk()) {
                            person.setParking(parkingListResponse.getSessionsList());
                        }
                        return null;
                    } else {
                        isLogin = false;
                        return accountCheckResponse.getErrorInfo();
                    }
                });
    }

    public Observable<String> uploadAvatar(File file) {
        return mppApi.call(new AttachmentAddRequest(file), AttachmentAddResponse.class)
                .flatMap(response -> response.isOk() ? avatarLoader.getAvatar(response.getAttachments()) : Observable.just(null));
    }

    @Nullable
    public String getParkingBySessionId(String sessionId) {
        MppRequest request = new ParkingCheckRequest(sessionId);
        ParkingCheckResponse response = mppApi.call(request, ParkingCheckResponse.class).toBlocking().single();
        return response.isOk() ? response.getStatus() : null;
    }

    public Observable<Response> parkStart(String zoneId, String carId, int mins, Location location) {
        return mppApi.call(new ParkingStartRequest(zoneId, carId, mins, location), ParkingStartResponse.class)
                .map(response -> {
                    if (response.isOk()) {
                        String sessionId = response.getSessionID();
                        String parkingStatus = response.getStatus();
                        if (parkingStatus.equalsIgnoreCase("updating")) {
                            log("start extra parking session updater");
                            new ParkingSessionUpdater(this, sessionId).start();
                        }
                        geofenceManager.onParkingStart(sessionId, mins, location);
                        parkingInitLocationManager.saveLocation(response, location);
                        return Response.success(sessionId);
                    } else {
                        return Response.error(response.getErrorInfo());
                    }
                })
                .flatMap(result -> result.isError() ? Observable.just(result) : updatePerson().map(s -> result));
    }

    public Observable<String> parkExtend(int mins, String parkingSessionId) {
        return mppApi.call(new ParkingExtendRequest(mins, parkingSessionId))
                .map(response -> !response.isOk() ? response.getErrorInfo() : null)
                .flatMap(result -> result != null ? Observable.just(result) : updatePerson());
    }

    public Observable<String> parkStop(String parkingSessionId) {
        return mppApi.call(new ParkingStopRequest(parkingSessionId))
                .map(response -> {
                    if (response.isOk()) {
                        geofenceManager.onParkingStop(parkingSessionId);
                        return null;
                    } else return response.getErrorInfo();
                })
                .flatMap(result -> result != null ? Observable.just(result) : updatePerson());
    }

    public Observable<Response> parkingTicketStart(Context context, String zone, String ticket) {
        return mppApi.call(new ParkingTicketRequest(zone, ticket), ParkingStartResponse.class)
                .map(response -> {
                    if (response.isOk()) {
                        return Response.success(response.getTotalCharge().toString());
                    } else {
                        String eCode = response.getErrorCode();
                        String eInfo = response.getErrorInfo();
                        if ("err_invalid_zone".equals(eCode) || "err_park_none".equals(eCode)) {
                            return Response.error(context.getString(R.string.parking_err_invalid_zone));
                        } else {
                            return Response.error(!TextUtils.isEmpty(eInfo) ? eInfo : context.getString(R.string.parking_no_money));
                        }
                    }
                })
                .flatMap(response -> response.isError() ? Observable.just(response) : updatePerson().map(s -> response));
    }

    public Response payCard(String card, int mm, int yy, int cvc, String cardHolder, float sum, Context context) {
        String orderId = "64327";
        String exp = "20" + yy + (mm < 10 ? "0" + mm : mm);
        try {
            JSONObject jsPayment = new JSONObject().put("type", "card")
                    .put("details", new JSONObject()
                            .put("subscriber", phone)
                            .put("pan_mko", card)
                            .put("cvc_mko", String.format("%03d", cvc))
                            .put("exp_mko", exp)
                            .put("cardholder_mko", cardHolder));
            JSONArray jsForms = new JSONArray()
                    .put(new JSONObject().put("id", "Order").put("value", orderId))
                    .put(new JSONObject().put("id", "Summ").put("value", String.format("%s", sum)));
            JSONObject jsRequest = new JSONObject().put("payment", jsPayment).put("forms", jsForms);
            String response = Network.requestString(Utils.PAY_CARD_URL, "POST", jsRequest.toString());
            log("payCard " + response);
            JSONObject js = new JSONObject(response);
            int result = js.optInt("result", -1);
            String url = js.optString("url");
            if (!url.equals("")) {
                return Response.success(context.getString(R.string.card_pay_success)).setData(url);
            } else if (result == 0) {
                return Response.success(context.getString(R.string.card_pay_success)).setData(null);
            } else {
                return Response.error(context.getString(R.string.card_pay_failed));
            }
        } catch (JSONException e) {
            log(e.getMessage(), e);
            return Response.error(context.getString(R.string.card_pay_request_error));
        } catch (IOException e) {
            log(e.getMessage(), e);
            return Response.error(context.getString(R.string.card_pay_request_error));
        }
    }

    public Response payCardSubscription(String card, int mm, int yy, int cvc,
                                        String cardHolder, float sum, Context context, String serviceCode,
                                        String carNo, int validYears) {
        String orderId = "64327";
        String exp = "20" + yy + (mm < 10 ? "0" + mm : mm);
        try {
            JSONObject jsPayment = new JSONObject()
                    .put("type", "card")
                    .put("details", new JSONObject()
                            .put("subscriber", phone)
                            .put("pan_mko", card)
                            .put("cvc_mko", String.format(Locale.getDefault(), "%03d", cvc))
                            .put("exp_mko", exp)
                            .put("cardholder_mko", cardHolder));
            JSONArray jsForms = new JSONArray()
                    .put(new JSONObject().put("id", "Order").put("value", orderId))
                    .put(new JSONObject().put("id", "Summ").put("value", String.format("%s", sum)));
            JSONObject jsRequest = new JSONObject().put("payment", jsPayment).put("forms", jsForms);

            String requestUrl = URLEncoder.encode(Utils.PAY_CARD_URL + "?service=card.parking.subscription&code="
                    + serviceCode + "&option=" + carNo + "&ValidYears=" + validYears, "UTF-8");

            String response = Network.requestString(requestUrl, "POST", jsRequest.toString());
            JSONObject js = new JSONObject(response);
            int result = js.optInt("result", -1);
            String url = js.optString("url");
            if (!url.equals("")) {
                return Response.success(context.getString(R.string.card_pay_success)).setData(url);
            } else if (result == 0) {
                return Response.success(context.getString(R.string.card_pay_success)).setData(null);
            } else {
                return Response.error(context.getString(R.string.card_pay_failed));
            }
        } catch (JSONException e) {
            log(e.getMessage(), e);
            return Response.error(context.getString(R.string.card_pay_request_error));
        } catch (IOException e) {
            log(e.getMessage(), e);
            return Response.error(context.getString(R.string.card_pay_request_error));
        }
    }

    public Response payCardFine(String card, int mm, int yy, int cvc, String cardHolder, float sum, String paymentId, Context context) {
        String exp = "20" + yy + (mm < 10 ? "0" + mm : mm);
        try {
            JSONObject jsPayment = new JSONObject().put("type", "card").put(
                    "details",
                    new JSONObject().put("subscriber", phone)
                            .put("pan_mko", card)
                            .put("cvc_mko", String.format("%03d", cvc))
                            .put("exp_mko", exp)
                            .put("cardholder_mko", cardHolder));
            JSONArray jsForms = new JSONArray().put(
                    new JSONObject()
                            .put("id", "Summ")
                            .put("value", String.format("%s", sum)));
            JSONObject jsRequest = new JSONObject()
                    .put("RNIPBillId", paymentId)
                    .put("payment", jsPayment)
                    .put("forms", jsForms);
            log(jsRequest.toString());
            String response = Network.requestString(Utils.PAY_CARD_FINE_URL, "POST", jsRequest.toString());
            log(response);
            JSONObject js = new JSONObject(response);
            if (js.has("error")) {
                StringBuilder error = new StringBuilder();
                Object o = js.get("error");
                if (o instanceof JSONObject) {
                    JSONObject jsError = (JSONObject) o;
                    Iterator it = jsError.keys();
                    while (it.hasNext()) {
                        String key = it.next().toString();
                        error.append(key).append(" : ").append(jsError.getString(key)).append("\n");
                    }
                } else {
                    error.append(o.toString());
                }
                return Response.error(error.toString());
            } else {
                String text;
                String url = js.optString("url");
                if (js.has("statusText")) {
                    text = js.getString("statusText");
                } else {
                    text = context.getString(R.string.card_pay_success);
                }
                return Response.success(text).setData(url.equals("") ? null : url);
            }
        } catch (JSONException e) {
            log(e.toString());
            return Response.error(error);
        } catch (IOException e) {
            log(e.toString());
            e.printStackTrace();
            return Response.error(error);
        }
    }

    public Response payMobile(String phone, int sum, Context context) {
        try {
            log("payMobile " + phone);
            Network.requestString(Utils.PAY_MOBILE_URL + "&subscriber=" + phone + "&message=pay" + sum, "GET", null);
            return Response.success(context.getString(R.string.mobile_pay_success));
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        }
    }

    public String activateScratchCard(String code) {
        try {
            return Network.requestString(Utils.API_URL, "POST",
                    "format=json&phoneNo=" + phone + "&PIN=" + pin + "&operation=valuecard_activate&data=" + code);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Response payBrowser() {
        try {
            String response = Network.requestString(Utils.API_URL, "POST",
                    "format=json&phoneNo=" + phone + "&PIN=" + pin + "&operation=payment_start&actingPartnerID=-&data=100,RNIP/-/EPSh,2&usageMethod=Web&partnerID=" + Utils.PARTNER_ID + "&backUrl=ru.mos.parking://process?result=");
            JSONObject js = new JSONObject(response);
            JSONObject responseJS = js.getJSONObject("response");
            int status = js.getInt("status");
            if (status == 1) {
                String serviceURL = responseJS.optString("pendingServiceURL", null);
                if (serviceURL != null)
                    return Response.success(serviceURL);
                else
                    return Response.error(responseJS.optString("info", null));
            } else {
                return Response.error(responseJS.optString("errorInfo", error));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.error(error);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.error(error);
        }
    }

    public void requestSupport(int categoryId, String question, HashMap<String, String> files) {
        try {
            mppApi.execute(new FeedbackRequest(categoryId, question, files));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CardPaymentParams getCardPayParams(String service) {
        try {
            String url = Utils.PAY_CARD_FEE_URL + "?service=" + service;
            log("getCardPayParams url: " + url);
            String response = Network.requestString(url, "GET", null);
            log("getCardPayParams: " + response);
            return gson.fromJson(response, CardPaymentParams.class);
        } catch (IOException e) {
            log(e.toString());
            return null;
        } catch (Exception e) {
            log(e.toString());
            return null;
        }
    }

    public Observable<List<Fine>> getFines() {
        return mppApi.call(new FinesRequest(), FinesResponse.class)
                .filter(FinesResponse::isOk)
                .flatMapIterable(FinesResponse::getFines)
                .concatWith(DebugFines.getTestFines())
                .toSortedList((fine, fine2) -> fine.getDatetime() > fine2.getDatetime() ? -1 : 1)
                .onErrorReturn(throwable -> new ArrayList<>(0));
    }

    public synchronized List<FeedbackHistory> getFeedbackHistory() {
        ArrayList<FeedbackHistory> historyItems = new ArrayList<>();
        try {
            StringBuilder request = new StringBuilder("format=json&phoneNo=")
                    .append(phone).append("&PIN=").append(pin)
                    .append("&operation=account_history&data=type:support");
            log(request.toString());
            String result = Network.requestString(Utils.API_URL, "POST", request.toString(), 60000);
            JSONObject js = new JSONObject(result);
            if (js.getInt("status") == 1) {
                if (js.getJSONObject("response").has("history")) {
                    JSONArray jsItems = js.getJSONObject("response").getJSONArray("history");
                    for (int i = 0; i < jsItems.length(); i++) {
                        JSONObject jsItem = jsItems.getJSONObject(i).getJSONObject("historyRow");
                        historyItems.add(FeedbackHistory.initJSON(jsItem));
                    }
                }
                updatePerson();
            } else {
                log(js.getJSONObject("response").getString("errorInfo"));
            }
            return historyItems;
        } catch (IOException e) {
            e.printStackTrace();
            return historyItems;
        } catch (JSONException e) {
            e.printStackTrace();
            return historyItems;
        }
    }

    public String setFinesNotificationFlag(int flag) {
        try {
            String response = Network.requestString(Utils.API_URL, "POST",
                    "format=json&phoneNo=" + phone + "&PIN=" + pin + "&operation=account_store&data=hasFinesSubscription:" + flag);
            log(response);
            JSONObject js = new JSONObject(response);
            int status = js.getInt("status");
            if (status == 1) {
                String val = js.getJSONObject("response").getString("hasFinesSubscription");
                val = val == null ? "0" : val;
                person.setNotifyNewFines(Integer.parseInt(val));
                return null;
            } else {
                return js.getJSONObject("response").getString("errorInfo");
            }
        } catch (JSONException e) {
            log(e.toString());
            e.printStackTrace();
            return error;
        } catch (IOException e) {
            e.printStackTrace();
            return error;
        }
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void logout(Context context) {
        preferences.edit().putString("pin", null).apply();
        pin = null;
        isLogin = false;
        pushManagerLogout(context, phone);
    }

    public Person getPerson() {
        return person;
    }

    public String getSavePhone() {
        return phone == null ? preferences.getString("phone", null) : phone;
    }

    public String getSavePin() {
        return preferences.getString("pin", null);
    }

    public void terminate() {
        personUpdater.stop();
    }

    private void pushManagerLogin(Context context, String phone) {
        if (isLogin())
            pushManager.registerLogin(context, phone);
    }

    private void pushManagerLogout(Context context, String phone) {
        pushManager.unregisterLogin(context, phone);
    }

    public void stopUpdatePerson() {
        personUpdater.stop();
    }

    public void startUpdatePerson() {
        personUpdater.start();
    }
}