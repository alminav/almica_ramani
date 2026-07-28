package com.almica.gpssatstatus;

import static android.location.LocationManager.GPS_PROVIDER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.almica.gpssatstatus.databinding.RvSatsBinding;

import java.util.ArrayList;
import java.util.List;

public class SatsFragment extends Fragment {

    private RvSatsBinding binding;
    private SatAdapter adapter;
    private LocationManager locationManager;
    private GnssStatus.Callback gnssStatusCallback;
    private LocListener gpsLocListener;

    public static final long MIN_DELAY = 2000;

    public SatsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RvSatsBinding.inflate(inflater, container, false);
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
    }

    private void setupRecyclerView() {
        adapter = new SatAdapter();
        binding.rv.setAdapter(adapter);
        binding.rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onStart() {
        super.onStart();
        startGpsUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopGpsUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("MissingPermission")
    private void startGpsUpdates() {
        if (locationManager == null) return;

        gpsLocListener = new LocListener(true, new LocListener.InfaLocListener() {
            @Override
            public void clearGpsData() {
                updateSatelliteList(new ArrayList<>());
            }

            @Override
            public void setTimer() {
                // Not used
            }
        });

        locationManager.requestLocationUpdates(GPS_PROVIDER, MIN_DELAY, 0, gpsLocListener);

        gnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                List<Sat> sats = new ArrayList<>();
                float maxSnrFound = 0;
                int usedCount = 0;
                int goodCount = 0;

                for (int i = 0; i < status.getSatelliteCount(); i++) {
                    float snr = status.getCn0DbHz(i);
                    boolean used = status.usedInFix(i);
                    sats.add(new Sat(status.getSvid(i), used, snr));

                    if (snr > maxSnrFound) maxSnrFound = snr;
                    if (used) usedCount++;
                    if (snr > 0) goodCount++;
                }

                sats.sort((s1, s2) -> Float.compare(s2.snr, s1.snr));
                
                final int total = sats.size();
                final int finalGood = goodCount;
                final int finalUsed = usedCount;
                final float finalMaxSnr = maxSnrFound;

                if (binding != null) {
                    binding.totalSatV.setText(String.valueOf(total));
                    binding.goodSatV.setText(String.valueOf(finalGood));
                    binding.usedSatV.setText(String.valueOf(finalUsed));
                }
                adapter.updateData(sats, finalMaxSnr);
            }
        };

        locationManager.registerGnssStatusCallback(gnssStatusCallback, new Handler(Looper.getMainLooper()));
    }

    private void updateSatelliteList(List<Sat> sats) {
        if (binding != null) {
            binding.totalSatV.setText("0");
            binding.goodSatV.setText("0");
            binding.usedSatV.setText("0");
        }
        if (adapter != null) {
            adapter.updateData(sats, 40f);
        }
    }

    private void stopGpsUpdates() {
        if (locationManager != null) {
            if (gpsLocListener != null) {
                locationManager.removeUpdates(gpsLocListener);
                gpsLocListener = null;
            }
            if (gnssStatusCallback != null) {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
                gnssStatusCallback = null;
            }
        }
    }
}
