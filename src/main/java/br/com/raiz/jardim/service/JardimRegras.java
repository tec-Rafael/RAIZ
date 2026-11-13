package br.com.raiz.jardim.service;

import br.com.raiz.score.dto.AmsDto.Calculo;

/** Economia independente do AMS: adubo nunca modifica notas ou saúde. */
public final class JardimRegras {
    private JardimRegras() { }
    public static boolean coberturaSuficiente(Calculo c) {
        long registradas=c.dimensoes().stream().filter(d->d.registrada()!=null).count();
        return c.nota()!=null && registradas*4>=c.dimensoesAplicaveis()*3L;
    }
    public static String nivel(Double ams) {
        if(ams==null)return null;
        if(!Double.isFinite(ams)||ams<0||ams>100)throw new IllegalArgumentException("AMS inválido");
        return ams<60?"baixo":ams<70?"medio":ams<85?"alto":"excelente";
    }
    public static int recompensaDiaria(Double ams) {
        return moedas(ams == null ? "baixo" : nivel(ams));
    }
    public static int moedas(String nivel) {
        if(nivel==null)return 0;
        return switch(nivel){case "baixo"->2;case "medio"->4;case "alto"->6;case "excelente"->8;default->throw new IllegalArgumentException("Nível inválido");};
    }
    public static int estagio(int adubos) { return adubos>=16?4:adubos>=8?3:adubos>=3?2:1; }
    public static int metaAdubo(int adubos) { return adubos>=16?16:adubos>=8?16:adubos>=3?8:3; }
    public static int baseAdubo(int adubos) { return adubos>=16?16:adubos>=8?8:adubos>=3?3:0; }
    public static int limiteDecoracoes(int estagio) {
        return estagio == 4 ? Integer.MAX_VALUE : estagio - 1;
    }
    public static int preco(String item) {
        return switch(item){case "adubo"->10;case "flores"->30;case "pedras"->20;case "cogumelos"->40;case "cerca"->35;case "colmeia"->50;case "placa"->25;case "passaro"->45;case "esquilo"->55;case "pinheiro"->60;case "presentes"->30;default->0;};
    }
}
