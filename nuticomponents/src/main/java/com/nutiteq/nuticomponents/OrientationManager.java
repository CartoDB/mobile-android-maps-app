package com.nutiteq.nuticomponents;

import java.util.LinkedHashSet;
import java.util.Set;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.view.Display;
import android.view.Surface;

/**
 * Collects and communicates information about the user's current orientation.
 */
public class OrientationManager {

	/**
	 * Classes should implement this interface if they want to be notified of
	 * changes in the user's location, orientation, or the accuracy of the
	 * compass.
	 */
	public interface OnChangedListener {
		/**
		 * Called when the user's orientation changes.
		 * 
		 * @param orientationManager
		 *            the orientation manager that detected the change
		 */
		void onOrientationChanged(OrientationManager orientationManager);

		/**
		 * Called when the accuracy of the compass changes.
		 * 
		 * @param orientationManager
		 *            the orientation manager that detected the change
		 */
		void onAccuracyChanged(OrientationManager orientationManager);
	}

	private final SensorManager mSensorManager;
	private final Set<OnChangedListener> mListeners;

	private final Display display;

	private boolean mTracking;
	private float mHeading;
	private float mPitch;
	private Location mLocation;
	private boolean mHasInterference;

	/**
	 * The sensor listener used by the orientation manager.
	 */
	private SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mHasInterference = (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
				notifyAccuracyChanged();
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				switch (display.getRotation()) {
				case Surface.ROTATION_0:
					mHeading = event.values[0];
					break;
				case Surface.ROTATION_90:
					mHeading = event.values[0] + 90;
					if (mHeading > 360) {
						mHeading = mHeading - 360;
					}
					break;
				case Surface.ROTATION_180:
					mHeading = event.values[0] + 180;
					if (mHeading > 360) {
						mHeading = mHeading - 360;
					}
					break;
				case Surface.ROTATION_270:
					mHeading = event.values[0] + -90;
					if (mHeading < 0) {
						mHeading = 360 + mHeading;
					}
					break;
				default:
					mHeading = event.values[0];
				}

				notifyOrientationChanged();
			}
		}
	};

	/**
	 * Initializes a new instance of {@code OrientationManager}, using the
	 * specified context to access system services.
	 */
	public OrientationManager(SensorManager sensorManager, Display display) {
		mSensorManager = sensorManager;
		mListeners = new LinkedHashSet<OnChangedListener>();
		this.display = display;
	}

	/**
	 * Adds a listener that will be notified when the user's location or
	 * orientation changes.
	 */
	public void addOnChangedListener(OnChangedListener listener) {
		mListeners.add(listener);
	}

	/**
	 * Removes a listener from the list of those that will be notified when the
	 * user's location or orientation changes.
	 */
	public void removeOnChangedListener(OnChangedListener listener) {
		mListeners.remove(listener);
	}

	private boolean hasCompass;

	public boolean hasCompassSensors() {
		if (hasCompass) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Starts tracking the user's location and orientation. After calling this
	 * method, any {@link OrientationManager.OnChangedListener}s added to this
	 * object will be notified of these events.
	 */
	@SuppressWarnings("deprecation")
	public void start() {
		if (!mTracking) {
			if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
				hasCompass = true;
				mSensorManager.registerListener(mSensorListener, mSensorManager
						.getDefaultSensor(Sensor.TYPE_ORIENTATION),
						SensorManager.SENSOR_DELAY_UI);
			} else {
				hasCompass = false;
			}

			mTracking = true;
		}
	}

	/**
	 * Stops tracking the user's location and orientation. Listeners will no
	 * longer be notified of these events.
	 */
	public void stop() {
		if (mTracking) {
			mSensorManager.unregisterListener(mSensorListener);
			mTracking = false;
		}
	}

	/**
	 * Gets a value indicating whether there is too much magnetic field
	 * interference for the compass to be reliable.
	 * 
	 * @return true if there is magnetic interference, otherwise false
	 */
	public boolean hasInterference() {
		return mHasInterference;
	}

	/**
	 * Gets a value indicating whether the orientation manager knows the user's
	 * current location.
	 * 
	 * @return true if the user's location is known, otherwise false
	 */
	public boolean hasLocation() {
		return mLocation != null;
	}

	/**
	 * Gets the user's current heading, in degrees. The result is guaranteed to
	 * be between 0 and 360.
	 * 
	 * @return the user's current heading, in degrees
	 */
	public float getHeading() {
		return mHeading;
	}

	/**
	 * Gets the user's current pitch (head tilt angle), in degrees. The result
	 * is guaranteed to be between -90 and 90.
	 * 
	 * @return the user's current pitch angle, in degrees
	 */
	public float getPitch() {
		return mPitch;
	}

	/**
	 * Gets the user's current location.
	 * 
	 * @return the user's current location
	 */
	public Location getLocation() {
		return mLocation;
	}

	/**
	 * Notifies all listeners that the user's orientation has changed.
	 */
	private void notifyOrientationChanged() {
		for (OnChangedListener listener : mListeners) {
			listener.onOrientationChanged(this);
		}
	}

	/**
	 * Notifies all listeners that the compass's accuracy has changed.
	 */
	private void notifyAccuracyChanged() {
		for (OnChangedListener listener : mListeners) {
			listener.onAccuracyChanged(this);
		}
	}
}