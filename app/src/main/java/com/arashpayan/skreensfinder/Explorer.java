package com.arashpayan.skreensfinder;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class Explorer implements ServiceListener {

    private static volatile Explorer singleton;
    private static final String SERVICE_TYPE = "_skreen-controller._tcp.local.";
    public static boolean DEBUG_LOGGING = false;

    private Handler mMainHandler;
    // requires CHANGE_WIFI_MULTICAST_STATE permission
    private WifiManager.MulticastLock mMulticastLock;
    private JmDNS mJmdns;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private LinkedList<ServiceEvent> mResolvedServices = new LinkedList<>();
    private ExplorerListener mListener;

    /**
     * Call <code>getExplorer()</code> to use this class.
     */
    private Explorer() {
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static Explorer getExplorer() {
        if (singleton == null) {
            synchronized (Explorer.class) {
                if (singleton == null) {
                    singleton = new Explorer();
                }
            }
        }

        return singleton;
    }

    public ArrayList<SkreensService> getFoundServices() {
        ArrayList<SkreensService> services = new ArrayList<>(mResolvedServices.size());
        for (ServiceEvent se : mResolvedServices) {
            services.add(new SkreensService(se));
        }

        return services;
    }

    public void startExploring(final Context context) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                mMulticastLock = wifiMgr.createMulticastLock("Skreens Multicast Lock");
                mMulticastLock.setReferenceCounted(false);
                mMulticastLock.acquire();

                try {
                    mJmdns = JmDNS.create();
                } catch (IOException e) {
                    L.w("exception creating jmdns", e);
                    mMulticastLock.release();
                    mMulticastLock = null;
                    L.w("unable to start jmdns");
                    return;
                }

                mJmdns.addServiceListener(SERVICE_TYPE, Explorer.this);
            }
        });
    }

    public void stopExploring() {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (mJmdns == null) {
                    return;
                }

                mResolvedServices.clear();

                mJmdns.removeServiceListener(SERVICE_TYPE, Explorer.this);
                mJmdns = null;

                mMulticastLock.release();
                mMulticastLock = null;
            }
        });
    }

    // ServiceListener methods

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        if (DEBUG_LOGGING) {
            L.i("service added");
        }
        if (mJmdns != null) {
            if (DEBUG_LOGGING) {
                L.i("|    resolving " + serviceEvent.getName());
            }
            mJmdns.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        }
    }

    @Override
    public void serviceRemoved(final ServiceEvent removedService) {
        if (DEBUG_LOGGING) {
            L.i("service removed");
        }
        // find the existing record
        Iterator<ServiceEvent> iter = mResolvedServices.iterator();
        while (iter.hasNext()) {
            ServiceEvent knownService = iter.next();
            if (knownService.getName().equals(removedService.getName())) {
                iter.remove();
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) {
                            mListener.serviceRemoved(removedService.getName());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        if (DEBUG_LOGGING) {
            L.i("service resolved: " + serviceEvent.getName());
        }
        mResolvedServices.add(serviceEvent);
        final SkreensService ss = new SkreensService(serviceEvent);
        if (DEBUG_LOGGING) {
            L.i("found: " + ss);
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.serviceFound(ss);
                }
            }
        });
    }

    public Explorer setListener(ExplorerListener l) {
        mListener = l;

        return this;
    }

    interface ExplorerListener {
        void serviceFound(SkreensService ss);
        void serviceRemoved(String serviceName);
    }
}
