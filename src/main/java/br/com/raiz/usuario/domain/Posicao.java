package br.com.raiz.usuario.domain;

/**
 * Posição do funcionário no sistema.
 *
 * Apenas GESTOR pode cadastrar novos funcionários (ver FuncionarioService.criar).
 */
public enum Posicao {
    COLABORADOR,
    GESTOR
}
