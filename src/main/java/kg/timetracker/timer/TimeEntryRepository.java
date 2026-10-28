package kg.timetracker.timer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

	Optional<TimeEntry> findByEmployeeIdAndEndedAtIsNull(Long employeeId);

	List<TimeEntry> findByEndedAtIsNull();

	/**
	 * Останавливает идущий таймер; возвращает число остановленных записей (0 или 1). Если часы
	 * сервера ушли назад раньше старта (коррекция NTP), конец = старт: запись нулевой длины
	 * вместо нарушения правила ended_at >= started_at и ошибки 500.
	 */
	@Modifying
	@Query("update TimeEntry t set t.endedAt = case when :now < t.startedAt then t.startedAt else :now end"
			+ " where t.employee.id = :employeeId and t.endedAt is null")
	int stopRunning(Long employeeId, Instant now);

	/**
	 * Записи, которые пересекаются с полуинтервалом [start, end): начались до конца периода
	 * и закончились после его начала. Идущая запись (endedAt = null) ещё не закончилась.
	 */
	@Query("""
			select t from TimeEntry t join fetch t.employee
			where t.startedAt < :end and (t.endedAt is null or t.endedAt > :start)
			order by t.startedAt""")
	List<TimeEntry> findOverlapping(Instant start, Instant end);

}
