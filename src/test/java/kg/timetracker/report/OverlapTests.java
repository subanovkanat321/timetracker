package kg.timetracker.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OverlapTests {

	// Период: 10:00–12:00, «сейчас» — 11:00
	private static final Instant PERIOD_START = Instant.parse("2026-10-27T10:00:00Z");

	private static final Instant PERIOD_END = Instant.parse("2026-10-27T12:00:00Z");

	private static final Instant NOW = Instant.parse("2026-10-27T11:00:00Z");

	@ParameterizedTest(name = "{0}")
	@CsvSource(nullValues = "null", textBlock = """
			# случай,                     начало, конец,  минут в периоде
			целиком внутри,               10:30,  11:00,  30
			начата до периода,            09:00,  10:15,  15
			закончена после периода,      11:30,  13:00,  30
			накрывает весь период,        09:00,  13:00,  120
			целиком до периода,           08:00,  09:00,  0
			целиком после периода,        12:30,  13:00,  0
			кончилась ровно в начале,     09:00,  10:00,  0
			нулевой длины,                10:30,  10:30,  0
			идущая — считается до now,    10:20,  null,   40
			идущая с начала до периода,   09:00,  null,   60
			""")
	void minutesInsidePeriod(String description, String start, String end, long expectedMinutes) {
		Duration overlap = Overlap.of(at(start), end == null ? null : at(end), PERIOD_START, PERIOD_END, NOW);

		assertThat(overlap).isEqualTo(Duration.ofMinutes(expectedMinutes));
	}

	@ParameterizedTest(name = "период в будущем, now = {0}")
	@CsvSource({ "09:00", "10:00" })
	void runningTimerGivesZeroBeforePeriodStarts(String now) {
		Duration overlap = Overlap.of(at("08:00"), null, PERIOD_START, PERIOD_END, at(now));

		assertThat(overlap).isZero();
	}

	private static Instant at(String time) {
		return Instant.parse("2026-10-27T" + time + ":00Z");
	}

}
