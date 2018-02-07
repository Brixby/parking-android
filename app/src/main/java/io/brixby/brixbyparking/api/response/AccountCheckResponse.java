package io.brixby.parking.api.response;

import com.google.gson.JsonObject;

import java.util.List;

import io.brixby.parking.model.Attachment;
import io.brixby.parking.model.Car;
import io.brixby.parking.model.Document;
import io.brixby.parking.model.Permit;
import io.brixby.parking.model.Person;


public class AccountCheckResponse extends MppResponse {

    private String name, surname, accountType;
    private double availableFunds;
    private int hasFinesSubscription;
    private JsonObject parking;

    private List<Car> carsList;
    private List<Document> documentList;
    private List<Permit> permitsList;
    private List<AccountPhone> phoneList;
    private List<Attachment> attachments;

    public String getAccountType() {
        return accountType;
    }

    public List<AccountPhone> getPhoneList() {
        return phoneList;
    }

    public JsonObject getParking() {
        return parking;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void fillPerson(Person person) {
        person.setName(name);
        person.setSurname(surname);
        person.setAvailableFunds(availableFunds);
        person.setAccountType(accountType);
        person.setNotifyNewFines(hasFinesSubscription);
        person.setPermits(permitsList);
        person.setCarsList(carsList);
        person.setDocuments(documentList);
    }

    public static class AccountPhone {
        private String phone, isAccountAdmin;

        public String getPhone() {
            return phone;
        }

        public String getIsAccountAdmin() {
            return isAccountAdmin;
        }
    }
}
