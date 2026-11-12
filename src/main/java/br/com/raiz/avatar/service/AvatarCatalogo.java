package br.com.raiz.avatar.service;

import br.com.raiz.avatar.dto.AvatarDto.Avatar;
import java.util.List;
import java.util.Set;

/**
 * Opções do avatar (as mesmas de public/js/avatarSprite.js) e o sorteio inicial de cada colaborador.
 * O sorteio é determinístico a partir do identificador e da tentativa, com o mesmo algoritmo do
 * protótipo em JavaScript (FNV-1a + mulberry32): o mesmo colaborador recebe sempre o mesmo resultado.
 * Combinações naturais: barba/bigode só no corpo masculino e cores de cabelo naturais na maioria.
 */
public final class AvatarCatalogo {
    private AvatarCatalogo() { }

    public static final String ADMINISTRADOR = "ROOT-E0001";
    /** Avatar fixo do administrador (o mesmo da animação do login). */
    public static final Avatar AVATAR_ADMINISTRADOR = new Avatar(2,"masculino","p2","o5","espetado","c5","nenhum",false,"r10","k3");

    public static final Set<String> GENEROS = Set.of("masculino","feminino");
    public static final Set<String> CABELOS = Set.of("careca","curto","topete","espetado","cacheado","chanel","longo","rabo","coque","moicano");
    public static final Set<String> FACIAIS = Set.of("nenhum","bigode","cavanhaque","barbaCurta","barbaCheia");

    private record Peso(String valor, int peso) { }
    private static final List<Peso> CAB_M = List.of(new Peso("curto",5),new Peso("topete",4),new Peso("espetado",3),new Peso("careca",2),new Peso("cacheado",3),
        new Peso("moicano",1),new Peso("rabo",1),new Peso("longo",1),new Peso("chanel",1),new Peso("coque",1));
    private static final List<Peso> CAB_F = List.of(new Peso("longo",5),new Peso("chanel",4),new Peso("rabo",4),new Peso("coque",3),new Peso("cacheado",4),
        new Peso("curto",2),new Peso("topete",1),new Peso("espetado",1),new Peso("moicano",1),new Peso("careca",1));
    private static final List<Peso> COR = List.of(new Peso("c1",6),new Peso("c2",7),new Peso("c3",7),new Peso("c4",5),new Peso("c5",4),new Peso("c6",1),
        new Peso("c7",2),new Peso("c9",2),new Peso("c8",1),new Peso("c10",1),new Peso("c11",1),new Peso("c12",1));
    private static final List<Peso> OLHOS = List.of(new Peso("o1",6),new Peso("o2",3),new Peso("o3",3),new Peso("o4",2),new Peso("o5",2),new Peso("o6",1),new Peso("o7",1),new Peso("o8",1));
    private static final List<Peso> FACIAL = List.of(new Peso("nenhum",6),new Peso("bigode",1),new Peso("cavanhaque",1),new Peso("barbaCurta",2),new Peso("barbaCheia",1));

    /** Valida uma configuração recebida do cliente; devolve a mensagem de erro ou null. */
    public static String validar(Avatar a) {
        if (a == null) return "Envie a configuração do avatar.";
        if (!GENEROS.contains(a.genero())) return "Corpo inválido.";
        if (!faixa(a.pele(),"p",8)) return "Cor de pele inválida.";
        if (!faixa(a.olhos(),"o",8)) return "Cor dos olhos inválida.";
        if (!CABELOS.contains(a.cabelo())) return "Estilo de cabelo inválido.";
        if (!faixa(a.corCabelo(),"c",12)) return "Cor do cabelo inválida.";
        if (!FACIAIS.contains(a.facial())) return "Barba ou bigode inválido.";
        if (a.sardas() == null) return "Informe se o avatar tem sardas.";
        if (!faixa(a.camisa(),"r",12)) return "Cor da camisa inválida.";
        if (!faixa(a.calca(),"k",8)) return "Cor da calça inválida.";
        return null;
    }
    private static boolean faixa(String v, String prefixo, int max) {
        if (v == null || !v.matches(prefixo + "[1-9][0-9]?")) return false;
        int n = Integer.parseInt(v.substring(1)); return n >= 1 && n <= max;
    }

    /** Chave de comparação: dois avatares com a mesma chave são visualmente iguais. */
    public static String chave(Avatar a) {
        return String.join("|",a.genero(),a.pele(),a.olhos(),a.cabelo(),a.corCabelo(),a.facial(),String.valueOf(a.sardas()),a.camisa(),a.calca());
    }

    /** Sorteio determinístico: (identificador, tentativa) -> avatar. A tentativa muda o resultado quando ele já está em uso. */
    public static Avatar sortear(String employeeId, int tentativa) {
        var r = new Aleatorio(fnv(employeeId + "#" + tentativa));
        String genero = r.proximo() < 0.5 ? "masculino" : "feminino";
        String pele = "p" + (1 + (int) Math.floor(r.proximo() * 8));
        String olhos = peso(r, OLHOS);
        String cabelo = peso(r, genero.equals("masculino") ? CAB_M : CAB_F);
        String corCabelo = peso(r, COR);
        String facial = genero.equals("masculino") ? peso(r, FACIAL) : "nenhum";
        boolean sardas = r.proximo() < 0.2;
        String camisa = "r" + (1 + (int) Math.floor(r.proximo() * 12));
        String calca = "k" + (1 + (int) Math.floor(r.proximo() * 8));
        return new Avatar(2,genero,pele,olhos,cabelo,corCabelo,facial,sardas,camisa,calca);
    }
    private static String peso(Aleatorio r, List<Peso> itens) {
        int total = 0; for (var p : itens) total += p.peso();
        double x = r.proximo() * total;
        for (var p : itens) { x -= p.peso(); if (x < 0) return p.valor(); }
        return itens.get(itens.size() - 1).valor();
    }
    /** FNV-1a de 32 bits sobre as unidades UTF-16 (igual a String.charCodeAt no JavaScript). */
    static int fnv(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) { h ^= s.charAt(i); h *= 0x01000193; }
        return h;
    }
    /** mulberry32: gerador pequeno e reprodutível (mesma sequência do JavaScript). */
    static final class Aleatorio {
        private int a;
        Aleatorio(int semente) { this.a = semente; }
        double proximo() {
            a += 0x6D2B79F5; int t = a;
            t = (t ^ (t >>> 15)) * (t | 1);
            t ^= t + (t ^ (t >>> 7)) * (t | 61);
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
        }
    }
}
