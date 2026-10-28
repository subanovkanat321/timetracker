package kg.timetracker.timer;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kg.timetracker.BusinessException;
import kg.timetracker.Formats;
import kg.timetracker.Texts;
import kg.timetracker.employee.EmployeeRepository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimerService {

	static final int MAX_DESCRIPTION_LENGTH = 500;

	private final TimeEntryRepository entries;

	private final EmployeeRepository employees;

	private final JdbcClient jdbc;

	private final Clock clock;

	TimerService(TimeEntryRepository entries, EmployeeRepository employees, JdbcClient jdbc, Clock clock) {
		this.entries = entries;
		this.employees = employees;
		this.jdbc = jdbc;
		this.clock = clock;
	}

	/**
	 * Запускает таймер. Второй идущий таймер у сотрудника невозможен: это правило держит
	 * частичный уникальный индекс time_entry_one_running_uq. {@code ON CONFLICT ... DO NOTHING}
	 * при его нарушении не бросает ошибку и не ломает транзакцию — просто не вставляет строку.
	 * Если два «Старта» пришли одновременно, второй INSERT ждёт, пока первый закоммитится,
	 * и после этого ничего не вставляет; следующий SELECT уже видит таймер победителя.
	 */
	@Transactional
	public void start(long employeeId, String rawDescription) {
		String description = rawDescription == null ? "" : rawDescription.strip();
		if (description.isEmpty()) {
			throw new BusinessException("Опишите задачу перед запуском таймера");
		}
		if (Texts.hasForbiddenCharacters(description)) {
			throw new BusinessException("Описание содержит недопустимые символы");
		}
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new BusinessException("Описание длиннее " + MAX_DESCRIPTION_LENGTH + " символов");
		}
		if (!employees.existsById(employeeId)) {
			throw employeeNotFound();
		}
		boolean inserted = jdbc.sql("""
				insert into time_entry (employee_id, description, started_at)
				values (:employeeId, :description, :now)
				on conflict (employee_id) where ended_at is null do nothing
				returning id""")
			.param("employeeId", employeeId)
			.param("description", description)
			.param("now", OffsetDateTime.ofInstant(now(), ZoneOffset.UTC))
			.query(Long.class)
			.optional()
			.isPresent();
		if (!inserted) {
			throw entries.findByEmployeeIdAndEndedAtIsNull(employeeId)
				.map(running -> new BusinessException("У сотрудника уже идёт таймер: «%s» с %s"
					.formatted(running.getDescription(), Formats.dateTime(running.getStartedAt()))))
				// Победитель успел остановить свой таймер между нашим INSERT и SELECT
				.orElseGet(() -> new BusinessException("У сотрудника уже идёт таймер"));
		}
	}

	@Transactional
	public void stop(long employeeId) {
		if (!employees.existsById(employeeId)) {
			throw employeeNotFound();
		}
		if (entries.stopRunning(employeeId, now()) == 0) {
			throw new BusinessException("Таймер не запущен");
		}
	}

	/** Идущие таймеры по id сотрудника — для главной страницы. */
	@Transactional(readOnly = true)
	public Map<Long, TimeEntry> findAllRunning() {
		return entries.findByEndedAtIsNull()
			.stream()
			.collect(Collectors.toMap(t -> t.getEmployee().getId(), Function.identity()));
	}

	/** Время с точностью до секунды: доли секунды на экране не видны и не должны копиться в суммах. */
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}

	private static BusinessException employeeNotFound() {
		return new BusinessException("Сотрудник не найден");
	}

}
