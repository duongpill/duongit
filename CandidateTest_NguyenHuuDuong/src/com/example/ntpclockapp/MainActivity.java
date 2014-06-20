package com.example.ntpclockapp;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.impl.cookie.DateUtils;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AnalogClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private int DEM_THOAT = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		private long nowAsPerDeviceTimeZone = 0;
		private AnalogClock analogClock;
		private TextView textDate, textAlso;
		private Button btnSync;
		private boolean running = false;
		private SntpClient sntpClient;
		private int TIME_STOP = 600000;
		private CountDownTimer countDownTimer;
		private Thread thread;
		private Timer timer;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			getControllers(rootView);

			getBtnSyncListener();
			return rootView;
		}

		@Override
		public void onResume() {
			super.onResume();
			sntpClient = new SntpClient();
			running = true;
			runThread();
		}

		private void getControllers(View view) {
			analogClock = (AnalogClock) view.findViewById(R.id.analogClock);
			textDate = (TextView) view.findViewById(R.id.textDate);
			textAlso = (TextView) view.findViewById(R.id.textAlso);
			btnSync = (Button) view.findViewById(R.id.btnSync);

			Typeface typeface = Typeface.createFromAsset(getActivity()
					.getAssets(), "fonts/digital-7.ttf");
			textDate.setTypeface(typeface);
		}

		private void runThread() {
			countDownToNTP();
			timer = new Timer();
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					Looper.prepare();
					while (running) {
						new ReceiverDateTimeNow()
								.execute("0.africa.pool.ntp.org");
						countDownTimer.start();
						try {
							Thread.sleep(TIME_STOP);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
			thread.start();
		}

		private void countDownToNTP() {
			countDownTimer = new CountDownTimer(TIME_STOP, 1000) {
				@Override
				public void onTick(long millisUntilFinished) {
					long sFinish = millisUntilFinished / 1000;
					long minutes = (sFinish % 3600) / 60;
					long seconds = (sFinish % 60);
					String times = null;
					if (minutes >= 0)
						times = "Also " + minutes + " minutes " + seconds
								+ " seconds ";
					textAlso.setText(times);
					textDate.setText(DateUtils.formatDate(new Date(
							nowAsPerDeviceTimeZone)));
					nowAsPerDeviceTimeZone += 1000;
				}

				@Override
				public void onFinish() {
				}
			};
		}

		private void getBtnSyncListener() {
			btnSync.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (thread.isAlive())
						thread.interrupt();

					if (countDownTimer != null)
						countDownTimer.cancel();
					running = true;
					btnSync.setEnabled(false);
					setTimerAutoEnableButton();
				}
			});
		}

		private void setTimerAutoEnableButton() {
			timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					mHandler.obtainMessage(1).sendToTarget();
				}
			}, 3000);
		}

		@SuppressLint("HandlerLeak")
		public Handler mHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				btnSync.setEnabled(true);
			};
		};

		class ReceiverDateTimeNow extends AsyncTask<String, Void, String> {
			@Override
			protected String doInBackground(String... params) {
				if (sntpClient.requestTime(params[0], 120000)) {
					nowAsPerDeviceTimeZone = sntpClient.getNtpTime();
					Calendar cal = Calendar.getInstance();
					TimeZone timeZoneInDevice = cal.getTimeZone();
					int differentialOfTimeZones = timeZoneInDevice
							.getOffset(System.currentTimeMillis());
					nowAsPerDeviceTimeZone += differentialOfTimeZones;
				}
				return DateUtils.formatDate(new Date(nowAsPerDeviceTimeZone));
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
		}

		@Override
		public void onPause() {
			super.onPause();
			if (thread.isAlive())
				thread.interrupt();

			if (countDownTimer != null)
				countDownTimer.cancel();
			running = false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			Toast.makeText(this, "Ấn lại để thoát", Toast.LENGTH_LONG).show();

			DEM_THOAT++;

			if (DEM_THOAT >= 2) {
				moveTaskToBack(true);
				android.os.Process.killProcess(android.os.Process.myPid());
			}

			Timer timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					DEM_THOAT = 0;
				}
			}, 3000);
		}
		return false;
	}
}
