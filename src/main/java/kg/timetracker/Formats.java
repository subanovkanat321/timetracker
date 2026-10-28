package kg.timetracker;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** Как время и длительность выглядят для пользователя. Один формат на всё приложение. */
public final class Formats {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
		.withZone(TimetrackerApplication.ZONE);

	private Formats() {
	}

	/** Момент по времени Бишкека: «28.10.2026 14:05». */
	public static String dateTime(Instant instant) {
		return DATE_TIME.format(instant);
	}

	/**
	 * «7 ч 05 мин 12 с». С секундами: тогда сумма строк отчёта всегда равна итогу,
	 * а короткая запись не выглядит пустой. Доли секунды отбрасываются.
	 */
	public static String duration(Duration duration) {
		long seconds = duration.toSeconds();
		return "%d ч %02d мин %02d с".formatted(seconds / 3600, seconds / 60 % 60, seconds % 60);
	}

}
