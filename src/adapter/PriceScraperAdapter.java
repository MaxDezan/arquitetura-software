package adapter;

/**
 * Interface que define o contrato para buscar precos de produtos em lojas.
 * Permite que o CrawlerService seja testado com Mocks sem precisar
 * acessar a internet de verdade.
 */
public interface PriceScraperAdapter {

    /**
     * Busca o preco atual de um produto na URL informada.
     *
     * @param url       URL da pagina do produto na loja
     * @param storeName Nome da loja (usado para logs)
     * @return O preco encontrado, ou null se nao foi possivel extrair
     */
    Float fetchPrice(String url, String storeName);
}
