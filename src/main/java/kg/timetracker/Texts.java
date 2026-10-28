package kg.timetracker;

/** Проверки текста, который вводит пользователь (имя сотрудника, описание задачи). */
public final class Texts {

	private Texts() {
	}

	/**
	 * Управляющие символы (например, \0 — Postgres не хранит его в тексте) и невидимые
	 * форматные (например, пробел нулевой ширины U+200B — из них можно сложить «пустое» имя).
	 */
	public static boolean hasForbiddenCharacters(String text) {
		return text.chars().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT);
	}

}
