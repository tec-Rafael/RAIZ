"""Configuração por variáveis de ambiente (docker-compose)."""
import os


def _env(nome: str, padrao: str) -> str:
    return os.environ.get(nome, padrao).strip()


BANCO_URL = _env("BANCO_URL", "postgresql+psycopg2://root:root@localhost:5433/ai_usage")
# Provedor da LLM: "claude" (API da Anthropic), "gemini" (API do Google AI Studio) ou "ollama" (local).
# Troca sem mexer no código.
LLM_PROVEDOR = _env("LLM_PROVEDOR", "claude").lower()
ANTHROPIC_API_KEY = _env("ANTHROPIC_API_KEY", "").strip('"\'')
# Só para chaves sem espaço de trabalho (escopo "Organização" no Console): id no formato wrkspc_...
ANTHROPIC_WORKSPACE_ID = _env("ANTHROPIC_WORKSPACE_ID", "").strip('"\'')
CLAUDE_MODELO = _env("CLAUDE_MODELO", "claude-haiku-5-5")
ANTHROPIC_URL = _env("ANTHROPIC_URL", "https://api.anthropic.com").rstrip("/")
ANTHROPIC_VERSION = _env("ANTHROPIC_VERSION", "2026-10-21")
GEMINI_API_KEY = _env("GEMINI_API_KEY", "")
GEMINI_MODELO = _env("GEMINI_MODELO", "gemini-2.5-flash-lite")
GEMINI_EMBEDDINGS = _env("GEMINI_EMBEDDINGS", "gemini-embedding-001")
GEMINI_URL = _env("GEMINI_URL", "https://generativelanguage.googleapis.com/v1beta").rstrip("/")
LLM_TIMEOUT = float(_env("LLM_TIMEOUT", "30"))
OLLAMA_URL = _env("OLLAMA_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODELO = _env("OLLAMA_MODELO", "qwen2.5:7b")
OLLAMA_EMBEDDINGS = _env("OLLAMA_EMBEDDINGS", "nomic-embed-text")
OLLAMA_TIMEOUT = float(_env("OLLAMA_TIMEOUT", "170"))
# Token compartilhado entre o Java e os agentes: os agentes só aceitam chamadas que o apresentem.
AGENTES_TOKEN = _env("AGENTES_TOKEN", "troque-este-token")
AGENTE_DADOS_URL = _env("AGENTE_DADOS_URL", "http://localhost:8002").rstrip("/")
AGENTE_PERFIL_URL = _env("AGENTE_PERFIL_URL", "http://localhost:8003").rstrip("/")
AGENTE_CONHECIMENTO_URL = _env("AGENTE_CONHECIMENTO_URL", "http://localhost:8004").rstrip("/")
PASTA_DADOS = _env("PASTA_DADOS", "./var")          # modelo treinado e cache de embeddings
# RAG: a metodologia do AMS vai inteira em cada pergunta ("completo") ou só os trechos mais parecidos ("busca").
# As políticas das empresas sempre usam a busca vetorial no banco, filtrada pelas empresas do recorte.
RAG_MODO = _env("RAG_MODO", "completo").lower()
RAG_TRECHOS_POLITICA = int(_env("RAG_TRECHOS_POLITICA", "4"))
# Embeddings (busca vetorial): "local" roda um modelo multilíngue dentro do agente (fastembed/ONNX), sem enviar
# o texto para fora; "ollama" e "gemini" usam esses provedores; "hash" é só para testes.
EMBEDDINGS_PROVEDOR = _env("EMBEDDINGS_PROVEDOR", "local").lower()
EMBEDDINGS_MODELO = _env("EMBEDDINGS_MODELO", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
EMBEDDINGS_CACHE = _env("EMBEDDINGS_CACHE", "./var/modelos")      # onde o modelo local fica salvo (na imagem: /app/modelos)
PASTA_POLITICAS = _env("PASTA_POLITICAS", os.path.join(os.path.dirname(__file__), "..", "politicas"))
PASTA_DOCS = _env("PASTA_DOCS", os.path.join(os.path.dirname(__file__), "..", "docs"))
