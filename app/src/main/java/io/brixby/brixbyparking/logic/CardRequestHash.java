package io.brixby.parking.logic;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class CardRequestHash {

    @Inject
    CardRequestHash() {}

    public String getParamsHash(String request) {
        String endTag = "&###";
        String endTagOnly = "###";
        String urlSplitter = "&";
        String params = split(request, "?", 1) + endTag;
        int i = 0;
        HashMap<String, String> paramsMap;
        HashMap<String, String> paramsNameValueMap = new HashMap<>();
        String param;
        Map<String, HashMap<String, String>> paramsTotal = new HashMap<>();
        while (!(param = split(params, urlSplitter, i)).equals(endTagOnly)) {
            paramsMap = new HashMap<>();
            paramsMap.put("name", split(param, "=", 0));
            paramsNameValueMap.put(split(param, "=", 0), split(param, "=", 1));
            paramsTotal.put(split(param, "=", 0), paramsMap);
            i++;
        }
        List<HashMap<String, String>> paramsList = new ArrayList<>(paramsTotal.values());
        Collections.sort(paramsList, (o1, o2) -> o1.get("name").compareTo(o2.get("name")));
        StringBuilder builderStr = new StringBuilder();
        int j = 0;
        for (HashMap<String, String> paramMap : paramsList) {
            builderStr.append(paramMap.get("name")).append("=").append(paramsNameValueMap.get(paramMap.get("name")))
                    .append(paramsList.size() - 1 == j ? "" : urlSplitter);
            j++;
        }
        return sha1(builderStr.toString());
    }

    public String split(String str, String delim, int valindx) {
        int i = 0;
        int itemindx = 0;
        String strOut;
        while (str.indexOf(delim, i) > -1 || i <= str.length()) {
            if (str.indexOf(delim, i) > -1) {
                strOut = str.substring(i, str.indexOf(delim, i));
            } else {
                strOut = str.substring(i);
            }
            if (itemindx == valindx) {
                return strOut;
            }
            i = str.indexOf(delim, i) + delim.length();
            itemindx = itemindx + 1;
        }
        return "";
    }

    private String sha1(String request) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(request.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            // it can't be
        }
        return sha1;
    }

    private String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
