package kg.timetracker.report;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Отчёт за период: итог по каждому сотруднику (в том числе нулевой) и записи периода.
 * Длительности — только та часть, что пришлась на период.
 */
public record Report(LocalDate from, LocalDate to, List<EmployeeTotal> totals, List<Row> rows, Duration total) {

	public record EmployeeTotal(long employeeId, String name, Duration duration) {
	}

	/** {@code endedAt == null} — таймер ещё идёт. */
	public record Row(String employeeName, String description, Instant startedAt, Instant endedAt,
			Duration inPeriod) {
	}

}
