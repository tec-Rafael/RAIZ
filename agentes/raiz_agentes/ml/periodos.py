"""Períodos do AMS, iguais aos do Java (PeriodoAMS): janelas de 90 dias a partir de 01/06/2025, até 31/07/2026."""
from datetime import date, timedelta

INICIO_BASE = date(2025, 6, 1)
FIM_BASE = date(2026, 7, 31)
TOTAL = 5


def periodo(numero: int) -> tuple[date, date]:
    if not 1 <= numero <= TOTAL:
        raise ValueError("Período inválido. Informe um número entre 1 e 5.")
    inicio = INICIO_BASE + timedelta(days=(numero - 1) * 90)
    return inicio, min(inicio + timedelta(days=89), FIM_BASE)
