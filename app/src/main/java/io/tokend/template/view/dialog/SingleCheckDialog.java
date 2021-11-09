package io.tokend.template.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;

import java.util.List;

import io.tokend.template.R;

/**
 * Created by spirit111 on 06.04.2017.
 */

public class SingleCheckDialog {
    private List<? extends CharSequence> items;
    private Context context;
    private int checkedIndex;
    private AlertDialog.Builder dialogBuilder;
    private DialogInterface.OnClickListener positiveButtonListener;
    private DialogInterface.OnClickListener neutralButtonListener;
    private View.OnClickListener nonClosingNeutralButtonListener;
    private DialogInterface.OnCancelListener cancelListener;
    private String message;
    private String neutralButtonTitle;
    private List<Integer> itemsColors;

    public SingleCheckDialog(Context context, List<? extends CharSequence> items) {
        this.context = context;
        this.items = items;

        dialogBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(context, R.style.AlertDialogStyle),
                R.style.AlertDialogStyle
        );
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
    }

    public SingleCheckDialog(Context context, List<? extends CharSequence> items, List<Integer> itemsColors) {
        this(context, items);
        this.itemsColors = itemsColors;
    }

    public SingleCheckDialog setDefaultCheckIndex(int checkedIndex) {
        this.checkedIndex = checkedIndex;
        return this;
    }

    public SingleCheckDialog setTitle(int resId) {
        return setTitle(context.getString(resId));
    }

    public SingleCheckDialog setTitle(String title) {
        dialogBuilder.setTitle(title);
        return this;
    }

    public SingleCheckDialog setMessage(int resId) {
        return setMessage(context.getString(resId));
    }

    public SingleCheckDialog setMessage(String message) {
        this.message = message;
        return this;
    }

    public SingleCheckDialog setPositiveButtonListener(DialogInterface.OnClickListener positiveButtonListener) {
        this.positiveButtonListener = positiveButtonListener;
        return this;
    }

    public SingleCheckDialog setOnCancelListener(DialogInterface.OnCancelListener cancelListener) {
        this.cancelListener = cancelListener;
        return this;
    }

    public SingleCheckDialog setNeutralButton(String title, DialogInterface.OnClickListener
            clickListener) {
        this.neutralButtonListener = clickListener;
        this.neutralButtonTitle = title;
        return this;
    }

    public SingleCheckDialog setNonClosingNeutralButton(String title,
                                                        View.OnClickListener clickListener) {
        this.nonClosingNeutralButtonListener = clickListener;
        return setNeutralButton(title, null);
    }

    @SuppressLint("RestrictedApi")
    public AlertDialog show() {
        int itemsSpacing =
                context.getResources()
                        .getDimensionPixelSize(R.dimen.standard_margin);
        int itemSpacingLeft = context.getResources()
                .getDimensionPixelSize(R.dimen.single_check_dialog_left_margin);

        final ScrollView parentLayout = new ScrollView(context);
        final RadioGroup radioGroup = getRadioGroup(context, items, checkedIndex);

        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        if (this.message != null) {
            TextView messageTextView = new TextView(context);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(context.getResources()
                            .getDimensionPixelSize(R.dimen.quarter_standard_margin),
                    0, 0, itemsSpacing);
            messageTextView.setLayoutParams(layoutParams);
            messageTextView.setText(message);
            messageTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimensionPixelSize(R.dimen.text_size_dialog)
            );
            dialogLayout.addView(messageTextView);
        }

        dialogLayout.addView(radioGroup);
        parentLayout.addView(dialogLayout);

        dialogBuilder
                .setView(parentLayout, itemSpacingLeft, itemsSpacing, itemsSpacing, 0)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (positiveButtonListener != null) {
                            positiveButtonListener.onClick(dialogInterface,
                                    radioGroup.getCheckedRadioButtonId());
                        }
                    }
                })
                .setOnCancelListener(cancelListener);

        if (neutralButtonTitle != null) {
            dialogBuilder.setNeutralButton(neutralButtonTitle, neutralButtonListener);
        }

        AlertDialog dialog = dialogBuilder.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(context,
                        R.color.primary_action));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context,
                        R.color.primary_action));
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        neutralButton.setTextColor(ContextCompat.getColor(context,
                R.color.primary_action));
        if (nonClosingNeutralButtonListener != null) {
            neutralButton.setOnClickListener(nonClosingNeutralButtonListener);
        }

        return dialog;
    }

    @SuppressLint("RestrictedApi")
    private RadioGroup getRadioGroup(Context context, List<? extends CharSequence> items,
                                     int checkedIndex) {
        RadioGroup radioGroup = new RadioGroup(context);
        for (int i = 0; i < items.size(); i++) {
            AppCompatRadioButton radioButton =
                    (AppCompatRadioButton) LayoutInflater.from(context)
                            .inflate(R.layout.single_check_dialog_item, radioGroup, false);
            radioButton.setId(i);
            radioButton.setText(items.get(i));
            radioButton.setChecked(i == checkedIndex);
            radioButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            if (itemsColors != null) {
                Integer color = getItemColor(i);
                if (color == Color.WHITE) {
                    //White is invisible on white background
                    //Need use light gray
                    color = 0xFFCCCCCC;
                }
                radioButton.setSupportButtonTintList(new ColorStateList(new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                        new int[]{color, color}));
            }

            radioGroup.addView(radioButton);
        }

        return radioGroup;
    }

    private Integer getItemColor(int itemIndex) {
        if (itemIndex > itemsColors.size()) {
            return context.getResources().getColor(R.color.accent);
        }
        return itemsColors.get(itemIndex);
    }
}
