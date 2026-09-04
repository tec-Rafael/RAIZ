package br.com.raiz.usuario.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A coluna employees.posicao guarda os valores em minúsculo ("gestor",
 * "colaborador"), conforme definido em CriacaoPosicao.sql e replicado por
 * convert_mysql_to_postgres.py. Este converter mantém o Enum Java em
 * maiúsculo (legível e seguro no código) enquanto permanece 100%
 * compatível com os dados reais já existentes no banco.
 */
@Converter(autoApply = true)
public class PosicaoConverter implements AttributeConverter<Posicao, String> {

    @Override
    public String convertToDatabaseColumn(Posicao posicao) {
        return posicao == null ? null : posicao.name().toLowerCase();
    }

    @Override
    public Posicao convertToEntityAttribute(String valorNoBanco) {
        return valorNoBanco == null ? null : Posicao.valueOf(valorNoBanco.trim().toUpperCase());
    }
}
