import java.nio.file.*;
import java.util.*;

public class BuddyAlocator {

    static final int KB = 1024;
    static final int TAM_TOTAL = 4 * KB * KB;
    static final int ORDEM_MIN = 10;
    static final int ORDEM_MAX = 22;
    static final int NUM_ORDENS = (ORDEM_MAX - ORDEM_MIN) + 1;
    static final int TAM_ARVORE = 1 << (NUM_ORDENS + 1);

    static final int LIVRE = 0;
    static final int DIVIDIDO = 1;
    static final int OCUPADO = 2;

    int[] estadoNo;
    String[] rotuloNo;
    int[] pedidoNo;

    public BuddyAlocator() {
        estadoNo = new int[TAM_ARVORE];
        rotuloNo = new String[TAM_ARVORE];
        pedidoNo = new int[TAM_ARVORE];
    }

    int ordemParaDepth(int depth) {
        return ORDEM_MAX - depth;
    }

    int tamanhoBlocoParaDepth(int depth) {
        int ordem = ordemParaDepth(depth);
        return 1 << ordem;
    }

    int depthParaIndice(int indice) {
        int d = 0;
        int val = 1;
        while (val <= indice) {
            val = val << 1;
            d = d + 1;
        }
        return d - 1;
    }

    int ordemNecessaria(int sizeBytes) {
        int o = ORDEM_MIN;
        int bs = 1 << o;
        while (bs < sizeBytes && o <= ORDEM_MAX) {
            o = o + 1;
            bs = 1 << o;
        }
        if (o > ORDEM_MAX) return -1;
        return o;
    }

    public int alocar(String rotulo, int tamanhoBytes) {
        int ordReq = ordemNecessaria(tamanhoBytes);
        if (ordReq == -1) return -1;
        int depthReq = ORDEM_MAX - ordReq;
        return buscarAlocar(1, 0, depthReq, rotulo, tamanhoBytes);
    }

    int buscarAlocar(int indice, int depthAtual, int depthReq, String rotulo, int tamanhoBytes) {
        if (indice <= 0 || indice >= TAM_ARVORE) return -1;
        int estado = estadoNo[indice];
        if (estado == OCUPADO) return -1;
        int ordemAtual = ordemParaDepth(depthAtual);
        if (ordemAtual < ORDEM_MIN || ordemAtual > ORDEM_MAX) return -1;

        if (estado == LIVRE) {
            if (depthAtual == depthReq) {
                estadoNo[indice] = OCUPADO;
                rotuloNo[indice] = rotulo;
                pedidoNo[indice] = tamanhoBytes;
                return indice;
            } else if (depthAtual < depthReq) {
                estadoNo[indice] = DIVIDIDO;
                int fe = indice << 1;
                int fd = fe + 1;
                estadoNo[fe] = LIVRE;
                estadoNo[fd] = LIVRE;
                int r = buscarAlocar(fe, depthAtual + 1, depthReq, rotulo, tamanhoBytes);
                if (r != -1) return r;
                return buscarAlocar(fd, depthAtual + 1, depthReq, rotulo, tamanhoBytes);
            } else {
                return -1;
            }
        } else {
            int fe = indice << 1;
            int fd = fe + 1;
            int r = buscarAlocar(fe, depthAtual + 1, depthReq, rotulo, tamanhoBytes);
            if (r != -1) return r;
            return buscarAlocar(fd, depthAtual + 1, depthReq, rotulo, tamanhoBytes);
        }
    }

    boolean temAncestralLivre(int indice) {
        int pai = indice >> 1;
        while (pai >= 1) {
            if (estadoNo[pai] == LIVRE) return true;
            pai = pai >> 1;
        }
        return false;
    }

    boolean temAncestralOcupado(int indice) {
        int pai = indice >> 1;
        while (pai >= 1) {
            if (estadoNo[pai] == OCUPADO) return true;
            pai = pai >> 1;
        }
        return false;
    }

    public int totalBytesLivres() {
        int soma = 0;
        int i = 1;
        int limite = TAM_ARVORE;
        while (i < limite) {
            if (estadoNo[i] == LIVRE) {
                if (!temAncestralLivre(i)) {
                    int depth = depthParaIndice(i);
                    int t = tamanhoBlocoParaDepth(depth);
                    soma = soma + t;
                }
            }
            i = i + 1;
        }
        return soma;
    }

