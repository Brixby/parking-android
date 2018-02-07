package io.brixby.parking.logic;

import javax.inject.Inject;

import io.brixby.parking.api.response.AccountCheckResponse;



public class KlcChecker {

    @Inject
    KlcChecker() {}

    public boolean isKlcClient(String phone, AccountCheckResponse response) {
        if (!response.getAccountType().equals("C")) return false;

        for (AccountCheckResponse.AccountPhone accountPhone : response.getPhoneList()) {
            if (phone.equals(accountPhone.getPhone())) {
                return accountPhone.getIsAccountAdmin().equals("0");
            }
        }
        return false;
    }

}
