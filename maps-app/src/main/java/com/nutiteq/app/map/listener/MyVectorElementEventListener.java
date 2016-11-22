package com.nutiteq.app.map.listener;


import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.carto.core.ScreenPos;
import com.carto.graphics.Color;
import com.carto.layers.VectorElementEventListener;
import com.carto.styles.BalloonPopupMargins;
import com.carto.styles.BalloonPopupStyle;
import com.carto.styles.BalloonPopupStyleBuilder;
import com.carto.ui.ClickType;
import com.carto.ui.MapView;
import com.carto.ui.VectorElementClickInfo;
import com.carto.vectorelements.BalloonPopup;
import com.carto.vectorelements.Billboard;
import com.carto.vectorelements.Marker;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Polygon;
import com.carto.vectorelements.VectorElement;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.map.RouteView;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.nuticomponents.customviews.BottomView;
import com.nutiteq.nuticomponents.customviews.LocationView;

public class MyVectorElementEventListener extends VectorElementEventListener {

    private MapView mapView;
    private MyMapEventListener myMapEventListener;
    private BottomView bottomView;
    private View emptyView;
    private LocationView locationView;
    private RouteView routeView;

    private Context context;

    private Billboard billboard;

    private VectorElement vectorElement;
    private VectorElement selectedVectorElement;

    private BalloonPopupStyleBuilder balloonPopupStyleBuilder;

    // contains information about selected location bookmark id and vector
    // element, it's init when user click on location bookmarks pin
    private String favoriteID;

    private Handler handler;

    private int strokeWidth;

    private Marker longClickPin;

    public MyVectorElementEventListener(MapView mapView, MyMapEventListener myMapEventListener, Marker longClickPin, BottomView bottomView, View emptyView, LocationView locationView, RouteView routeView) {
        this.mapView = mapView;
        this.myMapEventListener = myMapEventListener;
        this.longClickPin = longClickPin;
        this.bottomView=bottomView;
        this.emptyView=emptyView;
        this.locationView=locationView;
        this.routeView=routeView;

        context = mapView.getContext();

        handler = new Handler(context.getMainLooper());

        strokeWidth= MainActivity.strokeWidth;

        balloonPopupStyleBuilder = new BalloonPopupStyleBuilder();
        balloonPopupStyleBuilder.setCornerRadius(2);
        balloonPopupStyleBuilder.setTriangleHeight(9);

        balloonPopupStyleBuilder.setRightMargins(new BalloonPopupMargins(10, 10, 10, 10));
        balloonPopupStyleBuilder.setLeftMargins(new BalloonPopupMargins(10, 10, 10, 10));
        balloonPopupStyleBuilder.setTitleMargins(new BalloonPopupMargins(10, 10, 0, 0));
        balloonPopupStyleBuilder.setDescriptionMargins(new BalloonPopupMargins(10, 0, 0, 10));

        balloonPopupStyleBuilder.setLeftColor(new Color((short) 15, (short) 59, (short) 130, (short) 255));
        balloonPopupStyleBuilder.setTitleColor(new Color((short) 48, (short) 48, (short) 48, (short) 255));
        balloonPopupStyleBuilder.setDescriptionColor(new Color((short) 133, (short) 133, (short) 133, (short) 255));
        balloonPopupStyleBuilder.setStrokeColor(new Color((short) 208, (short) 208, (short) 208, (short) 108));

        balloonPopupStyleBuilder.setStrokeWidth(strokeWidth);
        balloonPopupStyleBuilder.setPlacementPriority(1);
    }

    @Override
    public boolean onVectorElementClicked(
            final VectorElementClickInfo clickInfo) {
        vectorElement = clickInfo.getVectorElement();

        if (vectorElement instanceof Marker) {
            favoriteID = (vectorElement).getMetaDataElement("id").getString();

            selectedVectorElement = vectorElement;

            if (favoriteID != null && !favoriteID.equals("")) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        emptyView.setVisibility(View.VISIBLE);
                        bottomView.show(
                                (vectorElement).getMetaDataElement("description").getString(),
                                vectorElement.getGeometry().getCenterPos(),
                                false
                        );
                        locationView.goUp();
                        longClickPin.setVisible(false);
                    }
                });
            } else {
                favoriteID = "-1";
                // set zoom to 17 if it is smaller than 17
                if (mapView.getZoom() < 17) {
                    mapView.zoom(17 - mapView.getZoom(), ((Marker) vectorElement)
                            .getRootGeometry().getCenterPos(), 0.6f);
                }
            }
        } else if (vectorElement instanceof Polygon) {
            if (clickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
                myMapEventListener.longClick(clickInfo.getClickPos());
            }
        } else {
            if (bottomView.getVisibility() == View.VISIBLE) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        longClickPin.setVisible(false);
                        bottomView.hide();
                        routeView.setVisibility(View.GONE);
                        mapView.getOptions().setFocusPointOffset(new ScreenPos(0, 0));
                        locationView.goDown();
                        emptyView.setVisibility(View.GONE);
                    }
                });
            }
        }

        BalloonPopup clickPopup = null;

        // If the element is billboard, attach the click label to the billboard element
        if (vectorElement instanceof Billboard) {

            billboard = (Billboard) vectorElement;
            String title = billboard.getMetaDataElement("title").getString();
            String description = billboard.getMetaDataElement("description").getString();

            BalloonPopupStyle style = balloonPopupStyleBuilder.buildStyle();

            if (!title.equals("") && !description.equals("")) {
                clickPopup = new BalloonPopup(billboard, style, title, description);
            }

        } else if (vectorElement instanceof com.carto.vectorelements.Point) {

            Point point = (Point) vectorElement;
            String bikes = vectorElement.getMetaDataElement("bikes").getString();
            String slot = vectorElement.getMetaDataElement("slot").getString();

            BalloonPopupStyle style = balloonPopupStyleBuilder.buildStyle();
            String title = vectorElement.getMetaDataElement("name").getString();

            String description = "\n" + context.getString(R.string.available_bikes) + "\n" +
                    bikes + "\n" + context.getString(R.string.empty_docks) + "\n" + slot + "\n";

            clickPopup = new BalloonPopup(point.getGeometry(), style, title, description);
        }

        // Remove old click label
        myMapEventListener.removeOldClickLabel(clickPopup);

        return true;
    }
}
