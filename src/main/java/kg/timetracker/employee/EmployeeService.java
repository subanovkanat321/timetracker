package kg.timetracker.employee;

import java.time.Clock;
import java.util.List;

import kg.timetracker.BusinessException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

	static final int MAX_NAME_LENGTH = 100;

	private final EmployeeRepository employees;

	private final Clock clock;

	EmployeeService(EmployeeRepository employees, Clock clock) {
		this.employees = employees;
		this.clock = clock;
	}

	public Employee add(String rawName) {
		String name = rawName == null ? "" : rawName.strip();
		if (name.isEmpty()) {
			throw new BusinessException("Введите имя сотрудника");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			throw new BusinessException("Имя длиннее " + MAX_NAME_LENGTH + " символов");
		}
		if (employees.existsByNameIgnoreCase(name)) {
			throw duplicate(name);
		}
		try {
			return employees.saveAndFlush(new Employee(name, clock.instant()));
		}
		catch (DataIntegrityViolationException e) {
			// Тот же сотрудник добавлен параллельно между проверкой и вставкой: сработал
			// уникальный индекс employee_name_uq
			throw duplicate(name);
		}
	}

	@Transactional(readOnly = true)
	public List<Employee> findAll() {
		return employees.findAllByName();
	}

	private static BusinessException duplicate(String name) {
		return new BusinessException("Сотрудник «" + name + "» уже есть");
	}

}
