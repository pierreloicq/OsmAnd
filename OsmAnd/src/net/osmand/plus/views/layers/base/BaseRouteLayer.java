package net.osmand.plus.views.layers.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.jni.FColorARGB;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;

public abstract class BaseRouteLayer extends OsmandMapLayer {

	private static final Log log = PlatformUtil.getLog(BaseRouteLayer.class);

	private static final int DEFAULT_WIDTH_MULTIPLIER = 7;

	protected boolean nightMode;

	protected PreviewRouteLineInfo previewRouteLineInfo;
	protected ColoringType routeColoringType = ColoringType.DEFAULT;
	protected String routeInfoAttribute;

	protected RenderingLineAttributes attrs;
	protected int routeLineColor;
	protected Integer directionArrowsColor;
	protected int customTurnArrowColor = 0;

	private final Map<String, Float> cachedRouteLineWidth = new HashMap<>();

	protected Paint paintIconAction;
	private Bitmap actionArrow;

	//OpenGL
	//kOutlineColor 150, 0, 0, 0
	public FColorARGB kOutlineColor;
	public static final int kOutlineWidth = 10;
	public static final int kOutlineId = 1001;

	public BaseRouteLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		init();
	}

	private void init() {
		float density = view.getDensity();
		initAttrs(density);
		initGeometries(density);
		initPaints();
		initIcons();
	}

	protected void initAttrs(float density) {
		attrs = new RenderingLineAttributes("route");
		attrs.defaultWidth = (int) (12 * density);
		attrs.defaultWidth3 = (int) (7 * density);
		attrs.defaultColor = ContextCompat.getColor(getContext(), R.color.nav_track);
		attrs.paint3.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint3.setColor(Color.WHITE);
		attrs.paint2.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint2.setColor(Color.BLACK);
	}

	protected void initPaints() {
		paintIconAction = new Paint();
		paintIconAction.setFilterBitmap(true);
		paintIconAction.setAntiAlias(true);
	}

	protected void initIcons() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);
	}

	protected void updateRouteColors(boolean night) {
		if (routeColoringType.isCustomColor()) {
			updateCustomColor(night);
		} else {
			directionArrowsColor = null;
			updateAttrs(new DrawSettings(night), view.getCurrentRotatedTileBox());
			routeLineColor = attrs.paint.getColor();
		}
		updateTurnArrowColor();
	}

	private void updateCustomColor(boolean night) {
		int customColor;
		if (previewRouteLineInfo != null) {
			customColor = previewRouteLineInfo.getCustomColor(night);
		} else {
			CommonPreference<Integer> colorPreference = night
					? view.getSettings().CUSTOM_ROUTE_COLOR_NIGHT
					: view.getSettings().CUSTOM_ROUTE_COLOR_DAY;
			customColor = colorPreference.getModeValue(getAppMode());
		}

		if (routeLineColor != customColor) {
			directionArrowsColor = ColorUtilities.getContrastColor(getContext(), customColor, false);
		}
		routeLineColor = customColor;
	}

	protected void updateRouteColoringType() {
		if (previewRouteLineInfo != null) {
			routeColoringType = previewRouteLineInfo.getRouteColoringType();
			routeInfoAttribute = previewRouteLineInfo.getRouteInfoAttribute();
		} else {
			ApplicationMode mode = view.getApplication().getRoutingHelper().getAppMode();
			OsmandSettings settings = view.getSettings();
			routeColoringType = settings.ROUTE_COLORING_TYPE.getModeValue(mode);
			routeInfoAttribute = settings.ROUTE_INFO_ATTRIBUTE.getModeValue(mode);
		}
	}

	@ColorInt
	public int getRouteLineColor(boolean night) {
		updateRouteColors(night);
		return routeLineColor;
	}

	@ColorInt
	public int getRouteLineColor() {
		return routeLineColor;
	}

	protected float getRouteLineWidth(@NonNull RotatedTileBox tileBox) {
		String widthKey;
		if (previewRouteLineInfo != null) {
			widthKey = previewRouteLineInfo.getWidth();
		} else {
			widthKey = view.getSettings().ROUTE_LINE_WIDTH.getModeValue(getAppMode());
		}
		float width = widthKey != null ? getWidthByKey(tileBox, widthKey) : attrs.paint.getStrokeWidth();
		return width * getCarScaleCoef(false);
	}

	@Nullable
	protected Float getWidthByKey(RotatedTileBox tileBox, String widthKey) {
		Float resultValue = cachedRouteLineWidth.get(widthKey);
		if (resultValue != null) {
			return resultValue;
		}
		if (!Algorithms.isEmpty(widthKey) && Algorithms.isInt(widthKey)) {
			try {
				int widthDp = Integer.parseInt(widthKey);
				resultValue = (float) AndroidUtils.dpToPx(view.getApplication(), widthDp);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
				resultValue = DEFAULT_WIDTH_MULTIPLIER * view.getDensity();
			}
		} else {
			RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.getZoom());
			req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.getZoom());
			RenderingRuleProperty ctWidth = rrs.PROPS.get(CURRENT_TRACK_WIDTH_ATTR);
			if (ctWidth != null) {
				req.setStringFilter(ctWidth, widthKey);
			}
			if (req.searchRenderingAttribute("gpx")) {
				OsmandRenderer.RenderingContext rc = new OsmandRenderer.RenderingContext(getContext());
				rc.setDensityValue((float) tileBox.getMapDensity());
				resultValue = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH);
			}
		}
		cachedRouteLineWidth.put(widthKey, resultValue);
		return resultValue;
	}

	protected boolean shouldShowTurnArrows() {
		if (previewRouteLineInfo != null) {
			return previewRouteLineInfo.shouldShowTurnArrows();
		} else {
			return view.getSettings().ROUTE_SHOW_TURN_ARROWS.getModeValue(getAppMode());
		}
	}

	protected void drawTurnArrow(Canvas canvas, Matrix matrix, float x, float y, float px, float py) {
		double angleRad = Math.atan2(y - py, x - px);
		double angle = (angleRad * 180 / Math.PI) + 90f;
		double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		if (distSegment == 0) {
			return;
		}

		float pdx = x - px;
		float pdy = y - py;
		float scale = attrs.paint3.getStrokeWidth() / (actionArrow.getWidth() / 2.25f);
		float scaledWidth = actionArrow.getWidth();
		matrix.reset();
		matrix.postTranslate(0, -actionArrow.getHeight() / 2f);
		matrix.postRotate((float) angle, actionArrow.getWidth() / 2f, 0);
		if (scale > 0) {
			matrix.postScale(scale, scale);
			scaledWidth *= scale;
		}
		matrix.postTranslate(px + pdx - scaledWidth / 2f, py + pdy);
		canvas.drawBitmap(actionArrow, matrix, paintIconAction);
	}

	protected void drawIcon(Canvas canvas, Drawable drawable, int locationX, int locationY) {
		drawable.setBounds(locationX - drawable.getIntrinsicWidth() / 2,
				locationY - drawable.getIntrinsicHeight() / 2,
				locationX + drawable.getIntrinsicWidth() / 2,
				locationY + drawable.getIntrinsicHeight() / 2);
		drawable.draw(canvas);
	}

	public boolean isPreviewRouteLineVisible() {
		return previewRouteLineInfo != null;
	}

	public void setPreviewRouteLineInfo(PreviewRouteLineInfo previewInfo) {
		this.previewRouteLineInfo = previewInfo;
	}

	protected ApplicationMode getAppMode() {
		return view.getApplication().getRoutingHelper().getAppMode();
	}

	protected abstract void initGeometries(float density);

	protected abstract void updateAttrs(DrawSettings settings, RotatedTileBox tileBox);

	protected abstract void updateTurnArrowColor();
}