package io.brixby.parking.api.response;

import java.util.List;

import io.brixby.parking.model.Fine;
import rx.Observable;


public class FinesResponse extends MppResponse {

    private List<HistoryRow> history;

    public List<Fine> getFines() {
        return Observable.from(history)
                .map(historyRow -> historyRow.historyRow)
                .toList().toBlocking().single();
    }

    private static class HistoryRow {
        Fine historyRow;
    }
}
