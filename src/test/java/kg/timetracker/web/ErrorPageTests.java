package kg.timetracker.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import kg.timetracker.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Страница ошибки — на настоящем сервере: MockMvc не делает servlet-перенаправление на /error,
 * и шаблон error.html в нём не отрисовывается.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ErrorPageTests {

	@LocalServerPort
	int port;

	@Test
	void badRequestShowsRussianErrorPage() throws Exception {
		var response = send(HttpRequest.newBuilder(uri("/employees/abc/start"))
			.POST(HttpRequest.BodyPublishers.noBody()));

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("Что-то пошло не так", "Неверный запрос", "На главную")
			.doesNotContain("Whitelabel");
	}

	@Test
	void unknownPageShowsNotFound() throws Exception {
		var response = send(HttpRequest.newBuilder(uri("/nope")).GET());

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.body()).contains("Такой страницы нет");
	}

	private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		try (var client = HttpClient.newHttpClient()) {
			return client.send(request.header("Accept", "text/html").build(), HttpResponse.BodyHandlers.ofString());
		}
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

}
