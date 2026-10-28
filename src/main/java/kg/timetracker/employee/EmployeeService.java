package kg.timetracker.employee;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import kg.timetracker.BusinessException;
import kg.timetracker.Texts;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

	static final int MAX_NAME_LENGTH = 100;

	private final EmployeeRepository employees;

	private final JdbcClient jdbc;

	private final Clock clock;

	EmployeeService(EmployeeRepository employees, JdbcClient jdbc, Clock clock) {
		this.employees = employees;
		this.jdbc = jdbc;
		this.clock = clock;
	}

	/**
	 * Добавляет сотрудника и возвращает сохранённое имя (без пробелов по краям).
	 * Дубликат без учёта регистра отсекает индекс employee_name_uq на lower(name):
	 * {@code ON CONFLICT ... DO NOTHING} тогда ничего не вставляет — проверять заранее не нужно.
	 */
	@Transactional
	public String add(String rawName) {
		String name = rawName == null ? "" : rawName.strip();
		if (name.isEmpty()) {
			throw new BusinessException("Введите имя сотрудника");
		}
		if (Texts.hasForbiddenCharacters(name)) {
			throw new BusinessException("Имя содержит недопустимые символы");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			throw new BusinessException("Имя длиннее " + MAX_NAME_LENGTH + " символов");
		}
		boolean inserted = jdbc.sql("""
				insert into employee (name, created_at)
				values (:name, :now)
				on conflict ((lower(name))) do nothing
				returning id""")
			.param("name", name)
			.param("now", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))
			.query(Long.class)
			.optional()
			.isPresent();
		if (!inserted) {
			throw new BusinessException("Сотрудник «" + name + "» уже есть");
		}
		return name;
	}

	@Transactional(readOnly = true)
	public List<Employee> findAll() {
		return employees.findAllByName();
	}

}
