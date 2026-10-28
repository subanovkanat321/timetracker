package kg.timetracker.employee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	@Query("select e from Employee e order by lower(e.name)")
	List<Employee> findAllByName();

}
