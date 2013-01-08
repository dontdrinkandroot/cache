package net.dontdrinkandroot.cache.utils;

public class Duration {

	public static final long SECONDS_PER_MINUTE = 60;

	public static final long MINUTES_PER_HOUR = 60;

	public static final long HOURS_PER_DAY = 60;

	public static final long MILLIS_PER_SECOND = 1000;

	public static final long MILLIS_PER_MINUTE = Duration.MILLIS_PER_SECOND * Duration.SECONDS_PER_MINUTE;

	public static final long MILLIS_PER_HOUR = Duration.MINUTES_PER_HOUR * Duration.MILLIS_PER_MINUTE;

	public static final long MILLIS_PER_DAY = Duration.MILLIS_PER_HOUR * Duration.HOURS_PER_DAY;


	public static long seconds(int i) {

		return Duration.MILLIS_PER_SECOND * i;
	}


	public static long minutes(int i) {

		return Duration.MILLIS_PER_MINUTE * i;
	}


	public static long days(int i) {

		return Duration.MILLIS_PER_DAY * i;
	}

}
