"""Perfis de uso de IA (k-means) e recomendações de melhoria.

Treino: uma linha por funcionário × período com uso de IA. Quem não usou IA no período recebe o perfil
"sem_uso" por regra, sem passar pelo modelo. Os nomes dos perfis são dados pelos centroides (e não pelo
número do cluster), então continuam corretos se o modelo for treinado de novo.
"""
from dataclasses import dataclass, field

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler

FEATURES = ["requisicoes", "dias_ativos", "tokens_por_requisicao", "custo_por_requisicao", "taxa_falha",
            "taxa_limite", "ferramentas", "aplicacoes", "parcela_dev", "parcela_nao_aprovada"]

PERFIS = {
    "dev_intensivo": {
        "nome": "Desenvolvimento intensivo",
        "descricao": "Usa IA todos os dias em várias ferramentas, com forte presença de assistentes de código.",
        "recomendacoes": ["Mantenha o uso nas ferramentas aprovadas e acompanhe a taxa de falhas das integrações.",
                          "Compartilhe boas práticas com a equipe: seu padrão de uso é referência de adoção."],
    },
    "corporativo_custo_alto": {
        "nome": "Uso corporativo frequente e caro",
        "descricao": "Usa IA com frequência em apps de escritório, mas com custo por requisição alto.",
        "recomendacoes": ["Para tarefas simples (resumos, revisões curtas), prefira modelos mais leves.",
                          "Concentre o uso nas ferramentas aprovadas pela empresa."],
    },
    "fora_da_politica": {
        "nome": "Uso fora da política",
        "descricao": "Uso leve, com boa parte das requisições em ferramentas não aprovadas para a empresa.",
        "recomendacoes": ["Migre para as ferramentas aprovadas: o uso fora da política pesa na governança do AMS.",
                          "Faça um curso de Governança ou de Segurança e Compliance."],
    },
    "esporadico": {
        "nome": "Uso esporádico",
        "descricao": "Usa IA poucas vezes no período e em poucas aplicações.",
        "recomendacoes": ["Experimente a IA em uma tarefa recorrente da semana para criar o hábito.",
                          "Um curso de IA Generativa ou de Produtividade ajuda a encontrar onde ela rende mais."],
    },
    "sem_uso": {
        "nome": "Sem uso registrado",
        "descricao": "Nenhuma requisição de IA registrada no período.",
        "recomendacoes": ["Comece por uma ferramenta aprovada pela empresa em uma tarefa simples do dia a dia.",
                          "O curso de Fundamentos de IA Generativa é um bom ponto de partida."],
    },
}


def _br(valor, casas=1) -> str:
    return f"{valor:,.{casas}f}".replace(",", "§").replace(".", ",").replace("§", ".")


def _matriz(df: pd.DataFrame) -> pd.DataFrame:
    x = df[FEATURES].astype(float).copy()
    x["requisicoes"] = np.log1p(x["requisicoes"])
    x["custo_por_requisicao"] = np.log1p(x["custo_por_requisicao"] * 1000)
    return x


def _nomear(centros: pd.DataFrame) -> dict[int, str]:
    """Dá nome a cada cluster pelo centroide (em unidades originais)."""
    restantes = list(centros.index)
    nomes = {}
    def escolher(chave, coluna, maior=True):
        alvo = (centros.loc[restantes, coluna].idxmax() if maior else centros.loc[restantes, coluna].idxmin())
        nomes[alvo] = chave; restantes.remove(alvo)
    escolher("dev_intensivo", "parcela_dev")
    escolher("fora_da_politica", "parcela_nao_aprovada")
    escolher("esporadico", "requisicoes", maior=False)
    for c in restantes:
        nomes[c] = "corporativo_custo_alto"
    return nomes


