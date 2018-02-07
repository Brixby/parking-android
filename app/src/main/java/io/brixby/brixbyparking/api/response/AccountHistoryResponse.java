package io.brixby.parking.api.response;

import java.util.List;

import io.brixby.parking.model.History;
import rx.Observable;

public class AccountHistoryResponse extends MppResponse {

    private List<HistoryRow> history;

    public List<History> getHistory() {
        return Observable.from(history)
                .map(historyRow -> historyRow.historyRow)
                .toList().toBlocking().single();
    }

    private static class HistoryRow {
        History historyRow;
    }
}
