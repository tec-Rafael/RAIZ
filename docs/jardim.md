# Jardim RAIZ

A home autenticada representa a jornada individual sobre os dados históricos existentes. Não são criados registros de produtividade nem alteradas as regras AMS. O calendário é explicitamente uma simulação: quatro períodos de 90 dias e o quinto de 66, iniciados no primeiro registro de utilização.

## Saúde, crescimento e economia

A saúde usa o AMS acumulado até o dia explorado: abaixo de 60, seca; de 60 a menos de 70, outono; de 70 a menos de 85, primavera; de 85 em diante, verão. Exige-se cobertura de pelo menos 75% das dimensões aplicáveis com nota registrada. Delivery só participa para desenvolvedores. Sem cobertura, o estado é “Em formação”, sem penalização visual de seca ou recompensa.

Cada dia da jornada concede, uma única vez, 2/4/6/8 moedas conforme esses níveis. Dias sem cobertura concedem zero. O botão +7 dias percorre até sete datas; reproduzir avança uma data por vez e permite pausar. Pré-visualizar datas pelo seletor e slider nunca concede moedas. Não se pode pular a ordem dos períodos na jornada.

A árvore é o elemento principal da home, centralizada horizontalmente, apoiada junto à base do cenário e colorida. O botão translúcido na lateral direita, abaixo do ranking abre a loja, que reúne compra e aplicação de adubo e seis decorações combináveis (flores, pedras, cogumelos, cerca, colmeia e placa). Os sprites são do pacote Tiny Town de Kenney, CC0, armazenados localmente com licença e créditos em frontend/public/img/decoracoes.

Adubo custa 10 moedas. Aplicações acumuladas 3, 8 e 16 liberam os estágios 2, 3 e 4, sem contador de adubo restante na interface. É possível aplicar de 1 a 16 unidades em uma ação, limitado ao estoque e à capacidade da árvore. A aplicação funciona desde o dia zero e em qualquer período selecionado, inclusive arquivado, sem avançar a data ou conceder moedas. No dia zero não há nota antecipada. Em cada dia, a saúde usa o AMS acumulado até a data selecionada, sem dados dos dias seguintes. Aplicar adubo não altera o calendário nem a janela do AMS. A ausência de cobertura suficiente continua indicada como “Em formação”, mas não aplica filtro preto e branco.

“Zerar período” reinicia o crescimento e o calendário do período selecionado e mantém o saldo de moedas. Remove as recompensas daquele período para permitir recebê-las novamente ao avançar os dias. O período volta a ser a jornada atual, aberto no dia zero. O AMS, estoque, itens comprados, decorações e conquistas são preservados; adubo já aplicado não é devolvido. O reset usa a mesma sessão, transação, bloqueio de carteira e verificação de versão das demais ações.

Ranking, loja e progresso ficam em três botões circulares translúcidos, alinhados verticalmente na lateral direita. Progresso reúne saúde, AMS, saldo de moedas, botão de zerar período, calendário e histórico. A home não exibe card de informações sobre o cenário.

Flores custam 30 moedas, pedras 20, cogumelos 40, cerca 35, colmeia 50 e placa 25. Cada decoração pode ser comprada uma vez e depois incluída ou retirada da cena individualmente. Carteira e inventário acompanham o usuário entre períodos. O painel “Períodos” mantém o calendário, a reprodução histórica e a coleção, liberando o centro da tela.


## Dados e limites da reconstrução

O Java reaproveita AmsBatchService com a janela encerrada na data histórica escolhida. No último dia, o resultado corresponde ao cálculo do dashboard. Registros de uso, treinamentos e eventos datados respeitam a janela. Vínculos, preços e metadados de revisão sem histórico usam o cadastro atual; portanto a prévia não é uma reconstituição perfeita de tudo que se sabia naquela data. Essa limitação também está explicada na interface. Sem novos registros, o avanço acontece pelo calendário simulado, não pelo relógio real.

## API e persistência

Todas as rotas exigem a sessão existente de 8 horas. O Node encaminha as requisições ao Java e preserva a proteção de origem. O usuário vem da sessão; IDs enviados no corpo não escolhem o proprietário.

