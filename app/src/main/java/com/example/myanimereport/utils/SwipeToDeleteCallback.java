package com.example.myanimereport.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myanimereport.R;
import com.example.myanimereport.adapters.BacklogItemsAdapter;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
    private final BacklogItemsAdapter adapter;
    private final Drawable iconDelete, iconCheck;
    private final ColorDrawable red, green;
    private Drawable icon;
    private ColorDrawable background;

    /* Constructor takes the backlog items adapter. */
    public SwipeToDeleteCallback(BacklogItemsAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        iconDelete = ContextCompat.getDrawable(adapter.getContext(), R.drawable.ic_baseline_delete_24);
        iconCheck = ContextCompat.getDrawable(adapter.getContext(), R.drawable.ic_baseline_check_24);
        red = new ColorDrawable(Color.RED);
        green = new ColorDrawable(Color.GREEN);
        icon = iconDelete;
        background = red;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    /* Deletes the item when user swipes left. */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.LEFT) {
            adapter.deleteItem(viewHolder.getAdapterPosition());
        } else if (direction == ItemTouchHelper.RIGHT) {
            adapter.addItemAsEntry(viewHolder.getAdapterPosition());
        }
    }

    /* Handles the UI effects of deleting to swipe. */
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        // Set boundaries of the delete icon
        View itemView = viewHolder.itemView;
        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + iconMargin;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        if (dX < 0) { // Swiping to the left
            icon = iconDelete;
            background = red;
            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            background.setBounds(itemView.getRight() + ((int) dX),
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else if (dX > 0) { // Swiping to the right
            icon = iconCheck;
            background = green;
            int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
            int iconLeft = itemView.getLeft() + iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            background.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX), itemView.getBottom());
        } else { // Swiper no swiping
            background.setBounds(0, 0, 0, 0);
        }
        background.draw(c);
        icon.draw(c);
    }
}