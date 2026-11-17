"""Agentes de IA do rAIz: cada agente é uma API REST (FastAPI) com uma responsabilidade.

orquestrador → recebe a pergunta (vinda do Java), consulta os outros agentes por HTTP e chama o Ollama.
dados        → métricas de uso de IA do recorte autorizado.
perfil       → perfil de uso (machine learning), pontos de atenção e cursos recomendados.
conhecimento → busca trechos dos documentos de apoio (RAG).
"""
