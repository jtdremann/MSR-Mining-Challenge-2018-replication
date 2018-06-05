package miner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import cc.kave.commons.model.events.ActivityEvent;
import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.model.events.testrunevents.TestCaseResult;
import cc.kave.commons.model.events.testrunevents.TestResult;
import cc.kave.commons.model.events.testrunevents.TestRunEvent;
import cc.kave.commons.model.events.visualstudio.BuildEvent;
import cc.kave.commons.model.events.visualstudio.BuildTarget;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import examples.IoHelper;

/**
 * Created through heavy modification of the KaVE project's GettingStarted.java example
 * found at https://github.com/kave-cc/java-cc-kave-examples/blob/master/src/main/java/examples/GettingStarted.java
 * 
 * This class attempts to replicate the findings of the study by Ariel Rodriguez, Fumiya Tanaka, and Yasutaka Kamei,
 * "Empirical Study on the Relationship Between Developer’s Working Habits and Efficiency"
 * 
 * This class depends on the IDE interaction dataset that can be downloaded from the KaVE project at
 * http://www.kave.cc/datasets
 * 
 * @author Jacob Dremann
 *
 */
public class WorkingHabitsMiner {
	
	private LocalDateTime start;
	private LocalDateTime stop;
	private PrintWriter dayOfWeekPw;
	private PrintWriter continuousPw;
	private String dir;

	private int userId = -1;
	private LocalDateTime activityCutoff = null;
	private LocalDateTime prevDate = null;
	// Variables for tracking statistics based on day of week. Prefixed with dow
	private String dowCurrentSessionId = null;
	private LocalDateTime dowFirstSessionActivityEventDate = null;
	private long dowWorkingSeconds = 0;
	private long dowContinuousSeconds = 0;
	private int dowTotalBuildEvents = 0;
	private int dowTotalBuildTargets = 0;
	private int dowBuildTargetSuccesses = 0;
	private int dowBuildTargetFails = 0;
	private int dowTotalTestRunEvents = 0;
	private int dowTotalTestCaseResults = 0;
	private int dowTestCaseSuccesses = 0;
	private int dowTestCaseFails = 0;
	private int dowTestCaseErrors = 0;
	private int dowTestCaseIgnoreds = 0;
	private int dowTestCaseUnknowns = 0;
	
	// Variables for tracking continuous working time statistics. Prefixed with c
	private long cContinuousSeconds = 0;
	private int cTotalBuildEvents = 0;
	private int cTotalBuildTargets = 0;
	private int cBuildTargetSuccesses = 0;
	private int cBuildTargetFails = 0;
	private int cTotalTestRunEvents = 0;
	private int cTotalTestCaseResults = 0;
	private int cTestCaseSuccesses = 0;
	private int cTestCaseFails = 0;
	private int cTestCaseErrors = 0;
	private int cTestCaseIgnoreds = 0;
	private int cTestCaseUnknowns = 0;
	
	public WorkingHabitsMiner(String dir){
		this.dir = dir;
	}
	
