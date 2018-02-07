package io.brixby.parking.logic;

import android.content.Context;

import com.github.pengrad.json.XML;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Network;
import io.brixby.parking.R;
import io.brixby.parking.Utils;
import io.brixby.parking.api.Response;
import io.brixby.parking.model.CardSubscription;

import static io.brixby.parking.utils.Logger.log;


@Singleton
public class CardManager {

    private final CardRequestHash cardRequestHash;
    private final PersonManager personManager;
    private final String error;

    @Inject
    public CardManager(CardRequestHash cardRequestHash, PersonManager personManager, Context context) {
        this.cardRequestHash = cardRequestHash;
        this.personManager = personManager;
        error = context.getString(R.string.error);
    }

    private String getPhone() {
        return personManager.getSavePhone();
    }

    public Response checkCardUser() {
        try {
            log("checkCardUser " + getPhone());
            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=checkUser&partner=" + Utils.PARTNER_PARAM
                    + "&time=" + time
                    + "&subscriber=" + getPhone()
                    + "&secret=" + Utils.SECRET_PARAM;
            String requestHash = cardRequestHash.getParamsHash(request);
            String url = Utils.CARD_BINDING_BASE_URL
                    + "?action=checkUser&partner=" + Utils.PARTNER_PARAM
                    + "&hash=" + requestHash
                    + "&time=" + time
                    + "&subscriber=" + getPhone();
            log("request " + request);
            log("requestHash " + requestHash);
            log("url " + url);
            String response = Network.requestString(url, "GET", null);
            log("response " + response);
            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0")) {
                return Response.error(error);
            } else {
                JSONObject userJsonObj = xmlJSONObj.getJSONObject("user");
                boolean userExist = userJsonObj.optBoolean("exists", false);
                return Response.success("").setData(userExist);
            }
        } catch (IOException e) {
            log(e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response getCardBindings(boolean onlyActivated) {
        ArrayList<CardSubscription> cards = new ArrayList<>();
        try {
            log("getBindings " + getPhone());
            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=getBindings&partner=" + Utils.PARTNER_PARAM
                    + "&time=" + time
                    + "&subscriber=" + getPhone()
                    + "&secret=" + Utils.SECRET_PARAM;

            String requestHash = cardRequestHash.getParamsHash(request);
            log("request " + request);
            log("requestHash " + requestHash);
            String response = Network.requestString(Utils.CARD_BINDING_BASE_URL
                            + "?action=getBindings&partner=" + Utils.PARTNER_PARAM
                            + "&time=" + time
                            + "&subscriber=" + getPhone()
                            + "&hash=" + requestHash,
                    "GET", null);
            log("response " + response);
            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0"))
                return Response.error(error).setData(null);
            JSONObject list = xmlJSONObj.optJSONObject("bindingList");
            if (list != null) {
                JSONArray cardArrays = list.optJSONArray("binding");
                if (cardArrays != null) {
                    for (int i = 0; i < cardArrays.length(); i++) {
                        JSONObject jsCard = cardArrays.getJSONObject(i);
                        if (!onlyActivated && (jsCard.getString("bindingStatus").equals("0") || jsCard.getString("bindingStatus").equals("2"))) {
                            cards.add(CardSubscription.initJSON(jsCard));
                        } else if (onlyActivated && jsCard.getString("bindingStatus").equals("0")) {
                            cards.add(CardSubscription.initJSON(jsCard));
                        }
                    }
                } else {
                    JSONObject jsList = xmlJSONObj.optJSONObject("bindingList");
                    JSONObject jsCard = jsList.optJSONObject("binding");
                    if (jsCard != null) {
                        if (!onlyActivated && (jsCard.getString("bindingStatus").equals("0") || jsCard.getString("bindingStatus").equals("2"))) {
                            cards.add(CardSubscription.initJSON(jsCard));
                        } else if (onlyActivated && jsCard.getString("bindingStatus").equals("0")) {
                            cards.add(CardSubscription.initJSON(jsCard));
                        }
                    }
                }
            }
            return Response.success("").setData(cards);
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response deleteCardBindings(String bindingId, Context context) {
        try {
            log("getBindings " + bindingId);
            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=deleteBinding&partner=" + Utils.PARTNER_PARAM
                    + "&subscriber=" + getPhone()
                    + "&bindingId=" + bindingId
                    + "&time=" + time
                    + "&secret=" + Utils.SECRET_PARAM;

            String requestHash = cardRequestHash.getParamsHash(request);
            String response = Network.requestString(Utils.CARD_BINDING_BASE_URL
                            + "?action=deleteBinding&partner=" + Utils.PARTNER_PARAM
                            + "&subscriber=" + getPhone() + "&bindingId=" + bindingId + "&time=" + time
                            + "&hash=" + requestHash
                    , "GET", null);
            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0"))
                return Response.error(error);
            return Response.success(context.getString(R.string.success_operation));
        } catch (IOException e) {
            log(e.toString());
        } catch (JSONException e) {
            log(e.toString());
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response userRegistrationForCard(String firstName, String lastName, String email, Context context) {
        try {
            log("userRegistrationForCard " + getPhone());
            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=createUser&partner=" + Utils.PARTNER_PARAM
                    + "&time=" + time
                    + "&secret=" + Utils.SECRET_PARAM
                    + "&subscriber=" + getPhone()
                    + "&email=" + email
                    + "&firstName=" + firstName
                    + "&lastName=" + lastName;
            String requestHash = cardRequestHash.getParamsHash(request);
            String response = Network.requestString(Utils.CARD_BINDING_BASE_URL
                            + "?action=createUser&partner=" + Utils.PARTNER_PARAM
                            + "&time=" + time
                            + "&hash=" + requestHash
                            + "&subscriber=" + getPhone()
                            + "&email=" + URLEncoder.encode(email, "UTF-8")
                            + "&firstName=" + URLEncoder.encode(firstName, "UTF-8")
                            + "&lastName=" + URLEncoder.encode(lastName, "UTF-8"),
                    "GET", null);

            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0")) {
                return Response.error(error);
            }
            return Response.success(context.getString(R.string.success_operation));
        } catch (IOException e) {
            log(e.toString());
        } catch (JSONException e) {
            log(e.toString());
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response addCardBinding(String card, String mm, String yy, String cvc, String cardHolder, String mnemonic, Context context) {
        try {
            log("createVerifyPayment " + getPhone());
            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=createVerifyPayment&partner=" + Utils.PARTNER_PARAM
                    + "&subscriber=" + getPhone()
                    + "&mnemonic=" + mnemonic
                    + "&cardPan=" + card
                    + "&time=" + time
                    + "&secret=" + Utils.SECRET_PARAM
                    + "&cardExpiry=20" + yy + "-" + mm
                    + "&cvc=" + cvc
                    + "&cardHolderName=" + cardHolder;
            String requestHash = cardRequestHash.getParamsHash(request);

            String response = Network.requestString(Utils.CARD_BINDING_BASE_URL, "POST",
                    "action=createVerifyPayment&partner=" + Utils.PARTNER_PARAM
                            + "&subscriber=" + getPhone()
                            + "&mnemonic=" + URLEncoder.encode(mnemonic, "UTF-8")
                            + "&cardPan=" + card
                            + "&time=" + time
                            + "&hash=" + requestHash
                            + "&cardExpiry=20" + yy + "-" + mm
                            + "&cvc=" + cvc
                            + "&cardHolderName=" + URLEncoder.encode(cardHolder, "UTF-8"));
            log("addCirdBinding: " + response);
            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0")) {
                return Response.error(error);
            }
            if (xmlJSONObj.has("externalConfirmation")) {
                return Response.success(context.getString(R.string.success_operation)).setData((xmlJSONObj.getJSONObject("externalConfirmation")).optString("acsUrl", ""));
            } else {
                return Response.success(context.getString(R.string.success_operation));
            }

        } catch (IOException e) {
            log(e.toString());
        } catch (JSONException e) {
            log(e.toString());
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response completeCardBinding(String bindingId, String merchantName, String amount, Context context) {
        try {
            log("completeVerifyPayment " + getPhone());

            String time = Long.toString((new Date()).getTime() * 1000000);
            String request = Utils.CARD_BINDING_BASE_URL
                    + "?action=completeVerifyPayment&partner=" + Utils.PARTNER_PARAM
                    + "&subscriber=" + getPhone()
                    + "&bindingId=" + bindingId
                    + "&secret=" + Utils.SECRET_PARAM
                    + "&time=" + time
                    + "&merchantName=" + URLEncoder.encode(merchantName, "UTF-8")
                    + "&amount=" + amount;
            String requestHash = cardRequestHash.getParamsHash(request);
            String response = Network.requestString(Utils.CARD_BINDING_BASE_URL
                    + "?action=completeVerifyPayment&partner=" + Utils.PARTNER_PARAM
                    + "&subscriber=" + getPhone()
                    + "&bindingId=" + bindingId
                    + "&time=" + time
                    + "&hash=" + requestHash
                    + "&merchantName=" + URLEncoder.encode(merchantName, "UTF-8")
                    + "&amount=" + amount, "GET", null);
            JSONObject xmlJSONObj = xmlToJson(response).getJSONObject("response");
            String responseResult = xmlJSONObj.optString("errors", "-1");
            if (!responseResult.equals("0")) {
                return Response.error(error);
            }
            return Response.success(context.getString(R.string.success_operation)).setData("");
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response getPaymentCapabilities(String bindingId, String amount, String carNo, String serviceCode,
                                           boolean isSubscriptionPayment, boolean isFinePay, String paymentId, Context context) {
        ArrayList<CardSubscription> cards = new ArrayList<>();
        try {
            JSONObject jsRequest = new JSONObject()
                    .put("subscriber", getPhone())
                    .put("bindingId", bindingId)
                    .put("amount", amount);
            String feeUrl = Utils.CARD_BINDING_PAYMENT_CAPABILITIES;
            if (isSubscriptionPayment)
                feeUrl = feeUrl + "?service=binding.parking.subscription&code=" + serviceCode + "&option=" + carNo;
            else if (isFinePay) {
                jsRequest.put("SupplierBillID", paymentId);
                feeUrl = feeUrl + "?service=rbc-fine";
            } else
                feeUrl = feeUrl + "?service=binding.payment.parking";
            log(jsRequest.toString());
            log(feeUrl);
            String response = Network.requestString(feeUrl, "POST", jsRequest.toString(), 60000);
            JSONArray cardArrays = new JSONArray(response);
            for (int i = 0; i < cardArrays.length(); i++) {
                JSONObject jsCard = cardArrays.getJSONObject(i);
                cards.add(CardSubscription.initJSON(jsCard));
            }
            return Response.success(context.getString(R.string.success_operation)).setData(cards);
        } catch (IOException e) {
            log(e.toString());
            return Response.error(error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public Response payWithMyCard(String amount, String cvc, String order, String carNo, String serviceCode,
                                  boolean subscriptionPayment, boolean isFinePay, String paymentId, Context context,
                                  int validYears) {
        try {
            JSONObject jsRequest = new JSONObject()
                    .put("subscriber", getPhone())
                    .put("cvc", cvc)
                    .put("mdOrder", order)
                    .put("amount", amount);
            String paymentUrl = Utils.CARD_BINDING_PAYMENT;
            if (subscriptionPayment) {
                paymentUrl = Utils.CARD_SUBSCRIPTION_BINDING_PAYMENT;
                paymentUrl = paymentUrl + "?service=binding.parking.subscription&code=" + serviceCode
                        + "&option=" + URLEncoder.encode(carNo, "UTF-8")
                        + "&ValidYears=" + validYears;
            } else if (isFinePay) {
                jsRequest.put("SupplierBillID", paymentId);
                paymentUrl = Utils.CARD_SUBSCRIPTION_BINDING_PAYMENT + "?service=rbc-fine";
            } else
                paymentUrl = paymentUrl + "?service=binding.payment.parking";
            log(jsRequest.toString());
            log(paymentUrl);

            String response = Network.requestString(paymentUrl, "POST", jsRequest.toString(), 60000);
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
            e.printStackTrace();
        }
        return Response.error(error);
    }

    public JSONObject xmlToJson(String xml) {
        try {
            return new JSONObject(XML.toJSONObject(xml).toString());
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
