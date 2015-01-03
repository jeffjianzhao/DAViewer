package daviewer;

import java.awt.event.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.Timer;

public class AutoLogger implements ActionListener {
	
	private Timer logtimer = new Timer(2 * 60 *1000, this);
	private PrintWriter output;
	private Date startTime;
	private boolean isLogging;
	private boolean isAutosaving;
	public static DAViewer appFrame;
	
	public AutoLogger(boolean enableLog, boolean enableSave) {
		logtimer.setRepeats(true);
		isLogging = enableLog;
		isAutosaving = enableSave;
	}
	
	public void start(){
		logtimer.start();
		if (isLogging) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
			try {
				startTime = Calendar.getInstance().getTime();
				output = new PrintWriter(new File(dateFormat.format(startTime) + ".csv"));
				logAction("logging starts");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop(){
		logtimer.stop();
		if (isLogging) {
			logAction("logging ends");
			output.close();
		}
	}
	
	public void logAction(String msg) {
		if (!isLogging)
			return;
		Date nowTime = Calendar.getInstance().getTime();
		long seconds = (nowTime.getTime() - startTime.getTime()) / 1000;
		output.println(seconds + "," + msg);
	}
	
	public void actionPerformed(ActionEvent arg0) {
		if(isAutosaving) {
			try {
				appFrame.overview.saveTreeTableModel(new File("autosave.tree.wksp"));
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}

}
