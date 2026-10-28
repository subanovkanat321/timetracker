package kg.timetracker.timer;

import java.util.Map;

import kg.timetracker.BusinessException;
import kg.timetracker.Formats;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Таймеры для веб-слоя. Сам без транзакции — и это главное: если два «Старта» пришли
 * одновременно, второй INSERT нарушает индекс time_entry_one_running_uq, и Postgres
 * помечает его транзакцию прерванной — в ней уже нельзя даже прочитать, какой таймер идёт.
 * Поэтому ошибка ловится здесь, после отката, а идущая запись читается новой транзакцией.
 */
@Component
public class TimerFacade {

	static final String ONE_RUNNING_INDEX = "time_entry_one_running_uq";

	private final TimerService timers;

	TimerFacade(TimerService timers) {
		this.timers = timers;
	}

	public TimeEntry start(long employeeId, String description) {
		try {
			return timers.start(employeeId, description);
		}
		catch (DataIntegrityViolationException e) {
			if (!violates(e, ONE_RUNNING_INDEX)) {
				throw e;
			}
			throw timers.findRunning(employeeId)
				.map(running -> new BusinessException("У сотрудника уже идёт таймер: «%s» с %s"
					.formatted(running.getDescription(), Formats.dateTime(running.getStartedAt()))))
				// Идущий таймер успели остановить между ошибкой и чтением
				.orElseGet(() -> new BusinessException("У сотрудника уже идёт таймер"));
		}
	}

	public void stop(long employeeId) {
		timers.stop(employeeId);
	}

	public Map<Long, TimeEntry> findAllRunning() {
		return timers.findAllRunning();
	}

	private static boolean violates(Throwable e, String constraint) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return constraint.equals(violation.getConstraintName());
			}
		}
		return false;
	}

}