| Método | Rota | Uso |
| --- | --- | --- |
| GET | /api/jardim/estado?periodo=1&dia=15 | Estado ou prévia histórica, sem escrita |
| POST | /api/jardim/avancar | periodo, dias de 1 a 7, versao |
| POST | /api/jardim/comprar | item, quantidade de 1 a 20, versao |
| POST | /api/jardim/adubar | periodo, versao, quantidade opcional (1 por padrão) |
| POST | /api/jardim/zerar | periodo, versao; reinicia crescimento, calendário e recompensas selecionadas; mantém saldo |
| POST | /api/jardim/equipar | item, versao |
| POST | /api/jardim/encerrar | periodo, versao |
| GET | /api/jardim/ranking | periodo, dia, escopo, ordem, grupo, companyId, departmentId |

Cada escrita usa transação, bloqueio da carteira e versão otimista; uma versão vencida retorna 409. Preços, saldo, estoque e estágio são validados no servidor. Um registro único por funcionário e data impede recompensa repetida. Aplicações de adubo guardam o dia do progresso daquele período, inclusive dia zero. Zerar remove as aplicações e recompensas da árvore selecionada, reinicia seu calendário e mantém o saldo de moedas. As tabelas jardim_* são criadas aditivamente por src/main/resources/db/jardim.sql.

O ranking mostra até dez pessoas, a posição do usuário e uma floresta coletiva. Compara AMS ou diferença frente ao mesmo dia do período anterior; P1 não inventa evolução. Perfis comuns e gestores consultam resumos da própria empresa e do próprio departamento. Administradores podem selecionar empresa e área. Os resumos não expõem e-mail, ID ou dimensões dos colegas e não liberam seus dashboards individuais. Filtros de desenvolvedores/demais profissionais ajudam na comparação entre quatro e cinco dimensões.

## Execução e verificação

Reinicie o processo Node após atualizar para registrar o proxy /api/jardim. A API deve ser reconstruída ao mudar Java; o banco e volumes existentes são preservados.

Execute mvn test com JDK 21. No frontend, npm run test:jardim usa API e banco reais, com as contas de demonstração de test/contas-demo.json. DASHBOARD_URL permite outra porta do Node, desde que conste em AUTH_ORIGINS da API. Esse teste avança o jogo e gasta moedas/adubo da conta demonstrativa, preservando métricas AMS. npm run test:loja valida compra, aplicação em lote, decorações simultâneas com banco real usando Valentina; esse teste altera apenas a jornada demonstrativa dela. npm run test:jardim:reset verifica preservação de saldo, retorno ao dia zero e nova recompensa dos mesmos dias. npm run test:jardim:ui valida o comportamento da loja. JARDIM_CICLO_COMPLETO=1 também completa e arquiva o período atual para verificar a próxima muda. Nunca use uma conta cujo progresso deseje manter intacto.

Foram verificados: 100 testes Java, integração de saldo e concorrência, igualdade do AMS com o dashboard, rejeição de origem indevida e acesso a outra empresa/área, além da renderização das views e revisão visual da home e dos painéis.

## Floresta de demonstração e datas

A carga manual docs/sql/semear-floresta-demo.sql acrescenta estágios de crescimento a quatro colegas por departamento em cada período. Preserva as quatro contas de apresentação e qualquer árvore já existente; não altera métricas AMS, saldo ou autenticação. A carga é idempotente e não roda ao iniciar a aplicação. A floresta mostra até 12 árvores, priorizando crescimento; a ordenação do ranking continua definida pelo AMS ou evolução. No dia zero, participantes e estágios aparecem com AMS em formação.

POST /api/jardim/adubar aceita também dia opcional (0 até o fim do período) para manter a data explorada. Sem esse campo, usa o dia salvo. Adubo modifica apenas inventário e crescimento naquela data, sem avançar dias ou premiar moedas. Abrir a loja pausa a reprodução automática do calendário.

O estágio salvo é mostrado imediatamente ao carregar a home. Animações usam tempo decorrido e têm espera limitada, evitando bloquear os controles em abas lentas. Árvores arquivadas são apresentadas com as faixas de saúde atuais.
