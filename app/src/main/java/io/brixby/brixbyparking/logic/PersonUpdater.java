package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.brixby.parking.utils.Logger;


public class PersonUpdater {

    public static final String PREF_NAME = "person_update";
    private ScheduledFuture taskUpdatePerson;
    private ScheduledExecutorService worker;
    private PersonUpdaterImpl impl;

    public PersonUpdater(PersonManager personManager, Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long period = 1000 * 16;
        impl = new PersonUpdaterImpl(personManager, period, sp);
        worker = Executors.newSingleThreadScheduledExecutor();
        Logger.log("PersonUpdater init");
    }

    public void stop() {
        if (taskUpdatePerson != null) {
            taskUpdatePerson.cancel(true);
        }
    }

    public void start() {
        taskUpdatePerson = worker.scheduleWithFixedDelay(impl, 60, 15, TimeUnit.SECONDS);
    }

    public static class PersonUpdaterImpl implements Runnable {
        private PersonManager personManager;
        private long period;
        private SharedPreferences sp;
        private String prefName = "updatePerson";

        public PersonUpdaterImpl(PersonManager personManager, long period, SharedPreferences sp) {
            this.personManager = personManager;
            this.period = period;
            this.sp = sp;
        }

        public void run() {
            try {
                Date now = new Date();
                long diff = now.getTime() - sp.getLong(prefName, 0);
                Logger.log(prefName + " " + diff);
                if (diff > 0 && diff < period)
                    return;
                personManager.updatePerson().toBlocking().single();
                sp.edit().putLong(prefName, now.getTime()).apply();
            } catch (Exception e) {
                Logger.log(e.toString());
            }
        }
    }
}