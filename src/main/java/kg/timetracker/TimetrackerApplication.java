package kg.timetracker;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TimetrackerApplication {

	/** Часовой пояс компании: по нему считаются дни отчёта и показывается время. В базе — UTC. */
	public static final ZoneId ZONE = ZoneId.of("Asia/Bishkek");

	public static void main(String[] args) {
		SpringApplication.run(TimetrackerApplication.class, args);
	}

	/** «Сейчас» берётся только отсюда: в тестах часы подменяются, и время становится предсказуемым. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
