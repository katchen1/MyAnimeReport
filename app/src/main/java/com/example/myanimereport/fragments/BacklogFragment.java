package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.FragmentBacklogBinding;

public class BacklogFragment extends Fragment {

    private FragmentBacklogBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBacklogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textBacklog.setText(R.string.title_backlog);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}