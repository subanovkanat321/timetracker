package kg.timetracker.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kg.timetracker.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Страницы целиком: контроллер → сервис → Postgres → шаблон. */
@AutoConfigureMockMvc
class WebTests extends IntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void emptyHomePageInvitesToAddEmployee() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Сотрудников пока нет")));
	}

	@Test
	void addEmployeeRedirectsWithMessage() throws Exception {
		mvc.perform(post("/employees").param("name", "Айбек Садыков"))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("message", "Сотрудник «Айбек Садыков» добавлен"));

		mvc.perform(get("/")).andExpect(content().string(containsString("Айбек Садыков")));
	}

	@Test
	void duplicateEmployeeKeepsEnteredName() throws Exception {
		insertEmployee("Айбек Садыков");

		mvc.perform(post("/employees").param("name", "айбек садыков"))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("error", "Сотрудник «айбек садыков» уже есть"))
			.andExpect(flash().attribute("name", "айбек садыков"));
	}

	@Test
	void runningTimerIsShownWithStopButton() throws Exception {
		long id = insertEmployee("Айбек");
		mvc.perform(post("/employees/{id}/start", id).param("description", "Вёрстка отчёта"))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attributeCount(0));

		mvc.perform(get("/"))
			.andExpect(content().string(containsString("Вёрстка отчёта")))
			.andExpect(content().string(containsString("Стоп")));
	}

	@Test
	void repeatedStartShowsMessageAndKeepsDescription() throws Exception {
		long id = insertEmployee("Айбек");
		mvc.perform(post("/employees/{id}/start", id).param("description", "Первая"));

		mvc.perform(post("/employees/{id}/start", id).param("description", "Вторая"))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("error", "У сотрудника уже идёт таймер: «Первая» с 27.10.2026 13:00"))
			.andExpect(flash().attribute("description", "Вторая"));
	}

	@Test
	void emptyReportShowsZeroAndNoEntries() throws Exception {
		insertEmployee("Айбек");

		mvc.perform(get("/report").param("from", "2026-10-26").param("to", "2026-10-27"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("За этот период записей нет")))
			.andExpect(content().string(containsString("0 ч 00 мин 00 с")));
	}

	@Test
	void reportWithoutParametersShowsCurrentWeek() throws Exception {
		mvc.perform(get("/report"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"2026-10-26\"")))
			.andExpect(content().string(containsString("value=\"2026-11-01\"")));
	}

	@Test
	void reportShowsRowsWithTimesAndRunningTimer() throws Exception {
		long id = insertEmployee("Айбек");
		// 10:00–11:30 по Бишкеку и идущий таймер с 12:30 (сейчас 13:00)
		insertEntry(id, "Вёрстка", java.time.Instant.parse("2026-10-27T04:00:00Z"),
				java.time.Instant.parse("2026-10-27T05:30:00Z"));
		insertEntry(id, "Ревью", java.time.Instant.parse("2026-10-27T06:30:00Z"), null);

		mvc.perform(get("/report").param("from", "2026-10-27").param("to", "2026-10-27"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("27.10.2026 10:00")))
			.andExpect(content().string(containsString("27.10.2026 11:30")))
			.andExpect(content().string(containsString("1 ч 30 мин 00 с")))
			.andExpect(content().string(containsString("идёт")))
			.andExpect(content().string(containsString("2 ч 00 мин 00 с")));
	}

	@Test
	void stopViaWebStopsTimerAndReportsMissingOne() throws Exception {
		long id = insertEmployee("Айбек");
		mvc.perform(post("/employees/{id}/start", id).param("description", "Задача"));

		mvc.perform(post("/employees/{id}/stop", id))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attributeCount(0));
		mvc.perform(get("/")).andExpect(content().string(not(containsString("Стоп"))));

		mvc.perform(post("/employees/{id}/stop", id))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("error", "Таймер не запущен"));
	}

	@Test
	void extremeDatesShowMessageInsteadOfError() throws Exception {
		mvc.perform(get("/report").param("from", "2026-10-26").param("to", "+999999999-12-31"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Дата должна быть с 2000 по 2100 год")));
		mvc.perform(get("/report").param("from", "+300000-01-01").param("to", "+300000-01-02"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Дата должна быть с 2000 по 2100 год")));
	}

	@Test
	void periodLongerThanYearIsRejected() throws Exception {
		mvc.perform(get("/report").param("from", "2025-01-01").param("to", "2026-06-01"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Период не длиннее года")));
	}

	@Test
	void invalidDateShowsMessageInsteadOfError() throws Exception {
		mvc.perform(get("/report").param("from", "abc"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Неверная дата: «abc»")))
			.andExpect(content().string(not(containsString("Итого по сотрудникам"))));
	}

}
