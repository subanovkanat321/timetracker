package kg.timetracker.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kg.timetracker.BusinessException;
import kg.timetracker.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmployeeServiceTests extends IntegrationTest {

	@Autowired
	EmployeeService employees;

	@Test
	void addsEmployeeWithTrimmedName() {
		assertThat(employees.add("  Айбек Садыков  ")).isEqualTo("Айбек Садыков");

		assertThat(employees.findAll()).extracting(Employee::getName).containsExactly("Айбек Садыков");
	}

	@Test
	void rejectsEmptyName() {
		assertThatThrownBy(() -> employees.add("   ")).isInstanceOf(BusinessException.class)
			.hasMessage("Введите имя сотрудника");
		assertThat(count("select count(*) from employee")).isZero();
	}

	@Test
	void rejectsTooLongName() {
		assertThatThrownBy(() -> employees.add("я".repeat(101))).isInstanceOf(BusinessException.class)
			.hasMessage("Имя длиннее 100 символов");
		assertThat(employees.add("я".repeat(100))).hasSize(100);
	}

	@Test
	void rejectsDuplicateIgnoringCase() {
		employees.add("Айбек Садыков");

		// Кириллица в другом регистре — тот же сотрудник: индекс построен на lower(name)
		assertThatThrownBy(() -> employees.add("айбек САДЫКОВ")).isInstanceOf(BusinessException.class)
			.hasMessage("Сотрудник «айбек САДЫКОВ» уже есть");
		assertThat(count("select count(*) from employee")).isEqualTo(1);
	}

	@Test
	void listsEmployeesByNameIgnoringCase() {
		employees.add("нургуль");
		employees.add("Айбек");
		employees.add("Тимур");

		assertThat(employees.findAll()).extracting(Employee::getName).containsExactly("Айбек", "нургуль", "Тимур");
	}

}
