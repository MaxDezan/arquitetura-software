package service;

import adapter.DatabaseStorage;
import adapter.PriceScraperAdapter;
import domain.Price;
import domain.Product;
import domain.ProductLink;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Servico central do crawler.
 * Percorre todos os produtos, busca o preco em cada loja cadastrada,
 * compara os resultados e salva o menor preco no historico.
 */
public class CrawlerService {

    private final PriceScraperAdapter scraper;
    private final DatabaseStorage<Product> armazenamento;

    public CrawlerService(PriceScraperAdapter scraper) {
        this.scraper = scraper;
        this.armazenamento = new DatabaseStorage<>(Product.class);
    }

    /**
     * Construtor alternativo para testes (permite injetar um storage mockado ou real).
     */
    public CrawlerService(PriceScraperAdapter scraper, DatabaseStorage<Product> armazenamento) {
        this.scraper = scraper;
        this.armazenamento = armazenamento;
    }

    /**
     * Executa o crawler para todos os produtos cadastrados no banco.
     */
    public void executar() {
        System.out.println("=== Iniciando execucao do Crawler ===");

        ArrayList<domain.EntityInterface> todos = armazenamento.listAll();

        if (todos.isEmpty()) {
            System.out.println("[INFO] Nenhum produto cadastrado. Encerrando.");
            return;
        }

        for (domain.EntityInterface entidade : todos) {
            if (entidade instanceof Product produto) {
                processarProduto(produto);
            }
        }

        System.out.println("=== Crawler finalizado ===");
    }

    /**
     * Processa um produto: busca o preco em todas as lojas e salva o menor.
     * Pode ser chamado diretamente nos testes passando um produto sem banco.
     */
    public void processarProduto(Product produto) {
        List<ProductLink> links = produto.getLinks();

        System.out.println("\nIniciando crawler para o produto: " + produto.getName());

        if (links == null || links.isEmpty()) {
            System.out.println("[AVISO] Produto '" + produto.getName() + "' nao possui links cadastrados. Pulando.");
            return;
        }

        Float menorPreco = null;
        String lojaDoMenorPreco = null;

        for (ProductLink link : links) {
            Float preco = scraper.fetchPrice(link.getUrl(), link.getStoreName());

            if (preco == null) {
                System.out.println("   [IGNORADO] " + link.getStoreName() + " retornou preco invalido.");
                continue;
            }

            System.out.println("-> Preco encontrado na " + link.getStoreName() + ": R$ " + preco);

            if (menorPreco == null || preco < menorPreco) {
                menorPreco = preco;
                lojaDoMenorPreco = link.getStoreName();
            }
        }

        if (menorPreco == null) {
            System.out.println("[AVISO] Nenhum preco valido encontrado para: " + produto.getName());
            return;
        }

        System.out.println("==> Menor preco atualizado: R$ " + menorPreco + " (" + lojaDoMenorPreco + ")");

        // Cria o novo preco com a loja e atualiza o produto
        // O metodo setPrice() ja cuida de mover o preco atual para o historico
        Price novoPreco = new Price(menorPreco, new Date(), lojaDoMenorPreco);
        produto.setPrice(novoPreco);

        // Persiste a alteracao no banco (apenas se o armazenamento estiver disponivel)
        if (armazenamento != null) {
            armazenamento.update(produto);
        }
    }
}
