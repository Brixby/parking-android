package io.brixby.parking.logic;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Network;
import io.brixby.parking.R;
import io.brixby.parking.Utils;
import io.brixby.parking.api.Response;
import io.brixby.parking.model.CardRecurrent;
import io.brixby.parking.model.CardSubscription;
import io.brixby.parking.utils.DateUtils;

import static io.brixby.parking.utils.Logger.log;

@Singleton
public class RecurrentManager {

    private final PersonManager personManager;
    private final String error;

    @Inject
    public RecurrentManager(PersonManager personManager, Context context) {
        this.personManager = personManager;
        error = context.getString(R.string.error);
    }

    private String getPhone() {
        return personManager.getSavePhone();
    }

    public Response removeRecurrent(String recurrentId) {
        try {
            String feeUrl = Utils.CARD_BINDING_PAYMENT_BASE_URL
                    + "/deleteRecurrents/?subscriber=" + getPhone() + "&recurrentId=" + recurrentId;
            log(feeUrl);

            String response = Network.requestString(feeUrl, "POST", null, 60000);
            log(response);

            return Response.success("");
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (Exception e) {
            log(e.toString());
            return Response.error(error);
        }
    }

    public Response getRecurrentInfo(Context context) {
        try {
            String feeUrl = Utils.CARD_BINDING_RECURRENT_INFO + "?subscriber=" + getPhone();
            log(feeUrl);

            String response = Network.requestString(feeUrl, "POST", null, 60000);
            log(response);

            if (!response.trim().equals("") && response.charAt(0) == '[') {
                ArrayList<CardRecurrent> recurrents = new ArrayList<>();
                JSONArray recurrentArray = new JSONArray(response);
                for (int i = 0; i < recurrentArray.length(); i++) {
                    JSONObject jsRecurrent = recurrentArray.getJSONObject(i);
                    recurrents.add(CardRecurrent.initJSON(jsRecurrent));
                }
                CardRecurrent recurrent = recurrents.size() > 0 ? recurrents.get(0) : null;
                return Response.success(context.getString(R.string.success_operation)).setData(recurrent);
            } else {
                return Response.error(error);
            }
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            log(e.toString());
            return Response.error(error);
        } catch (Exception e) {
            log(e.toString());
            return Response.error(error);
        }
    }

    public Response payRecurrent(String cvc, String order, String amount, Context context) {
        try {
            JSONObject jsRequest = new JSONObject()
                    .put("subscriber", getPhone())
                    .put("cvc", cvc)
                    .put("mdOrder", order)
                    .put("amount", amount);
            String paymentUrl = Utils.CARD_BINDING_PAYMENT + "?service=parking.recurrent.parent";
            log(jsRequest.toString());
            log(paymentUrl);

            String response = Network.requestString(paymentUrl, "POST", jsRequest.toString(), 60000);
            log(response);

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
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            log(e.toString());
            return Response.error(error);
        } catch (Exception e) {
            log(e.toString());
            return Response.error(error);
        }
    }

    public Response getRecurrentPaymentCapabilities(String bindingId, String amount, long expirySecs, Context context) {
        try {
            JSONObject jsRequest = new JSONObject()
                    .put("subscriber", getPhone())
                    .put("bindingId", bindingId)
                    .put("amount", amount)
                    .put("recurrent", new JSONObject()
                            .put("mnemonic", "my-recurrent")
                            .put("expiry", DateUtils.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"), expirySecs)));
            String feeUrl = Utils.CARD_BINDING_PAYMENT_CAPABILITIES + "?service=parking.recurrent.parent";
            log(jsRequest.toString());
            log(feeUrl);

            String response = Network.requestString(feeUrl, "POST", jsRequest.toString(), 60000);
            log(response);

            if (response.charAt(0) == '[') {
                ArrayList<CardSubscription> cards = new ArrayList<>();
                JSONArray cardArrays = new JSONArray(response);
                for (int i = 0; i < cardArrays.length(); i++) {
                    JSONObject jsCard = cardArrays.getJSONObject(i);
                    cards.add(CardSubscription.initJSON(jsCard));
                }
                return Response.success(context.getString(R.string.success_operation)).setData(cards);
            } else {
                JSONObject jsonObject = new JSONObject(response);
                String url = jsonObject.getString("url");
                return Response.success(context.getString(R.string.success_operation)).setData(url);
            }
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            log(e.toString());
            return Response.error(error);
        } catch (Exception e) {
            log(e.toString());
            return Response.error(error);
        }
    }
}