    public int contarBlocosLivres() {
        int c = 0;
        int i = 1;
        int limite = TAM_ARVORE;
        while (i < limite) {
            if (estadoNo[i] == LIVRE) {
                if (!temAncestralLivre(i)) c = c + 1;
            }
            i = i + 1;
        }
        return c;
    }

    public void imprimirAlocados() {
        System.out.println("Blocos alocados:");
        int i = 1;
        int limite = TAM_ARVORE;
        while (i < limite) {
            if (estadoNo[i] == OCUPADO) {
                if (!temAncestralOcupado(i)) {
                    int depth = depthParaIndice(i);
                    int tamanhoBloco = tamanhoBlocoParaDepth(depth);
                    long offset = calcularOffsetDoNo(i, depth);

                    System.out.println(
                            "  " + rotuloNo[i] +
                                    " pedido=" + pedidoNo[i] + "B (" + (pedidoNo[i] / KB) + " KB)" +
                                    " bloco=" + tamanhoBloco + "B (" + (tamanhoBloco / KB) + " KB)" +
                                    " offset=" + offset + "B (" + (offset / KB) + " KB)"
                    );
                }
            }
            i = i + 1;
        }
    }

    public void imprimirLivres() {
        System.out.println("Blocos livres:");
        int i = 1;
        int limite = TAM_ARVORE;
        while (i < limite) {
            if (estadoNo[i] == LIVRE) {
                if (!temAncestralLivre(i)) {
                    int depth = depthParaIndice(i);
                    int tamanhoBloco = tamanhoBlocoParaDepth(depth);
                    long offset = calcularOffsetDoNo(i, depth);

                    System.out.println(
                            "  start=" + offset + "B (" + (offset / KB) + " KB)" +
                                    " size=" + tamanhoBloco + "B (" + (tamanhoBloco / KB) + " KB)"
                    );
                }
            }
            i = i + 1;
        }
    }

    long calcularOffsetDoNo(int indice, int depth) {
        int inicioNivel = 1 << depth;
        int pos = indice - inicioNivel;
        int tamanho = tamanhoBlocoParaDepth(depth);
        return (long) pos * (long) tamanho;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java BuddyAlocator <arquivo.txt>");
            return;
        }

        List<String> linhas = Files.readAllLines(Paths.get(args[0]));
        BuddyAlocator a = new BuddyAlocator();

        long ini = System.nanoTime();
        int cont = 0;

        for (String linha : linhas) {
            if (linha == null) continue;
            String l = linha.trim();
            if (l.length() == 0) continue;
            String[] p = l.split("\\s+");
            if (p.length < 2) continue;

            String rotulo = p[0];
            int tamanhoKB = Integer.parseInt(p[1]);
            int tamanhoBytes = tamanhoKB * KB;

            int nodo = a.alocar(rotulo, tamanhoBytes);
            if (nodo == -1) {
                System.out.println("Falha ao alocar " + rotulo);
            } else {
                int depth = a.depthParaIndice(nodo);
                int bloco = a.tamanhoBlocoParaDepth(depth);
                long off = a.calcularOffsetDoNo(nodo, depth);

                System.out.println(
                        "Alocado " + rotulo +
                                " bloco=" + bloco + "B (" + (bloco / KB) + " KB)" +
                                " offset=" + off + "B (" + (off / KB) + " KB)"
                );
            }
            cont = cont + 1;
        }

        long fim = System.nanoTime();
        double tempo = (fim - ini) / 1_000_000_000.0;

        System.out.println();
        System.out.println("Benchmark:");
        System.out.println("Processados: " + cont);
        System.out.println("Tempo: " + tempo + " s");

        System.out.println();
        System.out.println("Relatorio:");
        int livre = a.totalBytesLivres();

        System.out.println("Livre: " + livre + "B (" + (livre / KB) + " KB)");
        int blocos = a.contarBlocosLivres();
        System.out.println("Blocos livres: " + blocos);

        System.out.println();
        a.imprimirLivres();
        System.out.println();
        a.imprimirAlocados();
    }
}
