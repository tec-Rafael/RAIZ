package br.com.raiz.avatar.dto;

/** Configuração do avatar (JSON versao 2, o mesmo formato usado por public/js/avatarSprite.js). */
public final class AvatarDto {
    private AvatarDto() { }
    public record Avatar(Integer versao, String genero, String pele, String olhos, String cabelo, String corCabelo,
                         String facial, Boolean sardas, String camisa, String calca) { }
    /** origem: "sorteado" (gerado para o colaborador), "escolhido" (editado por ele) ou "fixo" (administrador). */
    public record Resposta(Avatar avatar, String origem) { }
}
