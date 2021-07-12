package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.FragmentHomeBinding;
import com.example.myanimereport.models.Entry;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private List<Entry> entries;
    private EntriesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up adapter and layout of recycler view
        entries = new ArrayList<>();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        adapter = new EntriesAdapter(getContext(), entries);
        binding.rvEntries.setLayoutManager(layoutManager);
        binding.rvEntries.setAdapter(adapter);

        // Add placeholder entries
        for (int i = 0; i < 11; i++) entries.add(new Entry());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}