	public void run() throws FileNotFoundException, UnsupportedEncodingException {
		start = LocalDateTime.now();
		
		// Write .csv header rows
		dayOfWeekPw = new PrintWriter("day-of-week-output.csv", "UTF-8");
		dayOfWeekPw.println("userId,date,dayOfWeek,workingTime,continuousTime,totalBuildEvents,totalBuildTargets,buildTargetSuccesses"
				+ ",buildTargetFails,totalTestRunEvents,totalTestCaseResults,testCaseSuccesses,testCaseFails,testCaseErrors"
				+ ",testCaseIgnoreds,testCaseUnknowns");
		dayOfWeekPw.flush();
		
		continuousPw = new PrintWriter("continuous-output.csv", "UTF-8");
		continuousPw.println("userId,date,dayOfWeek,continuousTime,totalBuildEvents,totalBuildTargets,buildTargetSuccesses"
				+ ",buildTargetFails,totalTestRunEvents,totalTestCaseResults,testCaseSuccesses,testCaseFails,testCaseErrors"
				+ ",testCaseIgnoreds,testCaseUnknowns");
		continuousPw.flush();
		
		userId = 0;
		/*
		 * Each .zip that is contained in the eventsDir represents all events that we
		 * have collected for a specific user, the folder represents the first day when
		 * the user uploaded data.
		 */
		try {
			Set<String> userZips = IoHelper.findAllZips(dir);
	
			for (String userZip : userZips) {
				prevDate = null;
				activityCutoff = null;
				
				userId++;
				System.out.printf("\n#### processing user zip: %s #####\n", userZip);
				processUserZip(userZip);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			dayOfWeekPw.close();
		}
		
		stop = LocalDateTime.now();
		
		System.out.println("Began at " + start.toString());
		System.out.println("Ended at " + stop.toString());
	}

	private void processUserZip(String userZip) {
		// open the .zip file ...
		try (IReadingArchive ra = new ReadingArchive(new File(dir, userZip))) {
			// ... and iterate over content.
			while (ra.hasNext()) {
				/*
				 * within the userZip, each stored event is contained as a single file that
				 * contains the Json representation of a subclass of IDEEvent.
				 */
				IDEEvent e = ra.getNext(IDEEvent.class);
				processEvent(e);
			}
			
			if(prevDate != null) {
				addDayOfWeekRow(prevDate);
				addContinuousRow(prevDate);
			}
		}
	}

	/*
	 * Choose the correct type-cast of the event, and process it accordingly
	 */
	private void processEvent(IDEEvent e) {

		if (e instanceof ActivityEvent) {
			process((ActivityEvent) e);
		} else if (e instanceof BuildEvent) {
			process((BuildEvent) e);
		} else if (e instanceof TestRunEvent) {
			process((TestRunEvent) e);
		}/* else {
			System.out.println(e.getClass().getName());
		}*/

	}
	
	private void process(ActivityEvent e) {
		LocalDateTime date = e.TriggeredAt.toLocalDateTime();
		String sessionId = e.IDESessionUUID;
		//System.out.printf("found an ActivityEvent\n");
		
		if(dowCurrentSessionId == null) {
			dowCurrentSessionId = sessionId;
			dowFirstSessionActivityEventDate = date;
		}
		else if(!dowCurrentSessionId.equals(sessionId)) {
			long deltaSeconds = date.toEpochSecond(ZoneOffset.UTC) - dowFirstSessionActivityEventDate.toEpochSecond(ZoneOffset.UTC);
			dowWorkingSeconds += deltaSeconds;

			dowCurrentSessionId = sessionId;
			dowFirstSessionActivityEventDate = date;
		}
		
		// if prevDate exists
		if(prevDate != null) {
			// According to KaVE project, data is sorted by date and this should not run
			// Tested on first 100k events. exception was not thrown.
			/*if(date.isBefore(prevDate)) {
				throw new IllegalArgumentException("next date is before the previous!");
			}*/
			
			// DAY OF WEEK
			// If same day, calculate statistics for same day
			if(prevDate.getDayOfMonth() == date.getDayOfMonth()) {
				if(date.isBefore(activityCutoff)) {
					long deltaSeconds = date.toEpochSecond(ZoneOffset.UTC) - prevDate.toEpochSecond(ZoneOffset.UTC);
					
					dowContinuousSeconds += deltaSeconds;
				}
			}
			else {
				// TODO if date is before activityCutoff, they were working through midnight
				// I.E. add midnight - prevDate, save and reset continuous time,
				// then add date - midnight to continuous time
				addDayOfWeekRow(prevDate);
			}
			
			// CONTINUOUS TIME
			if(date.isBefore(activityCutoff)) {
				long deltaSeconds = date.toEpochSecond(ZoneOffset.UTC) - prevDate.toEpochSecond(ZoneOffset.UTC);
				cContinuousSeconds += deltaSeconds;
			}
			else {
				addContinuousRow(prevDate);
			}//*/
			
			// Total between the two different data set might differ; if the user continuously works across midnight,
			// continuous time gets cut off for day-of-week-output.csv, but not continuous-output.csv
		}
		
		prevDate = date;
		activityCutoff = date.plusMinutes(5);
	}
	
	private void addDayOfWeekRow(LocalDateTime date) {
		String dateStr = prevDate.getYear() + "-" + prevDate.getMonthValue() + "-" + prevDate.getDayOfMonth();
		String dayOfWeek = date.getDayOfWeek().name();
		System.out.println("Adding day for date: " + dateStr);
		
		// Not sure how working seconds is calculated...
		// temporarily set to zero so we can get working data.
		long tempWorkingSeconds = 0;
		
		// Write row to file
		String line = userId + "," + dateStr + "," + dayOfWeek + "," + tempWorkingSeconds + "," + dowContinuousSeconds + "," +
				dowTotalBuildEvents + "," + dowTotalBuildTargets + "," + dowBuildTargetSuccesses + "," + dowBuildTargetFails +
				"," + dowTotalTestRunEvents + "," + dowTotalTestCaseResults + "," + dowTestCaseSuccesses + "," + dowTestCaseFails +
				"," + dowTestCaseErrors + "," + dowTestCaseIgnoreds + "," + dowTestCaseUnknowns;
		dayOfWeekPw.println(line);
		dayOfWeekPw.flush();
		
		dowWorkingSeconds = 0;
		dowContinuousSeconds = 0;
		dowTotalBuildEvents = 0;
		dowTotalBuildTargets = 0;
		dowBuildTargetSuccesses = 0;
		dowBuildTargetFails = 0;
		dowTotalTestRunEvents = 0;
		dowTotalTestCaseResults = 0;
		dowTestCaseSuccesses = 0;
		dowTestCaseFails = 0;
		dowTestCaseErrors = 0;
		dowTestCaseIgnoreds = 0;
		dowTestCaseUnknowns = 0;
	}
	
	private void addContinuousRow(LocalDateTime date) {
		String dateStr = prevDate.getYear() + "-" + prevDate.getMonthValue() + "-" + prevDate.getDayOfMonth();
		String dayOfWeek = date.getDayOfWeek().name();
		System.out.println("Adding continuouos row for date: " + dateStr);
		
		// Write row to file
		String line = userId + "," + dateStr + "," + dayOfWeek + "," + cContinuousSeconds + "," +
				cTotalBuildEvents + "," + cTotalBuildTargets + "," + cBuildTargetSuccesses + "," + cBuildTargetFails +
				"," + cTotalTestRunEvents + "," + cTotalTestCaseResults + "," + cTestCaseSuccesses + "," + cTestCaseFails +
				"," + cTestCaseErrors + "," + cTestCaseIgnoreds + "," + cTestCaseUnknowns;
		continuousPw.println(line);
		continuousPw.flush();
		
		cContinuousSeconds = 0;
		cTotalBuildEvents = 0;
		cTotalBuildTargets = 0;
		cBuildTargetSuccesses = 0;
		cBuildTargetFails = 0;
		cTotalTestRunEvents = 0;
		cTotalTestCaseResults = 0;
		cTestCaseSuccesses = 0;
		cTestCaseFails = 0;
		cTestCaseErrors = 0;
		cTestCaseIgnoreds = 0;
		cTestCaseUnknowns = 0;
	}

	private void process(BuildEvent e) {
		//System.out.printf("found a BuildEvent\n");
		dowTotalBuildEvents++;
		cTotalBuildEvents++;
		
		List<BuildTarget> targets = e.Targets;
		dowTotalBuildTargets += targets.size();
		cTotalBuildTargets += targets.size();
		for(BuildTarget t : targets) {
			if(t.Successful) {
				dowBuildTargetSuccesses++;
				cBuildTargetSuccesses++;
			}
			else {
				dowBuildTargetFails++;
				cBuildTargetFails++;
			}
		}
	}
	
	private void process(TestRunEvent e) {
		//System.out.printf("found a TestRunEvent");
		dowTotalTestRunEvents++;
		cTotalTestRunEvents++;
		
		Set<TestCaseResult> tests = e.Tests;
		dowTotalTestCaseResults += tests.size();
		cTotalTestCaseResults += tests.size();
		for(TestCaseResult t : tests) {
			if(t.Result == TestResult.Success) {
				dowTestCaseSuccesses++;
				cTestCaseSuccesses++;
			}
			else if(t.Result == TestResult.Failed) {
				dowTestCaseFails++;
				cTestCaseFails++;
			}
			else if(t.Result == TestResult.Error) {
				dowTestCaseErrors++;
				cTestCaseErrors++;
			}
			else if(t.Result == TestResult.Ignored) {
				dowTestCaseIgnoreds++;
				cTestCaseIgnoreds++;
			}
			else if(t.Result == TestResult.Unknown) {
				dowTestCaseUnknowns++;
				cTestCaseUnknowns++;
			}
		}
	}
}
