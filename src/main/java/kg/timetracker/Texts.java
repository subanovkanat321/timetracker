package kg.timetracker;

import java.util.regex.Pattern;

/** Проверки и очистка текста, который вводит пользователь (имя сотрудника, описание задачи). */
public final class Texts {

	/** Любые пробелы Unicode (в том числе неразрывные U+00A0, U+202F) и переводы строк. */
	private static final Pattern SPACES = Pattern.compile("[\\p{Z}\\s]+");

	private Texts() {
	}

	/**
	 * Схлопывает любые пробелы в один обычный и убирает их по краям. Иначе «Иван Петров» с
	 * неразрывным пробелом — другой человек для уникального индекса, а строка из одних
	 * неразрывных пробелов выглядит пустой, но проходит проверку.
	 */
	public static String normalizeSpaces(String text) {
		return text == null ? "" : SPACES.matcher(text).replaceAll(" ").strip();
	}

	/**
	 * Управляющие символы (например, \0 — Postgres не хранит его в тексте) и невидимые
	 * форматные (например, пробел нулевой ширины U+200B). Перебор по кодовым точкам, а не по
	 * char: символы за пределами U+FFFF (теги U+E0000…) в char — две половинки суррогатной пары.
	 */
	public static boolean hasForbiddenCharacters(String text) {
		return text.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT);
	}

	public static boolean hasLetterOrDigit(String text) {
		return text.codePoints().anyMatch(Character::isLetterOrDigit);
	}

	/** Не длиннее {@code max} символов — для того, что возвращается в форму через сессию. */
	public static String limit(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max);
	}

}
