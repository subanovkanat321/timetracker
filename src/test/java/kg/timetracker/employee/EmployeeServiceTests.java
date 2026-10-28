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
	void rejectsControlAndInvisibleCharacters() {
		// \0 Postgres не хранит в тексте; без проверки это был 500
		assertThatThrownBy(() -> employees.add("Ай\u0000бек")).isInstanceOf(BusinessException.class)
			.hasMessage("Имя содержит недопустимые символы");
		// Имя из одних невидимых символов — не имя
		assertThatThrownBy(() -> employees.add("\u200B\u200B")).isInstanceOf(BusinessException.class)
			.hasMessage("Имя содержит недопустимые символы");
		assertThat(count("select count(*) from employee")).isZero();
	}

	@Test
	void rejectsInvisibleCharactersOutsideBasicPlane() {
		// Теги U+E0041, U+E0042 — невидимые «форматные» символы за пределами U+FFFF
		String tags = new String(Character.toChars(0xE0041)) + new String(Character.toChars(0xE0042));
		assertThatThrownBy(() -> employees.add("Айбек" + tags)).isInstanceOf(BusinessException.class)
			.hasMessage("Имя содержит недопустимые символы");
	}

	@Test
	void normalizesSpacesSoLookAlikeNamesAreDuplicates() {
		// Неразрывные и повторные пробелы — тот же человек, что и с обычным пробелом
		assertThat(employees.add("\u00A0Иван\u00A0\u00A0Петров\u202F")).isEqualTo("Иван Петров");
		assertThatThrownBy(() -> employees.add("Иван Петров")).isInstanceOf(BusinessException.class)
			.hasMessage("Сотрудник «Иван Петров» уже есть");
		// Из одних неразрывных пробелов имя не сложить
		assertThatThrownBy(() -> employees.add("\u00A0\u00A0")).isInstanceOf(BusinessException.class)
			.hasMessage("Введите имя сотрудника");
	}

	@Test
	void nameNeedsLetterOrDigit() {
		assertThatThrownBy(() -> employees.add("---")).isInstanceOf(BusinessException.class)
			.hasMessage("Имя должно содержать буквы или цифры");
	}

	@Test
	void listsEmployeesByNameIgnoringCase() {
		employees.add("нургуль");
		employees.add("Айбек");
		employees.add("Тимур");

		assertThat(employees.findAll()).extracting(Employee::getName).containsExactly("Айбек", "нургуль", "Тимур");
	}

}
