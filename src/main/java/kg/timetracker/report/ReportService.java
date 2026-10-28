package kg.timetracker.report;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kg.timetracker.BusinessException;
import kg.timetracker.TimetrackerApplication;
import kg.timetracker.employee.EmployeeRepository;
import kg.timetracker.timer.TimeEntry;
import kg.timetracker.timer.TimeEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

	private final TimeEntryRepository entries;

	private final EmployeeRepository employees;

	private final Clock clock;

	ReportService(TimeEntryRepository entries, EmployeeRepository employees, Clock clock) {
		this.entries = entries;
		this.employees = employees;
		this.clock = clock;
	}

	/**
	 * @param from первый день периода, включительно (по Бишкеку)
	 * @param to   последний день периода, включительно (по Бишкеку)
	 */
	@Transactional(readOnly = true)
	public Report build(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException("Дата «с» позже даты «по»");
		}
		// Дни по Бишкеку → полуинтервал в UTC: [from 00:00, (to + 1) 00:00)
		Instant start = from.atStartOfDay(TimetrackerApplication.ZONE).toInstant();
		Instant end = to.plusDays(1).atStartOfDay(TimetrackerApplication.ZONE).toInstant();
		Instant now = clock.instant();

		Map<Long, Duration> byEmployee = new HashMap<>();
		List<Report.Row> rows = new ArrayList<>();
		for (TimeEntry t : entries.findOverlapping(start, end)) {
			Duration inPeriod = Overlap.of(t.getStartedAt(), t.getEndedAt(), start, end, now);
			// Таймер, который идёт, но период ещё не наступил, даёт 0 — такую запись не показываем
			if (inPeriod.isPositive()) {
				rows.add(new Report.Row(t.getEmployee().getName(), t.getDescription(), t.getStartedAt(),
						t.getEndedAt(), inPeriod));
				byEmployee.merge(t.getEmployee().getId(), inPeriod, Duration::plus);
			}
		}

		List<Report.EmployeeTotal> totals = employees.findAllByName()
			.stream()
			.map(e -> new Report.EmployeeTotal(e.getId(), e.getName(),
					byEmployee.getOrDefault(e.getId(), Duration.ZERO)))
			.toList();
		Duration total = totals.stream().map(Report.EmployeeTotal::duration).reduce(Duration.ZERO, Duration::plus);
		return new Report(from, to, totals, rows, total);
	}

	/** Период по умолчанию — текущая неделя по Бишкеку, с понедельника по воскресенье. */
	public Period currentWeek() {
		LocalDate today = LocalDate.now(clock.withZone(TimetrackerApplication.ZONE));
		return new Period(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
	}

	public record Period(LocalDate from, LocalDate to) {
	}

}
