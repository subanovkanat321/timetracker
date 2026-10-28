package kg.timetracker;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Общая основа интеграционных тестов: пустая база и часы на {@link MutableClock#DEFAULT}. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

	@Autowired
	protected JdbcClient jdbc;

	@Autowired
	protected MutableClock clock;

	@BeforeEach
	void cleanDatabase() {
		jdbc.sql("truncate time_entry, employee restart identity").update();
		clock.set(MutableClock.DEFAULT);
	}

	protected long insertEmployee(String name) {
		return jdbc.sql("insert into employee (name, created_at) values (:name, now()) returning id")
			.param("name", name)
			.query(Long.class)
			.single();
	}

	/** Запись напрямую в базу; {@code endedAt == null} — идущий таймер. */
	protected void insertEntry(long employeeId, String description, Instant startedAt, Instant endedAt) {
		jdbc.sql("""
				insert into time_entry (employee_id, description, started_at, ended_at)
				values (:employeeId, :description, :startedAt, :endedAt)""")
			.param("employeeId", employeeId)
			.param("description", description)
			.param("startedAt", OffsetDateTime.ofInstant(startedAt, ZoneOffset.UTC))
			.param("endedAt", endedAt == null ? null : OffsetDateTime.ofInstant(endedAt, ZoneOffset.UTC))
			.update();
	}

	protected long count(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
