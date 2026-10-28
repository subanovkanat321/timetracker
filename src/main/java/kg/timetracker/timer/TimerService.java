package kg.timetracker.timer;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import kg.timetracker.BusinessException;
import kg.timetracker.employee.EmployeeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Запись в базу: каждый метод — отдельная транзакция. Снаружи вызывается через
 * {@link TimerFacade}, который разбирает ошибку уникального индекса уже после отката.
 */
@Service
@Transactional
class TimerService {

	static final int MAX_DESCRIPTION_LENGTH = 500;

	private final TimeEntryRepository entries;

	private final EmployeeRepository employees;

	private final Clock clock;

	TimerService(TimeEntryRepository entries, EmployeeRepository employees, Clock clock) {
		this.entries = entries;
		this.employees = employees;
		this.clock = clock;
	}

	TimeEntry start(long employeeId, String rawDescription) {
		String description = rawDescription == null ? "" : rawDescription.strip();
		if (description.isEmpty()) {
			throw new BusinessException("Опишите задачу перед запуском таймера");
		}
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new BusinessException("Описание длиннее " + MAX_DESCRIPTION_LENGTH + " символов");
		}
		var employee = employees.findById(employeeId).orElseThrow(TimerService::employeeNotFound);
		// Без предварительной проверки «таймер уже идёт»: её всё равно обгонит параллельный
		// запрос. Второй таймер не даст создать индекс time_entry_one_running_uq
		return entries.saveAndFlush(new TimeEntry(employee, description, clock.instant(), null));
	}

	void stop(long employeeId) {
		if (!employees.existsById(employeeId)) {
			throw employeeNotFound();
		}
		if (entries.stopRunning(employeeId, clock.instant()) == 0) {
			throw new BusinessException("Таймер не запущен");
		}
	}

	@Transactional(readOnly = true)
	Optional<TimeEntry> findRunning(long employeeId) {
		return entries.findByEmployeeIdAndEndedAtIsNull(employeeId);
	}

	/** Идущие таймеры по id сотрудника — для главной страницы. */
	@Transactional(readOnly = true)
	Map<Long, TimeEntry> findAllRunning() {
		return entries.findByEndedAtIsNull()
			.stream()
			.collect(Collectors.toMap(t -> t.getEmployee().getId(), Function.identity()));
	}

	private static BusinessException employeeNotFound() {
		return new BusinessException("Сотрудник не найден");
	}

}
