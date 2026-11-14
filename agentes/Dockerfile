# Uma imagem para os agentes e para a carga da base de conhecimento; o docker-compose escolhe o comando.
FROM python:3.12-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PASTA_DADOS=/app/var PASTA_DOCS=/app/docs \
    PASTA_POLITICAS=/app/politicas EMBEDDINGS_CACHE=/app/modelos
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
# Baixa o modelo de embeddings no build: em execução, o texto das políticas e das perguntas não sai do servidor.
ARG EMBEDDINGS_MODELO=sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
RUN python -c "from fastembed import TextEmbedding; TextEmbedding('${EMBEDDINGS_MODELO}', cache_dir='/app/modelos')"
COPY raiz_agentes ./raiz_agentes
COPY docs ./docs
COPY politicas ./politicas
RUN useradd --create-home agente && mkdir -p /app/var && chown -R agente /app/var /app/modelos
USER agente
EXPOSE 8000
CMD ["uvicorn", "raiz_agentes.orquestrador.app:app", "--host", "0.0.0.0", "--port", "8000"]
