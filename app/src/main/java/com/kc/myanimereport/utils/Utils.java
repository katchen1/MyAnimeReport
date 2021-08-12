package com.kc.myanimereport.utils;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;
import com.kc.myanimereport.R;

public class Utils {

    /* Shows or hides a password. */
    public static void togglePasswordVisibility(EditText et, ImageButton imgBtn) {
        // Get current visibility
        PasswordTransformationMethod ptm = PasswordTransformationMethod.getInstance();
        HideReturnsTransformationMethod hrtm = HideReturnsTransformationMethod.getInstance();
        boolean invisible = et.getTransformationMethod() == ptm;

        // Show or hide based on current visibility
        if (invisible) {
            imgBtn.setImageResource(R.drawable.ic_baseline_visibility_24);
            et.setTransformationMethod(hrtm);
        } else {
            imgBtn.setImageResource(R.drawable.ic_baseline_visibility_off_24);
            et.setTransformationMethod(ptm);
        }

        // Move cursor to end of text
        et.setSelection(et.getText().length());
    }
}
