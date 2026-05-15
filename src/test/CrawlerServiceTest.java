package test;

import adapter.PriceScraperAdapter;
import domain.Price;
import domain.Product;
import domain.ProductLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import service.CrawlerService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Testes unitarios do CrawlerService.
 * O Playwright NAO e inicializado aqui. Usamos Mockito para simular o scraper.
 */
class CrawlerServiceTest {

    // Scraper "falso" criado pelo Mockito - simula respostas sem acessar a internet
    private PriceScraperAdapter scraperMock;
    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        // Cria o mock do scraper antes de cada teste
        scraperMock = Mockito.mock(PriceScraperAdapter.class);
        // Injetamos o mock e passamos null para o storage (nao salvaremos no banco nos testes)
        crawlerService = new CrawlerService(scraperMock, null);
    }

    // =========================================================================
    // CENARIO 1: Funcionamento normal com 2 lojas
    // =========================================================================

    @Test
    @DisplayName("Deve identificar o menor preco entre Amazon e Kabum")
    void deveIdentificarMenorPreco() {
        // Configura os links do produto
        ProductLink linkAmazon = new ProductLink("Amazon", "https://amazon.com.br/ps5");
        ProductLink linkKabum = new ProductLink("Kabum", "https://kabum.com.br/ps5");
        List<ProductLink> links = List.of(linkAmazon, linkKabum);

        Product produto = new Product("SKU-PS5", "PlayStation 5", new Price(4000f, new Date()));
        produto.setLinks(new ArrayList<>(links));

        // Ensina o scraper falso a retornar precos fixos
        when(scraperMock.fetchPrice("https://amazon.com.br/ps5", "Amazon")).thenReturn(3799.00f);
        when(scraperMock.fetchPrice("https://kabum.com.br/ps5", "Kabum")).thenReturn(3699.00f);

        // Executa o crawler
        crawlerService.processarProduto(produto);

        // Verifica se o menor preco foi salvo corretamente
        assertNotNull(produto.getPrice(), "O preco atual nao deve ser nulo");
        assertEquals(3699.00f, produto.getPrice().getPrice(), 0.01f, "Deve salvar o menor preco (Kabum)");
        assertEquals("Kabum", produto.getPrice().getStoreName(), "Deve salvar o nome da loja correta");
    }

    @Test
    @DisplayName("Deve escolher o menor preco mesmo quando Amazon e mais barata")
    void deveEscolherAmazonQuandoForMaisBarata() {
        ProductLink linkAmazon = new ProductLink("Amazon", "https://amazon.com.br/notebook");
        ProductLink linkMagalu = new ProductLink("Magalu", "https://magalu.com.br/notebook");

        Product produto = new Product("SKU-NB", "Notebook", new Price(5000f, new Date()));
        produto.setLinks(new ArrayList<>(List.of(linkAmazon, linkMagalu)));

        when(scraperMock.fetchPrice("https://amazon.com.br/notebook", "Amazon")).thenReturn(4200.00f);
        when(scraperMock.fetchPrice("https://magalu.com.br/notebook", "Magalu")).thenReturn(4500.00f);

        crawlerService.processarProduto(produto);

        assertEquals(4200.00f, produto.getPrice().getPrice(), 0.01f);
        assertEquals("Amazon", produto.getPrice().getStoreName());
    }

    // =========================================================================
    // CENARIO 2: Acumulo de historico apos multiplas execucoes
    // =========================================================================

    @Test
    @DisplayName("Deve acumular historico de precos a cada execucao do crawler")
    void deveAcumularHistoricoDePrecos() {
        ProductLink link = new ProductLink("Kabum", "https://kabum.com.br/ps5");
        Product produto = new Product("SKU-PS5", "PlayStation 5", new Price(4000f, new Date()));
        produto.setLinks(new ArrayList<>(List.of(link)));

        // Primeira execucao: preco cai para 3699
        when(scraperMock.fetchPrice(anyString(), anyString())).thenReturn(3699.00f);
        crawlerService.processarProduto(produto);

        // Segundo execucao: preco cai ainda mais para 3499
        when(scraperMock.fetchPrice(anyString(), anyString())).thenReturn(3499.00f);
        crawlerService.processarProduto(produto);

        // O historico deve ter 2 entradas (o preco inicial + o preco da 1a execucao)
        assertEquals(2, produto.getHistoricalPrice().size(),
                "Historico deve ter 2 registros apos 2 execucoes");

        // O preco atual deve ser o da ultima execucao
        assertEquals(3499.00f, produto.getPrice().getPrice(), 0.01f,
                "Preco atual deve ser o mais recente");
    }

    // =========================================================================
    // CENARIO 3: Adverso - produto sem links cadastrados
    // =========================================================================

    @Test
    @DisplayName("Nao deve falhar quando o produto nao possui links")
    void naoDeveFalharSemLinks() {
        Product produto = new Product("SKU-001", "Produto Sem Links", new Price(100f, new Date()));
        // produto.getLinks() retornara lista vazia por padrao

        // Nao deve lancar excecao
        assertDoesNotThrow(() -> crawlerService.processarProduto(produto),
                "O crawler nao deve quebrar para produtos sem links");

        // O scraper nao deve ter sido chamado
        verify(scraperMock, never()).fetchPrice(anyString(), anyString());
    }

    // =========================================================================
    // CENARIO 4: Adverso - link quebrado retornando null
    // =========================================================================

    @Test
    @DisplayName("Deve ignorar lojas com link quebrado e usar a loja valida")
    void deveIgnorarLinkQuebrado() {
        ProductLink linkQuebrado = new ProductLink("Amazon", "https://amazon.com.br/produto-404");
        ProductLink linkValido = new ProductLink("Kabum", "https://kabum.com.br/produto-ok");

        Product produto = new Product("SKU-002", "Produto Teste", new Price(500f, new Date()));
        produto.setLinks(new ArrayList<>(List.of(linkQuebrado, linkValido)));

        // Amazon retorna null (link quebrado / produto fora de estoque)
        when(scraperMock.fetchPrice("https://amazon.com.br/produto-404", "Amazon")).thenReturn(null);
        when(scraperMock.fetchPrice("https://kabum.com.br/produto-ok", "Kabum")).thenReturn(450.00f);

        // Nao deve lancar excecao
        assertDoesNotThrow(() -> crawlerService.processarProduto(produto));

        // Deve usar o preco da loja valida
        assertEquals(450.00f, produto.getPrice().getPrice(), 0.01f,
                "Deve usar o preco da unica loja disponivel");
        assertEquals("Kabum", produto.getPrice().getStoreName());
    }

    @Test
    @DisplayName("Nao deve atualizar o preco se todos os links retornarem null")
    void naoDeveAtualizarSeNenhumaLojaResponder() {
        ProductLink link1 = new ProductLink("Amazon", "https://amazon.com.br/produto");
        ProductLink link2 = new ProductLink("Kabum", "https://kabum.com.br/produto");

        Price precoOriginal = new Price(999f, new Date(), "Loja Original");
        Product produto = new Product("SKU-003", "Produto Sem Resposta", precoOriginal);
        produto.setLinks(new ArrayList<>(List.of(link1, link2)));

        // Todas as lojas retornam null
        when(scraperMock.fetchPrice(anyString(), anyString())).thenReturn(null);

        crawlerService.processarProduto(produto);

        // O preco original nao deve ter sido alterado
        assertEquals(999f, produto.getPrice().getPrice(), 0.01f,
                "Preco nao deve mudar se nenhuma loja responder");
        assertTrue(produto.getHistoricalPrice().isEmpty(),
                "Historico deve continuar vazio se nao houve atualizacao");
    }
}
