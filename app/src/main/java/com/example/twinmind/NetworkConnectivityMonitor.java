package com.example.twinmind;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class NetworkConnectivityMonitor {

    private static final String TAG = "NetworkMonitor";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final Set<NetworkStateListener> listeners;
    private boolean isConnected = false;
    private NetworkCallback networkCallback;

    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    public NetworkConnectivityMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new HashSet<>();

        // Check initial connectivity state
        this.isConnected = isCurrentlyConnected();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setupNetworkCallback();
        }
    }

    private void setupNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new NetworkCallback();

            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }

    public void addListener(NetworkStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected;
    }

    private boolean isCurrentlyConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            // Fallback for older Android versions
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void notifyNetworkAvailable() {
        Log.d(TAG, "Network became available");
        isConnected = true;
        for (NetworkStateListener listener : listeners) {
            try {
                listener.onNetworkAvailable();
            } catch (Exception e) {
                Log.e(TAG, "Error notifying network available", e);
            }
        }
    }

    private void notifyNetworkLost() {
        Log.d(TAG, "Network lost");
        isConnected = false;
        for (NetworkStateListener listener : listeners) {
            try {
                listener.onNetworkLost();
            } catch (Exception e) {
                Log.e(TAG, "Error notifying network lost", e);
            }
        }
    }

    public void destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
        listeners.clear();
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);

            // Verify the network actually has internet connectivity
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {

                if (!isConnected) {
                    notifyNetworkAvailable();
                }
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);

            // Check if we still have other network connections
            if (!isCurrentlyConnected() && isConnected) {
                notifyNetworkLost();
            }
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);

            boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            if (hasInternet && !isConnected) {
                notifyNetworkAvailable();
            } else if (!hasInternet && isConnected) {
                // Check if we have other connections
                if (!isCurrentlyConnected()) {
                    notifyNetworkLost();
                }
            }
        }
    }

    // Utility methods for different connection types
    public boolean isWifiConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public boolean isCellularConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public String getConnectionType() {
        if (!isConnected) return "None";

        if (isWifiConnected()) return "WiFi";
        if (isCellularConnected()) return "Cellular";
        return "Unknown";
    }
}