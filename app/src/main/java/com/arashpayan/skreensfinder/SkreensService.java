package com.arashpayan.skreensfinder;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

public class SkreensService {

    public final String name;
    public final String[] addresses;
    public final int port;
    public final HashMap<String, String> attributes = new HashMap<>();

    protected SkreensService(ServiceEvent serviceEvent) {
        ServiceInfo si = serviceEvent.getInfo();
        this.name = si.getName();
        this.addresses = si.getHostAddresses();
        this.port = si.getPort();
        final Enumeration<String> propNames = si.getPropertyNames();
        if (propNames != null) {
            while (propNames.hasMoreElements()) {
                String propName = propNames.nextElement();
                attributes.put(propName, si.getPropertyString(propName));
            }
        }
    }

    public String getDeviceName() {
        String devName = attributes.get("name");
        if (devName != null) {
            return devName;
        }

        return name;
    }

    @Override
    public String toString() {
        return "SkreensService{" +
                "name='" + name + '\'' +
                ", addresses=" + Arrays.toString(addresses) +
                ", port=" + port +
                ", attributes=" + attributes +
                '}';
    }
}
