package net.osmand.plus.chooseplan.button;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;

public class ButtonUiUtilities {

	private OsmandApplication app;
	private boolean nightMode;

	public ButtonUiUtilities(@NonNull OsmandApplication app,
	                         boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public void setupRoundedBackground(@NonNull View view, ButtonBackground background) {
		setupRoundedBackground(view, ContextCompat.getColor(app, getActiveColorId(nightMode)), background);
	}

	public void setupRoundedBackground(@NonNull View view, @ColorInt int color, ButtonBackground background) {
		Drawable normal = createRoundedDrawable(UiUtilities.getColorWithAlpha(color, 0.1f), background);
		setupRoundedBackground(view, normal, color, background);
	}

	public void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal,
	                                   @ColorInt int color, ButtonBackground background) {
		Drawable selected = createRoundedDrawable(UiUtilities.getColorWithAlpha(color, 0.5f), background);
		setupRoundedBackground(view, normal, selected);
	}

	public void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal, @NonNull Drawable selected) {
		Drawable background;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			background = UiUtilities.getLayeredIcon(normal, getRippleDrawable());
		} else {
			background = AndroidUtils.createPressedStateListDrawable(normal, selected);
		}
		AndroidUtils.setBackground(view, background);
	}

	public Drawable getActiveStrokeDrawable() {
		return app.getUIUtilities().getIcon(nightMode ?
				R.drawable.btn_background_stroked_active_dark :
				R.drawable.btn_background_stroked_active_light);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public Drawable getRippleDrawable() {
		return AppCompatResources.getDrawable(app, nightMode ?
				R.drawable.purchase_button_ripple_dark :
				R.drawable.purchase_button_ripple_light);
	}

	public Drawable createRoundedDrawable(@ColorInt int color, ButtonBackground background) {
		return UiUtilities.createTintedDrawable(app, background.drawableId, color);
	}

}
