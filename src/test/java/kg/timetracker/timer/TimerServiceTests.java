package kg.timetracker.timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import kg.timetracker.BusinessException;
import kg.timetracker.IntegrationTest;
import kg.timetracker.MutableClock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TimerServiceTests extends IntegrationTest {

	@Autowired
	TimerService timers;

	@Autowired
	DataSource dataSource;

	@Test
	void startThenStopSavesBothTimes() {
		long id = insertEmployee("Айбек");

		timers.start(id, "  Вёрстка отчёта ");
		clock.advance(Duration.ofMinutes(90));
		timers.stop(id);

		var entry = jdbc.sql("select description, started_at, ended_at from time_entry")
			.query((rs, n) -> new Object[] { rs.getString(1), rs.getTimestamp(2).toInstant(),
					rs.getTimestamp(3).toInstant() })
			.single();
		assertThat(entry[0]).isEqualTo("Вёрстка отчёта");
		assertThat(entry[1]).isEqualTo(MutableClock.DEFAULT);
		assertThat(entry[2]).isEqualTo(MutableClock.DEFAULT.plus(Duration.ofMinutes(90)));
	}

	@Test
	void repeatedStartIsRejectedAndKeepsOneRunningTimer() {
		long id = insertEmployee("Айбек");
		timers.start(id, "Вёрстка отчёта");
		clock.advance(Duration.ofMinutes(5));

		// 13:00 по Бишкеку — время первого старта
		assertThatThrownBy(() -> timers.start(id, "Другая задача")).isInstanceOf(BusinessException.class)
			.hasMessage("У сотрудника уже идёт таймер: «Вёрстка отчёта» с 27.10.2026 13:00");
		assertThat(count("select count(*) from time_entry where ended_at is null")).isEqualTo(1);
	}

	@Test
	void canStartAgainAfterStop() {
		long id = insertEmployee("Айбек");
		timers.start(id, "Первая задача");
		clock.advance(Duration.ofMinutes(10));
		timers.stop(id);

		timers.start(id, "Вторая задача");

		assertThat(count("select count(*) from time_entry")).isEqualTo(2);
		assertThat(count("select count(*) from time_entry where ended_at is null")).isEqualTo(1);
	}

	@Test
	void timersOfDifferentEmployeesAreIndependent() {
		long aibek = insertEmployee("Айбек");
		long nurgul = insertEmployee("Нургуль");

		timers.start(aibek, "Задача А");
		timers.start(nurgul, "Задача Н");

		assertThat(timers.findAllRunning()).containsOnlyKeys(aibek, nurgul);
	}

	/**
	 * Гонка без надежды на планировщик потоков: тест сам держит незакоммиченную вставку
	 * идущего таймера (как будто первый «Старт» ещё не завершился), а второй «Старт» в другом
	 * потоке должен ждать блокировку индекса. После коммита первого — второй получает отказ.
	 */
	@Test
	void concurrentStartWaitsForFirstAndIsRejected() throws Exception {
		long id = insertEmployee("Айбек");

		CompletableFuture<Throwable> second;
		try (Connection first = dataSource.getConnection()) {
			first.setAutoCommit(false);
			try (PreparedStatement insert = first.prepareStatement(
					"insert into time_entry (employee_id, description, started_at) values (?, ?, ?)")) {
				insert.setLong(1, id);
				insert.setString(2, "Первый старт");
				insert.setTimestamp(3, Timestamp.from(MutableClock.DEFAULT));
				insert.executeUpdate();
			}

			second = CompletableFuture.supplyAsync(() -> {
				try {
					timers.start(id, "Второй старт");
					return null;
				}
				catch (Throwable e) {
					return e;
				}
			});

			// Второй INSERT действительно ждёт блокировку, а не прошёл мимо
			awaitLockWait();
			assertThat(second).isNotDone();

			first.commit();
		}

		Throwable result = second.get(10, TimeUnit.SECONDS);
		assertThat(result).isInstanceOf(BusinessException.class)
			.hasMessageContaining("уже идёт таймер: «Первый старт»");
		assertThat(count("select count(*) from time_entry")).isEqualTo(1);
	}

	@Test
	void stopWithoutRunningTimerIsRejected() {
		long id = insertEmployee("Айбек");

		assertThatThrownBy(() -> timers.stop(id)).isInstanceOf(BusinessException.class)
			.hasMessage("Таймер не запущен");
	}

	@Test
	void unknownEmployeeIsRejected() {
		assertThatThrownBy(() -> timers.start(999, "Задача")).isInstanceOf(BusinessException.class)
			.hasMessage("Сотрудник не найден");
		assertThatThrownBy(() -> timers.stop(999)).isInstanceOf(BusinessException.class)
			.hasMessage("Сотрудник не найден");
	}

	@Test
	void descriptionIsRequiredAndLimited() {
		long id = insertEmployee("Айбек");

		assertThatThrownBy(() -> timers.start(id, "  ")).isInstanceOf(BusinessException.class)
			.hasMessage("Опишите задачу перед запуском таймера");
		assertThatThrownBy(() -> timers.start(id, "x".repeat(501))).isInstanceOf(BusinessException.class)
			.hasMessage("Описание длиннее 500 символов");
		assertThat(count("select count(*) from time_entry")).isZero();
	}

	private void awaitLockWait() throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (count("select count(*) from pg_stat_activity where wait_event_type = 'Lock'") > 0) {
				return;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("Второй старт не встал в ожидание блокировки за 10 с");
	}

}
