package net.osmand.plus.chooseplan.button;

import net.osmand.plus.R;

public enum ButtonBackground {
	ROUNDED(R.drawable.rectangle_rounded),
	ROUNDED_SMALL(R.drawable.rectangle_rounded_small),
	ROUNDED_LARGE(R.drawable.rectangle_rounded_large);

	ButtonBackground(int drawableId) {
		this.drawableId = drawableId;
	}

	public int drawableId;
}
