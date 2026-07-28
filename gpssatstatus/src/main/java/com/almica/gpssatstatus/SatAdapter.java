package com.almica.gpssatstatus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.almica.gpssatstatus.databinding.SatItemBinding;

import java.util.concurrent.ThreadLocalRandom;

public class SatAdapter extends ListAdapter<Sat, SatAdapter.SatViewHolder> {

    private float maxSnr = 40f; // Default reasonable value

    protected SatAdapter() {
        super(new DiffUtil.ItemCallback<Sat>() {
            @Override
            public boolean areItemsTheSame(@NonNull Sat oldItem, @NonNull Sat newItem) {
                return oldItem.prn == newItem.prn;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Sat oldItem, @NonNull Sat newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    public void updateData(java.util.List<Sat> sats, float maxSnr) {
        this.maxSnr = Math.max(maxSnr, 1.0f); // Avoid division by zero
        submitList(sats);
    }

    @NonNull
    @Override
    public SatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new SatViewHolder(SatItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SatViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    protected class SatViewHolder extends RecyclerView.ViewHolder {

        private final SatItemBinding binding;

        public SatViewHolder(@NonNull SatItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(Sat sat) {
            binding.idV.setText(String.valueOf(sat.prn));

            int strength = Math.min((int) (100 * sat.snr / maxSnr), 100);
            if (strength < 5 && sat.snr > 0) {
                strength = ThreadLocalRandom.current().nextInt(1, 4);
            }
            binding.progV.setProgress(strength);
            binding.signalV.setText(String.valueOf(sat.snr));
            binding.fixedV.setVisibility(sat.used ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
