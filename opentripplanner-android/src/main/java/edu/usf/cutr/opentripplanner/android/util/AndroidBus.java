package edu.usf.cutr.opentripplanner.android.util;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * An Otto {@link Bus} that is restricted to delivering events on the main thread.
 */
public class AndroidBus extends Bus {

    private static final Bus INSTANCE = new AndroidBus();

    public static Bus getInstance() {
        return INSTANCE;
    }

    private final Handler mMainThread = new Handler(Looper.getMainLooper());

    public AndroidBus() {
        super(ThreadEnforcer.ANY);
    }

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AndroidBus.super.post(event);
        } else {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    post(event);
                }
            });
        }
    }

}