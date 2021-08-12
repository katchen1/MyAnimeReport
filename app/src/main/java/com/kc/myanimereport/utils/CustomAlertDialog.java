package com.kc.myanimereport.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import com.kc.myanimereport.R;

public class CustomAlertDialog {

    // Style the colors and background of the alert dialog
    public static void style(AlertDialog alert, Context context) {
        alert.getWindow().setBackgroundDrawableResource(R.drawable.gray_rounded_bg_dark);
        Button nButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
        nButton.setTextColor(context.getColor(R.color.white));
        Button pButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
        pButton.setTextColor(context.getColor(R.color.theme));
    }
}
