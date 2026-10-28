package kg.timetracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class FormatsTests {

	@Test
	void durationShowsHoursMinutesSeconds() {
		assertThat(Formats.duration(Duration.ZERO)).isEqualTo("0 ч 00 мин 00 с");
		assertThat(Formats.duration(Duration.ofSeconds(40))).isEqualTo("0 ч 00 мин 40 с");
		assertThat(Formats.duration(Duration.ofHours(7).plusMinutes(5).plusSeconds(12))).isEqualTo("7 ч 05 мин 12 с");
		assertThat(Formats.duration(Duration.ofHours(30))).isEqualTo("30 ч 00 мин 00 с");
		// Доли секунды отбрасываются, а не округляются
		assertThat(Formats.duration(Duration.ofMillis(59_999))).isEqualTo("0 ч 00 мин 59 с");
	}

	@Test
	void dateTimeIsShownInBishkekTime() {
		// 18:30 UTC — это 00:30 следующего дня по Бишкеку (UTC+6)
		assertThat(Formats.dateTime(Instant.parse("2026-10-27T18:30:00Z"))).isEqualTo("28.10.2026 00:30");
	}

}
