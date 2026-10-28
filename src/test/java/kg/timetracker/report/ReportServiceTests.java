package kg.timetracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import kg.timetracker.BusinessException;
import kg.timetracker.IntegrationTest;
import kg.timetracker.TimetrackerApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportServiceTests extends IntegrationTest {

	// MutableClock.DEFAULT — вторник 27.10.2026, 13:00 по Бишкеку
	private static final LocalDate MONDAY = LocalDate.parse("2026-10-26");

	private static final LocalDate TUESDAY = LocalDate.parse("2026-10-27");

	@Autowired
	ReportService reports;

	@Test
	void emptyReportWithoutEmployees() {
		Report report = reports.build(MONDAY, TUESDAY);

		assertThat(report.totals()).isEmpty();
		assertThat(report.rows()).isEmpty();
		assertThat(report.total()).isZero();
	}

	@Test
	void emptyPeriodListsEveryEmployeeWithZero() {
		long aibek = insertEmployee("Айбек");
		insertEmployee("Нургуль");
		// Запись есть, но вне периода
		insertEntry(aibek, "Прошлая неделя", at("2026-10-20T09:00"), at("2026-10-20T10:00"));

		Report report = reports.build(MONDAY, TUESDAY);

		assertThat(report.rows()).isEmpty();
		assertThat(report.totals()).extracting(Report.EmployeeTotal::name).containsExactly("Айбек", "Нургуль");
		assertThat(report.totals()).allSatisfy(t -> assertThat(t.duration()).isZero());
		assertThat(report.total()).isZero();
	}

	@Test
	void sumsEntriesPerEmployee() {
		long aibek = insertEmployee("Айбек");
		long nurgul = insertEmployee("Нургуль");
		insertEntry(aibek, "Вёрстка", at("2026-10-26T09:00"), at("2026-10-26T11:30"));
		insertEntry(aibek, "Ревью", at("2026-10-27T10:00"), at("2026-10-27T10:45"));
		insertEntry(nurgul, "Счета", at("2026-10-26T14:00"), at("2026-10-26T15:00"));

		Report report = reports.build(MONDAY, TUESDAY);

		assertThat(report.totals()).extracting(Report.EmployeeTotal::duration)
			.containsExactly(Duration.ofMinutes(195), Duration.ofMinutes(60));
		assertThat(report.total()).isEqualTo(Duration.ofMinutes(255));
		assertThat(report.rows()).extracting(Report.Row::description).containsExactly("Вёрстка", "Счета", "Ревью");
	}

	@Test
	void cutsEntryAtPeriodBoundary() {
		long id = insertEmployee("Айбек");
		// С воскресенья 23:00 до понедельника 01:00 по Бишкеку: в отчёт за понедельник — 1 час
		insertEntry(id, "Ночной релиз", at("2026-10-25T23:00"), at("2026-10-26T01:00"));

		Report report = reports.build(MONDAY, MONDAY);

		assertThat(report.rows()).singleElement()
			.satisfies(row -> assertThat(row.inPeriod()).isEqualTo(Duration.ofHours(1)));
		assertThat(report.total()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void daysAreCountedInBishkekTime() {
		long id = insertEmployee("Айбек");
		// 26.10 23:30 UTC — это уже 27.10 05:30 по Бишкеку (UTC+6)
		insertEntry(id, "Утренняя задача", Instant.parse("2026-10-26T23:30:00Z"), Instant.parse("2026-10-27T00:30:00Z"));

		assertThat(reports.build(MONDAY, MONDAY).rows()).isEmpty();
		assertThat(reports.build(TUESDAY, TUESDAY).total()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void runningTimerCountsUntilNow() {
		long id = insertEmployee("Айбек");
		insertEntry(id, "Идёт", at("2026-10-27T12:00"), null);
		clock.advance(Duration.ofMinutes(30));

		Report report = reports.build(TUESDAY, TUESDAY);

		// С 12:00 до «сейчас» (13:30)
		assertThat(report.rows()).singleElement().satisfies(row -> {
			assertThat(row.endedAt()).isNull();
			assertThat(row.inPeriod()).isEqualTo(Duration.ofMinutes(90));
		});
	}

	@Test
	void runningTimerIsHiddenInPeriodThatHasNotStarted() {
		long id = insertEmployee("Айбек");
		insertEntry(id, "Идёт", at("2026-10-27T12:00"), null);

		LocalDate tomorrow = TUESDAY.plusDays(1);
		Report report = reports.build(tomorrow, tomorrow);

		assertThat(report.rows()).isEmpty();
		assertThat(report.total()).isZero();
	}

	@Test
	void zeroLengthEntryIsShown() {
		long id = insertEmployee("Айбек");
		insertEntry(id, "Нажал Старт и сразу Стоп", at("2026-10-27T10:00"), at("2026-10-27T10:00"));

		assertThat(reports.build(TUESDAY, TUESDAY).rows()).singleElement()
			.satisfies(row -> assertThat(row.inPeriod()).isZero());
	}

	@Test
	void rowsAddUpToTotalToTheSecond() {
		long id = insertEmployee("Айбек");
		for (int i = 0; i < 3; i++) {
			Instant start = at("2026-10-27T10:00").plus(Duration.ofMinutes(i));
			insertEntry(id, "Короткая " + i, start, start.plusSeconds(40));
		}

		Report report = reports.build(TUESDAY, TUESDAY);

		Duration sumOfRows = report.rows().stream().map(Report.Row::inPeriod).reduce(Duration.ZERO, Duration::plus);
		assertThat(sumOfRows).isEqualTo(report.total()).isEqualTo(Duration.ofSeconds(120));
	}

	@Test
	void rowsAddUpToTotalWithFractionalSeconds() {
		long id = insertEmployee("Айбек");
		// Записи с долями секунды (например, сделанные до округления времени) и идущий таймер
		insertEntry(id, "Первая", at("2026-10-27T10:00").plusMillis(300), at("2026-10-27T10:00").plusMillis(1_900));
		insertEntry(id, "Вторая", at("2026-10-27T11:00").plusMillis(300), at("2026-10-27T11:00").plusMillis(1_900));
		insertEntry(id, "Идёт", at("2026-10-27T12:00").plusMillis(500), null);

		Report report = reports.build(TUESDAY, TUESDAY);

		// Каждая строка — целые секунды, итог — ровно их сумма: на экране всё сходится
		assertThat(report.rows()).allSatisfy(row -> assertThat(row.inPeriod().getNano()).isZero());
		Duration sumOfRows = report.rows().stream().map(Report.Row::inPeriod).reduce(Duration.ZERO, Duration::plus);
		assertThat(report.total()).isEqualTo(sumOfRows);
	}

	@Test
	void fromAfterToIsRejected() {
		assertThatThrownBy(() -> reports.build(TUESDAY, MONDAY)).isInstanceOf(BusinessException.class)
			.hasMessage("Дата «с» позже даты «по»");
	}

	@Test
	void defaultPeriodIsCurrentWeekInBishkek() {
		var week = reports.currentWeek();

		assertThat(week.from()).isEqualTo(MONDAY);
		assertThat(week.to()).isEqualTo(LocalDate.parse("2026-11-01"));
	}

	/** Время по Бишкеку: "2026-10-27T10:00". */
	private static Instant at(String bishkekDateTime) {
		return LocalDateTime.parse(bishkekDateTime).atZone(TimetrackerApplication.ZONE).toInstant();
	}

}
