package io.brixby.parking.map;

import android.animation.LayoutTransition;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.brixby.parking.R;
import io.brixby.parking.model.MapObject;
import io.brixby.parking.model.ZoneInfo;

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_HIDDEN;



public class SlidingPanel extends CoordinatorLayout {

    public interface BottomViewListener {
        void onBottomViewClicked(ZoneInfo zoneInfo);

        // TODO: 12/26/16 cancel request
//        void onPanelHidden()
    }

    @BindView(R.id.panel_content) ViewGroup panel;
    @BindView(R.id.data) View dataView;
    @BindView(R.id.progress) View progress;

    private BottomSheetBehavior<ViewGroup> bottomSheet;

    private MarginLayoutParams layoutParams;
    private int layoutBottomMargin;

    private View bottomView;
    private BottomViewListener bottomViewListener;

    private boolean isRu;

    @BindView(R.id.parkinfo_name) TextView parkName;
    @BindView(R.id.parkinfo_top_block) View parkTopBlock;
    @BindView(R.id.parkinfo_top_separator) View parkTopSeparator;
    @BindView(R.id.parkinfo_top_30m) TextView parkTop30m;
    @BindView(R.id.parkinfo_top_30m_block) View parkTop30mBlock;
    @BindView(R.id.parkinfo_top_30m_separator) View parkTop30mSeparator;
    @BindView(R.id.parkinfo_top_1h) TextView parkTop1h;
    @BindView(R.id.parkinfo_top_1h_block) View parkTop1hBlock;
    @BindView(R.id.parkinfo_top_1h_separator) View parkTop1hSeparator;
    @BindView(R.id.parkinfo_top_capacity) TextView parkTopCapacity;
    @BindView(R.id.parkinfo_price) TextView parkPrice;
    @BindView(R.id.parkinfo_price_block) View parkPriceBlock;
    @BindView(R.id.parkinfo_price_separator) View parkPriceSeparator;
    @BindView(R.id.parkinfo_2h) TextView park2h;
    @BindView(R.id.parkinfo_2h_block) View park2hBlock;
    @BindView(R.id.parkinfo_2h_separator) View park2hSeparator;
    @BindView(R.id.parkinfo_night) TextView parkNight;
    @BindView(R.id.parkinfo_night_block) View parkNightBlock;
    @BindView(R.id.parkinfo_night_separator) View parkNightSeparator;
    @BindView(R.id.parkinfo_month) TextView parkMonth;
    @BindView(R.id.parkinfo_month_block) View parkMonthBlock;
    @BindView(R.id.parkinfo_month_separator) View parkMonthSeparator;
    @BindView(R.id.parkinfo_capacity) TextView parkCapacity;
    @BindView(R.id.parkinfo_capacity_block) View parkCapacityBlock;
    @BindView(R.id.parkinfo_capacity_separator) View parkCapacitySeparator;
    @BindView(R.id.parkinfo_metro) TextView parkMetro;
    @BindView(R.id.parkinfo_metro_block) View parkMetroBlock;
    @BindView(R.id.parkinfo_metro_separator) View parkMetroSeparator;
    @BindView(R.id.parkinfo_additional) TextView parkAdditional;
    @BindView(R.id.parkinfo_additional_block) View parkAdditionalBlock;
    @BindView(R.id.parkinfo_additional_separator) View parkAdditionalSeparator;

