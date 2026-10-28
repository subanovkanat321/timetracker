package kg.timetracker;

import static org.assertj.core.api.Assertions.assertThat;

import kg.timetracker.employee.EmployeeService;
import kg.timetracker.timer.TimerService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Данные переживают перезапуск приложения: второй запуск на той же базе видит сотрудника и
 * его идущий таймер. Проверяется сторона приложения (всё хранится в Postgres, ничего в памяти);
 * том Docker pgdata из compose.yaml проверяется вручную — см. README.
 */
@Testcontainers
class RestartTests {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Test
	void dataSurvivesRestart() {
		try (var first = start()) {
			first.getBean(EmployeeService.class).add("Айбек");
			long id = first.getBean(EmployeeService.class).findAll().getFirst().getId();
			first.getBean(TimerService.class).start(id, "Задача до перезапуска");
		}

		try (var second = start()) {
			var employees = second.getBean(EmployeeService.class).findAll();
			assertThat(employees).singleElement().satisfies(e -> assertThat(e.getName()).isEqualTo("Айбек"));
			var running = second.getBean(TimerService.class).findAllRunning();
			assertThat(running.values()).singleElement()
				.satisfies(t -> assertThat(t.getDescription()).isEqualTo("Задача до перезапуска"));
		}
	}

	/**
	 * Адрес базы — аргументами командной строки: у них высший приоритет. Через
	 * {@code builder.properties(...)} нельзя: это свойства по умолчанию, их перекрывает
	 * {@code spring.datasource.url} из application.yaml, и тест ушёл бы в базу на localhost:5432.
	 */
	private static ConfigurableApplicationContext start() {
		return new SpringApplicationBuilder(TimetrackerApplication.class).run(
				"--spring.datasource.url=" + postgres.getJdbcUrl(),
				"--spring.datasource.username=" + postgres.getUsername(),
				"--spring.datasource.password=" + postgres.getPassword(), "--server.port=0");
	}

}
