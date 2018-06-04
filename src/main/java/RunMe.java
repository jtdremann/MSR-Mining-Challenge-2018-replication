
/**
 * Copyright 2016 University of Zurich
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import miner.WorkingHabitsMiner;

/**
 * Slight modification of the original KaVE Project's example RunMe.java found at
 * https://github.com/kave-cc/java-cc-kave-examples/blob/master/src/main/java/RunMe.java
 */
public class RunMe {

	/*
	 * download the interaction data and unzip it into the root of this project (at
	 * the level of the pom.xml). Unpack it, you should now have a folder that
	 * includes a bunch of folders that have dates as names and that contain .zip
	 * files.
	 */
	public static String eventsDir = "Events-170301-2";

	public static void main(String[] args) {
		try {
			new WorkingHabitsMiner(eventsDir).run();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
};