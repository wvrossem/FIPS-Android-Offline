package ips.android.offline;

import ips.android.R;
import ips.data.entities.Position;
import ips.data.entities.wlan.AccessPoint;
import ips.data.entities.wlan.AccessPointPowerLevels;
import ips.data.entities.wlan.GridMap;
import ips.data.entities.wlan.GridPosition;
import ips.data.entities.wlan.WLANFingerprint;
import ips.data.entities.wlan.WLANMeasurement;
import ips.data.serialization.Serializer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.TransformException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class Fingerprinting extends Activity {

	private static final String TAG = "FP";

	private static final int SETTINGS_CODE = 0;

	private ProgressDialog mProgressDialog;

	private WifiManager mWifiManager;

	private IntentFilter mIntentFilter;

	private BroadcastReceiver mBroadcastReceiver;

	private ArrayList<ScanResult> mScanResults;

	private Rect lastRect;

	// Physical display width and height.
	private static int DISPLAY_WIDTH = 0;
	private static int DISPLAY_HEIGHT = 0;

	private MapView mMapView;

	// Serializer to write fingerprints to a file
	private Serializer mSerializer;

	private String mapId;
	private int gridSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fingerprinting);

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		DISPLAY_WIDTH = display.getWidth();
		DISPLAY_HEIGHT = display.getHeight();

		// Get the singleton instance of the serializer
		mSerializer = Serializer.getInstance();

		mapId = "full";
		gridSize = 60;

		mMapView = new MapView(this);

		mMapView.setMap(mapId);
		mMapView.setGridSize(gridSize);

		// SampleView constructor must be constructed last as it needs the
		// displayWidth and displayHeight we just got.
		setContentView(mMapView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_fingerprinting, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			intent.putExtra("mapId", mapId);
			intent.putExtra("gridSize", gridSize);

			startActivityForResult(intent, 0);
			break;
		}
		return false;
	}

	/**
	 * Executed when we return from the settings activity. Sets the new settings
	 * that were configured there.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == SETTINGS_CODE) {

			Bundle extras = data.getExtras();

			mapId = extras.getString("mapId");
			gridSize = extras.getInt("gridSize");

			mMapView.setMap(mapId);
			mMapView.setGridSize(gridSize);
		}
	}

	/**
	 * First gets the wifi manager system service and start to scan for access
	 * points. When the scan is completed an asynchonous message will be sent.
	 * When this is done, the found results will be prepared in order for them
	 * to be displayed in a listview.
	 */
	public void initializeWifiScan(float startX, float startY) {
		this.lastRect = mMapView.getRectForPos((int) startX, (int) startY);

		mProgressDialog = ProgressDialog.show(this, "Wifi Scan",
				"Scanning ...", true);

		mIntentFilter = new IntentFilter();

		mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		mBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("WLAN", "Receiving WLAN Scan results");

				mScanResults = (ArrayList<ScanResult>) mWifiManager
						.getScanResults();

				mProgressDialog.dismiss();

				saveResults.run();
			};
		};

		registerReceiver(mBroadcastReceiver, mIntentFilter);

		scanWifi.run();
	}

	/**
	 * Start the WIFI scan
	 */
	private Runnable scanWifi = new Runnable() {

		public void run() {
			mWifiManager.startScan();
		}
	};

	/**
	 * Save the results from the scan to an XML file
	 */
	private Runnable saveResults = new Runnable() {

		public void run() {

			unregisterReceiver(mBroadcastReceiver);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
			Date date = new Date();
			String dateString = sdf.format(new Date());

			// long curr_time = System.currentTimeMillis();
			String FILENAME = "WifiScan_" + mapId + "_" + dateString;

			GridMap map = new GridMap(mapId, "", gridSize);

			Position position = new GridPosition(map, lastRect.left,
					lastRect.top, lastRect.right, lastRect.bottom);

			Map<AccessPoint, AccessPointPowerLevels> apLevels = new HashMap<AccessPoint, AccessPointPowerLevels>();

			Log.d("DEBUG", "Scan results size = " + mScanResults.size());

			for (ScanResult result : mScanResults) {

				AccessPoint ap = new AccessPoint(result.BSSID, result.SSID,
						result.frequency, result.capabilities);

				List<Double> levels = new ArrayList<Double>();

				levels.add((double) result.level);

				AccessPointPowerLevels apPLevels = new AccessPointPowerLevels(
						levels);

				apLevels.put(ap, apPLevels);
			}

			Log.d("DEBUG", "Ap levels size = " + apLevels.size());

			WLANMeasurement measurement = new WLANMeasurement(apLevels);

			WLANFingerprint fingerprint = new WLANFingerprint(position,
					measurement);

			BufferedWriter writer = null;

			try {
				// SAVING TO EXTERNAL SD

				File directory = Environment.getExternalStorageDirectory();
				FileWriter out = new FileWriter(new File(directory,
						"/ips_samples/" + FILENAME + ".xml"));
				org.simpleframework.xml.Serializer sz = new Persister();
				// sz.write("something", out);
				sz.write(fingerprint, out);

				out.close();

				Toast.makeText(getApplicationContext(),
						"File succesfully saved", Toast.LENGTH_SHORT).show();
				Log.d("WLAN", "Saved file " + FILENAME + " to " + getFilesDir());

			} catch (FileNotFoundException e) {
				Log.w("ExternalStorage", "File not found", e);

				Toast.makeText(getApplicationContext(), "File not found",
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				// Unable to create file, likely because external storage is
				// not currently mounted.
				Log.w("ExternalStorage", "Error writing file", e);

				Toast.makeText(getApplicationContext(),
						"Error while writing file (IOException)",
						Toast.LENGTH_SHORT).show();

				Log.d("DEBUG", e.getStackTrace().toString());
			} catch (Exception e) {
				Toast.makeText(
						getApplicationContext(),
						"Error while writing file (Simple-xml exception) - "
								+ e.getClass().getName(), Toast.LENGTH_SHORT)
						.show();
				Log.d("DEBUG", ((TransformException) e).getMessage());
			} finally {

				if (writer != null) {
					try {
						writer.close();

						Toast.makeText(getApplicationContext(),
								"File succesfully saved", Toast.LENGTH_SHORT)
								.show();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					Log.d("DEBUG", "Write is null");
				}

			}

			mProgressDialog.dismiss();
		}
	};

	/**
	 * 
	 * @author Wouter Van Rossem
	 * 
	 */
	private static class MapView extends View {
		private static Bitmap bmLargeImage; // bitmap large enough to be
											// scrolled
		private static Rect displayRect = null; // rect we display to
		private Rect scrollRect = null; // rect we scroll over our bitmap with
		/*
		 * private int scrollRectX = 0; //current left location of scroll rect
		 * private int scrollRectY = 0; //current top location of scroll rect
		 * private float scrollByX = 0; //x amount to scroll by private float
		 * scrollByY = 0; //y amount to scroll by
		 */
		private float startX = 0; // track x from one ACTION_MOVE to the next
		private float startY = 0; // track y from one ACTION_MOVE to the next

		private Context ctx;
		private Paint paint;

		// Constants and variables for constructing the grid
		int COLUMNS = 6;
		int ROWS = 6;
		int HEIGHT, WIDTH;

		List<Rect> rects;

		int gridSize;

		public MapView(Context context) {
			super(context);

			ctx = context;

			// Destination rect for our main canvas draw. It never changes.
			displayRect = new Rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
			// Scroll rect: this will be used to 'scroll around' over the
			// bitmap in memory. Initialize as above.
			scrollRect = new Rect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);

			// Load a large bitmap into an offscreen area of memory.
			bmLargeImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.plan_brusselzuid_5_n);

			paint = new Paint();

			WindowManager wm = (WindowManager) ctx
					.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();

			HEIGHT = display.getWidth();
			WIDTH = display.getHeight();

			rects = new ArrayList<Rect>();

			gridSize = 60;
		}

		public void setGridSize(int gridSize) {
			COLUMNS = gridSize;
			ROWS = gridSize;

			this.gridSize = gridSize;

			invalidate();
		}

		/**
		 * Receives touch events when the user wants to start a Wi-Fi scan.
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {

			Log.d("DEBUG", "Received on touch event 1 - " + startX + ", "
					+ startY);

			switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				// Remember our initial down event location.
				startX = event.getRawX();
				startY = event.getRawY();
				break;

			case MotionEvent.ACTION_UP:

				((Fingerprinting) ctx).initializeWifiScan(startX, startY);

			case MotionEvent.ACTION_MOVE:
				float x = event.getRawX();
				float y = event.getRawY();

				startX = x; // reset initial values to latest
				startY = y;
				invalidate(); // force a redraw
				break;
			}

			return true; // done with this event so consume it
		}

		/**
		 * Draw the map image and the grid overlay
		 */
		@Override
		protected void onDraw(Canvas canvas) {

			canvas.drawBitmap(bmLargeImage, scrollRect, displayRect, paint);

			drawGrid(canvas, gridSize, gridSize);
		}

		public void setMap(String mapId) {

			if (mapId.equals("full")) {
				bmLargeImage = BitmapFactory.decodeResource(getResources(),
						R.drawable.full);
			} else if (mapId.equals("medium1")) {
				bmLargeImage = BitmapFactory.decodeResource(getResources(),
						R.drawable.medium1);
			}
			if (mapId.equals("medium2")) {
				bmLargeImage = BitmapFactory.decodeResource(getResources(),
						R.drawable.medium2);
			}
			if (mapId.equals("small1")) {
				bmLargeImage = BitmapFactory.decodeResource(getResources(),
						R.drawable.small1);
			}
			if (mapId.equals("small2")) {
				bmLargeImage = BitmapFactory.decodeResource(getResources(),
						R.drawable.small2);
			}

			this.invalidate();
		}

		/**
		 * 
		 * Find the rectangle that contains the given point.
		 * 
		 * @param x
		 *            X-coordinate of the point
		 * @param y
		 *            Y-coordinate of the point
		 * @return The rectangle that contains the point
		 */
		public Rect getRectForPos(int x, int y) {
			// Loop over all the rectangles
			for (Rect rect : rects) {
				// Check if it contains the point
				if (rect.contains(x, y)) {
					return rect;
				}
			}

			return null;
		}

		/**
		 * Draw a grid with a number of rows and columns
		 * 
		 * @param canvas
		 */
		private void drawGridAmount(Canvas canvas, int rows, int columns) {

			HEIGHT = canvas.getHeight();
			WIDTH = canvas.getWidth();

			int stepRow = HEIGHT / rows;
			int stepCol = WIDTH / columns;

			drawGrid(canvas, stepRow, stepCol);
		}

		/**
		 * Draws a grid of X rows and Y columns each of a fixed height and width
		 * 
		 * @param canvas
		 */
		private void drawGrid(Canvas canvas, int stepRow, int stepCol) {

			// Some initializations
			HEIGHT = canvas.getHeight();
			WIDTH = canvas.getWidth();

			ROWS = HEIGHT / stepRow;
			COLUMNS = WIDTH / stepCol;

			int startX = 0;
			int startY = 0;

			int left = startX;
			int top = startY;
			int right = stepCol;
			int bottom = stepRow;

			// Set rectangle color to black and straight lines
			// Paint paint = new Paint();
			// paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.BLACK);
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(3);

			// For each row, draw the columns
			for (int i = 0; i < ROWS; i++) {
				// Draw a row of rectangles
				for (int j = 0; j < COLUMNS; j++) {

					Rect rect = new Rect(left, top, right, bottom);
					// Store the rectangle to use later with touch events
					rects.add(rect);

					canvas.drawRect(rect, paint);

					left += stepCol;
					right += stepCol;
				}

				left = startX;
				right = stepCol;
				top += stepRow;
				bottom += stepRow;
			}
		}
	}
}
