package kg.timetracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Демо-данные (включены в compose.yaml): создаются один раз и не дублируются при перезапуске. */
@SpringBootTest(properties = "app.demo-data=true")
class DemoDataTests extends IntegrationTest {

	@Autowired
	DemoData demoData;

	@Test
	void seedsOnceAndNotAgain() {
		demoData.run(null);
		demoData.run(null);

		assertThat(count("select count(*) from employee")).isEqualTo(3);
		assertThat(count("select count(*) from time_entry")).isEqualTo(5);
		assertThat(count("select count(*) from time_entry where ended_at is null")).isEqualTo(1);
	}

}
