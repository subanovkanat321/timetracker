package kg.timetracker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вымышленные сотрудники и записи за последние дни, чтобы после первого запуска было что
 * посмотреть в отчёте. Включается {@code app.demo-data=true} (так в compose.yaml) и
 * срабатывает только на пустой таблице employee — после перезапуска данные не дублируются.
 */
@Component
@ConditionalOnBooleanProperty("app.demo-data")
class DemoData implements ApplicationRunner {

	private final JdbcClient jdbc;

	private final Clock clock;

	DemoData(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		long count = jdbc.sql("select count(*) from employee").query(Long.class).single();
		if (count > 0) {
			return;
		}
		long aibek = employee("Айбек Садыков");
		long nurgul = employee("Нургуль Асанова");
		employee("Тимур Беков");

		LocalDate today = LocalDate.now(clock.withZone(TimetrackerApplication.ZONE));
		entry(aibek, "Вёрстка страницы отчёта", today.minusDays(1), "09:00", "12:30");
		entry(aibek, "Ревью кода", today.minusDays(1), "13:30", "15:10");
		entry(nurgul, "Звонок с поставщиком", today.minusDays(2), "10:00", "10:45");
		entry(nurgul, "Подготовка счетов", today.minusDays(1), "11:00", "17:20");
		// Идущий таймер: начат 25 минут назад
		Instant now = clock.instant();
		insert(aibek, "Исправление ошибок в отчёте", now.minus(Duration.ofMinutes(25)), null);
	}

	private long employee(String name) {
		return jdbc.sql("insert into employee (name, created_at) values (:name, :now) returning id")
			.param("name", name)
			.param("now", utc(clock.instant()))
			.query(Long.class)
			.single();
	}

	private void entry(long employeeId, String description, LocalDate day, String from, String to) {
		insert(employeeId, description, at(day, from), at(day, to));
	}

	private void insert(long employeeId, String description, Instant startedAt, Instant endedAt) {
		jdbc.sql("""
				insert into time_entry (employee_id, description, started_at, ended_at)
				values (:employeeId, :description, :startedAt, :endedAt)""")
			.param("employeeId", employeeId)
			.param("description", description)
			.param("startedAt", utc(startedAt))
			.param("endedAt", endedAt == null ? null : utc(endedAt))
			.update();
	}

	private static Instant at(LocalDate day, String time) {
		return day.atTime(LocalTime.parse(time)).atZone(TimetrackerApplication.ZONE).toInstant();
	}

	private static OffsetDateTime utc(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
