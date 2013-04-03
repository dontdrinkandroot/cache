/**
 * Copyright (C) 2012, 2013 Philip W. Sorst <philip@sorst.net>
 * and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dontdrinkandroot.cache.utils;

public class Duration {

	public static final long SECONDS_PER_MINUTE = 60;

	public static final long MINUTES_PER_HOUR = 60;

	public static final long HOURS_PER_DAY = 60;

	public static final long MILLIS_PER_SECOND = 1000;

	public static final long MILLIS_PER_MINUTE = Duration.MILLIS_PER_SECOND * Duration.SECONDS_PER_MINUTE;

	public static final long MILLIS_PER_HOUR = Duration.MINUTES_PER_HOUR * Duration.MILLIS_PER_MINUTE;

	public static final long MILLIS_PER_DAY = Duration.MILLIS_PER_HOUR * Duration.HOURS_PER_DAY;


	public static long seconds(int seconds) {

		return Duration.MILLIS_PER_SECOND * seconds;
	}


	public static long minutes(int minutes) {

		return Duration.MILLIS_PER_MINUTE * minutes;
	}


	public static long hours(int hours) {

		return Duration.MILLIS_PER_HOUR * hours;
	}


	public static long days(int days) {

		return Duration.MILLIS_PER_DAY * days;
	}

}
