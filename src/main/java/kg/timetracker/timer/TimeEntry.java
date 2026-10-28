package kg.timetracker.timer;

import java.time.Instant;

import kg.timetracker.employee.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Одна запись времени: над чем работал сотрудник, с какого момента и до какого. */
@Entity
public class TimeEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id")
	private Employee employee;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false)
	private Instant startedAt;

	/** {@code null} — таймер ещё идёт. */
	private Instant endedAt;

	protected TimeEntry() {
	}

	public TimeEntry(Employee employee, String description, Instant startedAt, Instant endedAt) {
		this.employee = employee;
		this.description = description;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
	}

	public Long getId() {
		return id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public String getDescription() {
		return description;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public boolean isRunning() {
		return endedAt == null;
	}

}