    public SlidingPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.view_panel_parking, this);
    }

    public void init(View bottomView, BottomViewListener bottomViewListener, boolean isRu) {
        this.bottomView = bottomView;
        this.bottomViewListener = bottomViewListener;
        this.isRu = isRu;

        ButterKnife.bind(this);

        LayoutTransition transition = new LayoutTransition();
        transition.setAnimateParentHierarchy(false);
        panel.setLayoutTransition(transition);

        bottomSheet = BottomSheetBehavior.from(panel);
        bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomView.setVisibility(GONE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        layoutParams = (MarginLayoutParams) getLayoutParams();
        layoutBottomMargin = layoutParams.bottomMargin;
    }

    public void showLoading(MapObject mapObject) {
        layoutParams.bottomMargin = layoutBottomMargin;
        panel.getLayoutParams().height = bottomSheet.getPeekHeight();
        requestLayout();

        bottomView.setVisibility(VISIBLE);
        progress.setVisibility(VISIBLE);
        dataView.setVisibility(INVISIBLE);
        bottomSheet.setState(STATE_COLLAPSED);

        parkName.setText(isRu ? mapObject.getTitleRu() : mapObject.getTitleEn());
        bottomView.setOnClickListener(null);
    }

    public void showData(MapObject mapObject, ZoneInfo zoneInfo) {
        layoutParams.bottomMargin = layoutBottomMargin;
        panel.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
        requestLayout();

        if (bottomSheet.getState() != STATE_COLLAPSED && bottomSheet.getState() != STATE_HIDDEN) {
            bottomSheet.setState(STATE_COLLAPSED);
        }

        dataView.setVisibility(VISIBLE);
        progress.setVisibility(INVISIBLE);

        updateView(mapObject, zoneInfo);
        updateBottomView(zoneInfo);
    }

    public void showError() {
        parkName.setText(R.string.parkinfo_no_data);
        progress.setVisibility(INVISIBLE);
    }

    public void hide() {
        layoutParams.bottomMargin = 0;
        requestLayout();

        bottomView.setVisibility(GONE);
        bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void updateView(MapObject mapObject, ZoneInfo zoneInfo) {
        boolean showPrice = false;
        boolean showCapacityAfter = false;

        Map<String, String> rates = parseParkingRate(zoneInfo.getRate() != null ? zoneInfo.getRate() : mapObject.getRate());

        String rate30m = rates.get("30m");
        String rate1h = rates.get("1h");
        String rate30mValue = TextUtils.isEmpty(rate30m) ? null : rate30m;
        String rate1hValue = TextUtils.isEmpty(rate1h) ? null : rate1h;

        if (rate30mValue == null && rate1hValue == null) {
            parkTopBlock.setVisibility(GONE);
            parkTopSeparator.setVisibility(GONE);
            showPrice = true;
            showCapacityAfter = true;
        } else {
            parkTopBlock.setVisibility(VISIBLE);
            parkTopSeparator.setVisibility(VISIBLE);
            updateItemView(parkTop30mBlock, parkTop30m, rate30mValue, parkTop30mSeparator);
            updateItemView(parkTop1hBlock, parkTop1h, rate1hValue, parkTop1hSeparator);
        }

        updateItemView(park2hBlock, park2h, rates.get("2h"), park2hSeparator);
        updateItemView(parkNightBlock, parkNight, rates.get("night"), parkNightSeparator);
        updateItemView(parkMonthBlock, parkMonth, rates.get("1m"), parkMonthSeparator);
        updateItemView(parkMetroBlock, parkMetro, isRu ? zoneInfo.getMetroRu() : zoneInfo.getMetroEn(), parkMetroSeparator);
        updateItemView(parkAdditionalBlock, parkAdditional, zoneInfo.getAdditional(), parkAdditionalSeparator);

        if (showPrice) {
            String price = zoneInfo.getPrice();
            if (TextUtils.isEmpty(price)) {
                parkPrice.setText(R.string.info_no_price);
            } else {
                try {
                    int priceInt = Integer.valueOf(price);
                    if (priceInt <= 0) {
                        parkPrice.setText(R.string.info_price_free);
                    } else {
                        parkPrice.setText(String.valueOf(priceInt) + " " + zoneInfo.getCurrencyPeriod(getContext()));
                    }
                } catch (NumberFormatException e) {
                    parkPrice.setText(price);
                }
            }
            parkPriceSeparator.setVisibility(VISIBLE);
            parkPriceBlock.setVisibility(VISIBLE);
        } else {
            parkPriceSeparator.setVisibility(GONE);
            parkPriceBlock.setVisibility(GONE);
        }

        Integer capacity = zoneInfo.getCapacity();
        String capacityValue = capacity != null && capacity > 0 ? String.valueOf(capacity) : null;

        if (showCapacityAfter) {
            updateItemView(parkCapacityBlock, parkCapacity, capacityValue, findViewById(R.id.parkinfo_capacity_separator));
        } else {
            parkCapacityBlock.setVisibility(GONE);
            updateItemView(parkTopCapacity, parkTopCapacity, capacityValue, null);
        }
    }

    private Map<String, String> parseParkingRate(String rate) {
        if (TextUtils.isEmpty(rate)) {
            return new HashMap<>(0);
        }

        String[] rates = rate.split(";");
        HashMap<String, String> rateMap = new HashMap<>(rates.length);
        for (String ratePart : rates) {
            String[] rateData = ratePart.split(":");
            if (rateData.length == 2) {
                rateMap.put(rateData[0], rateData[1]);
            }
        }

        return rateMap;
    }

    private void updateItemView(View block, TextView text, String value, @Nullable View separator) {
        if (!TextUtils.isEmpty(value)) {
            text.setText(value);
            block.setVisibility(VISIBLE);
            if (separator != null) separator.setVisibility(VISIBLE);
        } else {
            block.setVisibility(GONE);
            if (separator != null) separator.setVisibility(GONE);
        }
    }

    private void updateBottomView(ZoneInfo zoneInfo) {
        if (zoneInfo.canPark()) {
            bottomView.setOnClickListener(view -> bottomViewListener.onBottomViewClicked(zoneInfo));
            bottomView.setBackgroundResource(R.drawable.bg_btn_green);
            bottomView.setEnabled(true);
        } else {
            bottomView.setOnClickListener(null);
            bottomView.setBackgroundResource(R.drawable.bg_btn_flat_white);
            bottomView.setEnabled(false);
        }
        bottomView.setVisibility(VISIBLE);
    }

}
