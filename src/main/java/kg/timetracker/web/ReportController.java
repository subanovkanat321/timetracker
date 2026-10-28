package kg.timetracker.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import kg.timetracker.BusinessException;
import kg.timetracker.report.ReportService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class ReportController {

	private final ReportService reports;

	ReportController(ReportService reports) {
		this.reports = reports;
	}

	/**
	 * Даты приходят строками из {@code <input type="date">} (2026-10-28). Пустая дата заменяется
	 * границей текущей недели; неверная — сообщение на странице, а не страница ошибки 400.
	 */
	@GetMapping("/report")
	String report(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			Model model) {
		var week = reports.currentWeek();
		model.addAttribute("from", isBlank(from) ? week.from().toString() : from);
		model.addAttribute("to", isBlank(to) ? week.to().toString() : to);
		try {
			model.addAttribute("report", reports.build(parse(from, week.from()), parse(to, week.to())));
		}
		catch (BusinessException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "report";
	}

	private static LocalDate parse(String value, LocalDate fallback) {
		if (isBlank(value)) {
			return fallback;
		}
		try {
			return LocalDate.parse(value.strip());
		}
		catch (DateTimeParseException e) {
			throw new BusinessException("Неверная дата: «" + value + "». Формат — ГГГГ-ММ-ДД");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
