package kg.timetracker.web;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import kg.timetracker.BusinessException;
import kg.timetracker.Formats;
import kg.timetracker.employee.EmployeeService;
import kg.timetracker.timer.TimeEntry;
import kg.timetracker.timer.TimerFacade;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Главная страница: сотрудники и их таймеры. Каждое действие (POST) заканчивается
 * редиректом на «/» (Post/Redirect/Get): обновление страницы не повторит действие,
 * а результат показывается одноразовым flash-сообщением.
 */
@Controller
class EmployeesController {

	private final EmployeeService employees;

	private final TimerFacade timers;

	private final Clock clock;

	EmployeesController(EmployeeService employees, TimerFacade timers, Clock clock) {
		this.employees = employees;
		this.timers = timers;
		this.clock = clock;
	}

	@GetMapping("/")
	String index(Model model) {
		var running = timers.findAllRunning();
		var now = clock.instant();
		List<EmployeeRow> rows = employees.findAll().stream().map(e -> {
			TimeEntry timer = running.get(e.getId());
			if (timer == null) {
				return new EmployeeRow(e.getId(), e.getName(), null, null, 0);
			}
			// Сколько уже идёт, считает сервер: часы браузера могут отличаться от серверных
			long elapsed = Duration.between(timer.getStartedAt(), now).toSeconds();
			return new EmployeeRow(e.getId(), e.getName(), timer.getDescription(),
					Formats.dateTime(timer.getStartedAt()), Math.max(elapsed, 0));
		}).toList();
		model.addAttribute("rows", rows);
		return "index";
	}

	@PostMapping("/employees")
	String add(@RequestParam(defaultValue = "") String name, RedirectAttributes redirect) {
		try {
			var employee = employees.add(name);
			redirect.addFlashAttribute("message", "Сотрудник «" + employee.getName() + "» добавлен");
		}
		catch (BusinessException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			redirect.addFlashAttribute("name", name);
		}
		return "redirect:/";
	}

	@PostMapping("/employees/{id}/start")
	String start(@PathVariable long id, @RequestParam(defaultValue = "") String description,
			RedirectAttributes redirect) {
		try {
			timers.start(id, description);
		}
		catch (BusinessException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			// Введённое описание возвращается в строку того же сотрудника
			redirect.addFlashAttribute("failedEmployeeId", id);
			redirect.addFlashAttribute("description", description);
		}
		return "redirect:/";
	}

	@PostMapping("/employees/{id}/stop")
	String stop(@PathVariable long id, RedirectAttributes redirect) {
		try {
			timers.stop(id);
		}
		catch (BusinessException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/";
	}

	/** Строка таблицы; {@code task == null} — таймер не идёт. */
	record EmployeeRow(long id, String name, String task, String since, long elapsedSeconds) {
	}

}
