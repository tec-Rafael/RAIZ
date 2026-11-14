# Agentes de IA do rAIz

São quatro agentes, cada um uma API REST (FastAPI), que se comunicam por HTTP com JSON. O modelo de linguagem é usado **só para inferência**: não há treino nem fine-tuning do modelo.

O provedor da LLM é configurável pela variável `LLM_PROVEDOR`:
- **`claude`** (padrão): API da Anthropic, com `claude-haiku-5-5`, esforço `low` e raciocínio desligado, para responder rápido. A Anthropic não tem embeddings, então o RAG usa busca por palavras.
- **`gemini`**: API do Google AI Studio, com `gemini-2.5-flash-lite` e embeddings `gemini-embedding-001`.
- **`ollama`**: local, com `qwen2.5:7b` e `nomic-embed-text`. É gratuito, mas lento sem placa de vídeo.

```
Navegador ──► Node (/api/assistente) ──► Java (/api/assistente/perguntar: sessão + regras de acesso + AMS)
                                              │  POST /perguntar  (X-Agentes-Token)
                                              ▼
                                    agente-orquestrador ──► LLM: Claude ou Gemini (API) ou Ollama (local)
                                     │        │        │
                       POST /contexto│  POST /perfil   │POST /buscar
                                     ▼        ▼        ▼
                              agente-dados  agente-perfil  agente-conhecimento ──► embeddings (Gemini/Ollama) ou palavras
                                 (uso)       (ML: perfis    (RAG: docs/*.md)
                                             e cursos)
                                     └────── Postgres (somente leitura) ──────┘
```

| Agente | Porta interna | Endpoint | O que faz |
|---|---|---|---|
| orquestrador | 8000 (8001 no host) | `POST /perguntar` | Recebe a pergunta do Java, chama os outros três em paralelo, monta o prompt e chama a LLM. |
| dados | 8000 | `POST /contexto` | Métricas de uso de IA do recorte: requisições, custo, falhas, ferramentas, comparação com o período anterior. |
| perfil | 8000 | `POST /perfil`, `POST /treinar` | Perfil de uso (k-means), pontos de atenção e cursos recomendados. Treina o modelo na primeira subida. |
| conhecimento | 8000 | `POST /buscar` | RAG: trechos de `docs/*.md` mais parecidos com a pergunta (embeddings do provedor; sem eles, busca por palavras). |

Todos têm `GET /saude`. As chamadas `POST` exigem o cabeçalho `X-Agentes-Token`, com o mesmo valor de `AGENTES_TOKEN` configurado no Java.

## Segurança
- **Quem pode ver o quê** é decidido só pelo **Java**, com as mesmas regras do dashboard. Os agentes recebem o recorte já autorizado (`employeeIds`) e nunca decidem permissões.
- Os agentes leem o banco em transação **somente leitura**.
- A LLM só vê o contexto montado pelo orquestrador; ela não acessa o banco.

## Subir
1. Copie `.env.example` para `.env`, na raiz do projeto, e cole a chave da API da Anthropic em `ANTHROPIC_API_KEY`. A chave é criada em platform.claude.com, em Settings → API Keys, e a conta precisa ter créditos. O `.env` está no `.gitignore`: a chave nunca vai para o Git.
2. Rode:
```bash
docker compose up -d --build
```
- **Limites:** quando a conta bate no limite de requisições, o chat mostra "Muitas perguntas em pouco tempo". Se os créditos acabarem, mostra "O assistente está indisponível"; o log do agente-orquestrador traz o motivo.
- **Uso dos dados:** pelos termos comerciais da Anthropic, ela não treina modelos com as entradas e saídas da API.
- **Para usar o Gemini:** defina `LLM_PROVEDOR=gemini` e `GEMINI_API_KEY`. Use a camada paga se houver dados reais.
- **Para usar o Ollama local:** defina `LLM_PROVEDOR=ollama` e suba com `docker compose --profile ollama up -d --build`. Na primeira vez, o perfil baixa o `qwen2.5:7b` (~4,7 GB). Se o Ollama estiver instalado no Windows, use `OLLAMA_URL=http://host.docker.internal:11434` e não precisa do perfil.

## Testar sem o Java
```bash
curl http://localhost:8001/saude
curl -X POST http://localhost:8001/perguntar -H "Content-Type: application/json" -H "X-Agentes-Token: raiz-agentes-local" \
  -d '{"pergunta":"Como foi meu uso de IA e como melhorar?","usuario":{"nome":"Ana","perfil":"colaborador"},"periodo":5,
       "escopo":{"tipo":"pessoal","descricao":"seus dados","proprio":true,"employeeIds":["C01-E0001"]}}'
```

## Testes automáticos (sem banco e sem Ollama)
```bash
pip install -r requirements.txt
python -m pytest -q tests
```

## Base de conhecimento (RAG no banco da empresa)
O agente de conhecimento responde **só** com o que está no banco da empresa, nunca com conteúdo da internet.
- **Políticas de uso de IA**: um arquivo por empresa em `politicas/<company_id>.md`. Hoje são fictícias, escritas para o case a partir das ferramentas aprovadas, em avaliação e bloqueadas de cada empresa na base. `politicas/_gerar_politicas.py` gera de novo.
- **Metodologia do AMS**: `docs/metodologia-ams.md`, válida para todas as empresas.

**Carga** (serviço `conhecimento-carga`, roda a cada `docker compose up` e só regrava o que mudou): divide cada documento em trechos, um por seção `##`/`###`, gera o embedding de cada trecho e grava nas tabelas `conhecimento_documentos` e `conhecimento_trechos`. O vetor fica numa coluna `REAL[]`: não precisa de pgvector, e a similaridade (cosseno) é calculada no agente.

**Embeddings** (`EMBEDDINGS_PROVEDOR`):
- `local` (padrão): modelo multilíngue `paraphrase-multilingual-MiniLM-L12-v2` (fastembed/ONNX), baixado no build da imagem. Em execução, o texto não sai do servidor.
- Também aceita `ollama` ou `gemini`. `hash` serve só para testes.

**Na pergunta:**
- a metodologia vai inteira (`RAG_MODO=completo`) ou por busca (`RAG_MODO=busca`);
- as políticas vêm sempre pela **busca vetorial**, filtrada pelas empresas do recorte que o Java autorizou. Um colaborador nunca recebe trechos da política de outra empresa.
- `RAG_TRECHOS_POLITICA` define quantos trechos de política vão no prompt (padrão: 4).

Para incluir ou alterar uma política, edite o `.md` e rode `docker compose up -d conhecimento-carga`. O agente recarrega a base a cada 5 minutos, ou na hora com `POST /recarregar`.
