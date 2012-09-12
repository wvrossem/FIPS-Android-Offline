package ips.android.offline;

import ips.android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * 
 * The settings activity is used to configure the figerprinting such as what map
 * to use and how big the grid sizes should be
 * 
 * @author Wouter Van Rossem
 * 
 */
public class SettingsActivity extends Activity implements
		OnItemSelectedListener {

	String mapId;

	int gridSize;

	EditText textField;

	Intent newIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.Maps, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);

		Bundle extras = getIntent().getExtras();

		mapId = extras.getString("mapId");
		gridSize = extras.getInt("gridSize");

		textField = (EditText) findViewById(R.id.editText1);

		textField.setText(gridSize + "", TextView.BufferType.EDITABLE);

		newIntent = new Intent();
	}

	@Override
	public void onBackPressed() {

		saveSettings();

		super.onBackPressed();
	}

	@Override
	protected void onStop() {

		saveSettings();

		super.onStop();
	}

	private void saveSettings() {

		gridSize = Integer.parseInt(textField.getText().toString());

		newIntent.putExtra("mapId", mapId);
		newIntent.putExtra("gridSize", gridSize);

		setResult(RESULT_OK, newIntent);

		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		String mapName = (String) parent.getItemAtPosition(pos);

		if (mapName.equals("Full map")) {
			mapId = "full";
		} else if (mapName.equals("Medium map 1")) {
			mapId = "medium1";
		} else if (mapName.equals("Medium map 2")) {
			mapId = "medium2";
		} else if (mapName.equals("Small map 1")) {
			mapId = "small1";
		} else if (mapName.equals("Small map 2")) {
			mapId = "small2";
		}

		newIntent.putExtra("mapId", mapId);
		setResult(RESULT_OK, newIntent);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

}
