package kg.timetracker;

/** Ошибка, которую видит пользователь: текст сообщения показывается на странице как есть. */
public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}

}
