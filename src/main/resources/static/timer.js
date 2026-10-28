// Тикающий счётчик у идущих таймеров. Начальное значение (секунды) посчитал сервер,
// здесь оно только увеличивается раз в секунду: часы браузера в расчёте не участвуют.
for (const el of document.querySelectorAll('.elapsed')) {
	const startedAt = Date.now() - Number(el.dataset.elapsed) * 1000;
	const render = () => {
		const total = Math.floor((Date.now() - startedAt) / 1000);
		const pad = (n) => String(n).padStart(2, '0');
		el.textContent = `${pad(Math.floor(total / 3600))}:${pad(Math.floor(total / 60) % 60)}:${pad(total % 60)}`;
	};
	render();
	setInterval(render, 1000);
}
