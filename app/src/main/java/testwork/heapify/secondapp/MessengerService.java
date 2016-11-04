package testwork.heapify.secondapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.Display;

import java.util.Date;

public class MessengerService extends Service {
	private static final String TAG = "MessengerService";

	private static final int ALARM_INTERVAL = 1000 * 60 * 2;

	private static final int MESSAGE_INFO = 1;

	private static final String EXTRA_PID = "PID";
	private static final String EXTRA_SCREEN_STATE = "SCREEN_STATE";

	private static final String FORMAT_RECEIVE =
			"SecondApp:: Received info at <%s> from process <%d>, WiFiState=<WiFiState(%d)>";
	private static final String FORMAT_SEND =
			"SecondApp:: Sent info at <%s>, ScreenState=<ScreenState(%d)>";

	private static final String ACTION_SEND_INFORMATION =
			"testwork.heapify.secondapp.SEND_INFORMATION";

	private Handler repeatingHandler;

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MESSAGE_INFO:
					Date date = new Date();
					Log.d(TAG, String.format(FORMAT_RECEIVE, date.toString(), msg.arg1, msg.arg2));
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public IBinder onBind(Intent intent) {
		setScheduler();

		return mMessenger.getBinder();
	}

	private void setScheduler() {
		repeatingHandler = new Handler();
		repeatingHandler.postDelayed(sendInfoTask, ALARM_INTERVAL);

	}

	private Runnable sendInfoTask = new Runnable() {
		@Override
		public void run() {
			int pId = Process.myPid();
			int isScreenOn = isScreenOn() ? 1 : 0;

			Intent intent = new Intent(ACTION_SEND_INFORMATION);
			intent.putExtra(EXTRA_PID, pId);
			intent.putExtra(EXTRA_SCREEN_STATE, isScreenOn);

			sendBroadcast(intent);

			Log.d(TAG, String.format(FORMAT_SEND, new Date().toString(), isScreenOn));

			repeatingHandler.postDelayed(this, ALARM_INTERVAL);
		}
	};

	private boolean isScreenOn() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
			DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
			boolean screenOn = false;
			for (Display display : dm.getDisplays()) {
				if (display.getState() != Display.STATE_OFF) {
					screenOn = true;
				}
			}
			return screenOn;
		} else {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			//noinspection deprecation
			return pm.isScreenOn();
		}
	}

	@Override
	public void onDestroy() {
		if (repeatingHandler != null) {
			repeatingHandler.removeCallbacks(sendInfoTask);
			repeatingHandler = null;
		}
	}
}
