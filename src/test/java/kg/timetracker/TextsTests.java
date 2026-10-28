package kg.timetracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextsTests {

	@Test
	void limitKeepsShortTextAndCutsLongText() {
		assertThat(Texts.limit("Айбек", 10)).isEqualTo("Айбек");
		assertThat(Texts.limit("Айбек Садыков", 5)).isEqualTo("Айбек");
	}

	@Test
	void limitDoesNotSplitSurrogatePair() {
		// «😀» в Java — два char (суррогатная пара); граница обрезки 5 приходится между ними
		String text = "Тест😀ы";
		assertThat(Texts.limit(text, 5)).isEqualTo("Тест");
		assertThat(Texts.limit(text, 6)).isEqualTo("Тест😀");
	}

}
