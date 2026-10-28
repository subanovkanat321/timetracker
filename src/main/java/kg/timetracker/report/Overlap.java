package kg.timetracker.report;

import java.time.Duration;
import java.time.Instant;

/** Сколько времени записи приходится на период. Чистая функция — без базы и без часов. */
final class Overlap {

	private Overlap() {
	}

	/**
	 * @param startedAt   начало записи
	 * @param endedAt     конец записи; {@code null} — таймер идёт, считается до {@code now}
	 * @param periodStart начало периода (включительно)
	 * @param periodEnd   конец периода (не включительно)
	 * @return часть записи внутри периода; {@link Duration#ZERO}, если они не пересекаются
	 */
	static Duration of(Instant startedAt, Instant endedAt, Instant periodStart, Instant periodEnd, Instant now) {
		Instant end = endedAt != null ? endedAt : now;
		Instant from = max(startedAt, periodStart);
		Instant to = min(end, periodEnd);
		return from.isBefore(to) ? Duration.between(from, to) : Duration.ZERO;
	}

	private static Instant max(Instant a, Instant b) {
		return a.isAfter(b) ? a : b;
	}

	private static Instant min(Instant a, Instant b) {
		return a.isBefore(b) ? a : b;
	}

}