@dataclass
class ModeloPerfis:
    escala: StandardScaler
    kmeans: KMeans
    nomes: dict[int, str]
    referencias: dict = field(default_factory=dict)   # medianas e quartis por perfil, para comparar a pessoa
    metricas: dict = field(default_factory=dict)

    @classmethod
    def treinar(cls, df: pd.DataFrame, k: int = 4, semente: int = 7) -> "ModeloPerfis":
        uso = df[df["usou_ia"]]
        x = _matriz(uso)
        escala = StandardScaler().fit(x)
        xs = escala.transform(x)
        km = KMeans(n_clusters=k, n_init=20, random_state=semente).fit(xs)
        centros = uso.assign(c=km.labels_).groupby("c")[FEATURES].median()
        nomes = _nomear(centros)
        rotulado = uso.assign(perfil=[nomes[c] for c in km.labels_])
        refs = {p: {"mediana": g[FEATURES].median().to_dict(), "p25": g[FEATURES].quantile(.25).to_dict(),
                    "p75": g[FEATURES].quantile(.75).to_dict(), "n": int(len(g))}
                for p, g in rotulado.groupby("perfil")}
        refs["_geral"] = {"mediana": uso[FEATURES].median().to_dict(), "p75": uso[FEATURES].quantile(.75).to_dict()}
        sil = float(silhouette_score(xs, km.labels_, sample_size=min(3000, len(xs)), random_state=1))
        return cls(escala, km, nomes, refs, {"silhueta": round(sil, 3), "k": k, "linhas_treino": int(len(uso))})

    def atribuir(self, df: pd.DataFrame) -> pd.Series:
        perfil = pd.Series("sem_uso", index=df.index, dtype=object)
        uso = df["usou_ia"]
        if uso.any():
            rot = self.kmeans.predict(self.escala.transform(_matriz(df[uso])))
            perfil[uso] = [self.nomes[c] for c in rot]
        return perfil

    def explicar(self, linha: pd.Series) -> dict:
        """Perfil + pontos de atenção individuais (comparando a pessoa com o próprio perfil e com o geral)."""
        perfil = self.atribuir(linha.to_frame().T.astype({"usou_ia": bool})).iloc[0]
        info = PERFIS[perfil]
        atencao = []
        if perfil != "sem_uso":
            ref, geral = self.referencias[perfil], self.referencias["_geral"]
            if linha["taxa_falha"] > max(geral["p75"]["taxa_falha"], 0.05):
                atencao.append(("taxa_falha", f"{_br(linha['taxa_falha'] * 100, 1)}% das requisições falharam (erro ou tempo esgotado), acima do comum."))
            if linha["taxa_limite"] > 0.02:
                atencao.append(("taxa_limite", f"{_br(linha['taxa_limite'] * 100, 1)}% das requisições bateram no limite de uso da ferramenta."))
            if linha["parcela_nao_aprovada"] > 0:
                atencao.append(("parcela_nao_aprovada", f"{_br(linha['parcela_nao_aprovada'] * 100, 0)}% do uso foi em ferramentas não aprovadas pela empresa."))
            if linha["custo_por_requisicao"] > 1.5 * ref["mediana"]["custo_por_requisicao"]:
                atencao.append(("custo_por_requisicao", f"Custo médio de US$ {_br(linha['custo_por_requisicao'], 3)} por requisição, bem acima de pessoas com o mesmo perfil (US$ {_br(ref['mediana']['custo_por_requisicao'], 3)})."))
            if linha["dias_ativos"] < ref["p25"]["dias_ativos"]:
                atencao.append(("dias_ativos", f"Usou IA em {int(linha['dias_ativos'])} dias, menos que a maioria do seu perfil ({int(ref['mediana']['dias_ativos'])} dias)."))
        if linha.get("eventos_seguranca", 0) > 0:
            atencao.append(("eventos_seguranca", f"{int(linha['eventos_seguranca'])} evento(s) de segurança registrado(s) no período."))
        return {"perfil": perfil, "nome": info["nome"], "descricao": info["descricao"],
                "recomendacoes": info["recomendacoes"], "pontos_de_atencao": [t for _, t in atencao],
                "codigos_atencao": [c for c, _ in atencao]}
