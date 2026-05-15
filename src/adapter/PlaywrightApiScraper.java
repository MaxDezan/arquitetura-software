package adapter;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementacao do PriceScraperAdapter usando o Playwright em modo API (CURL).
 * NAO abre nenhum navegador nem janela visual.
 * Faz requisicoes HTTP puras e extrai o preco do HTML retornado via Regex.
 */
public class PlaywrightApiScraper implements PriceScraperAdapter {

    // Padrao Regex para capturar valores monetarios no formato brasileiro
    // Exemplos que captura: R$ 3.699,00 | 3699,00 | R$3.699
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "R\\$\\s*([\\d.]+(?:,[\\d]{2})?)|([\\d]{1,3}(?:\\.[\\d]{3})*(?:,[\\d]{2}))"
    );

    @Override
    public Float fetchPrice(String url, String storeName) {
        // Playwright.create() inicia o motor em background, sem abrir janela
        try (Playwright playwright = Playwright.create()) {
            APIRequestContext request = playwright.request().newContext();

            System.out.println("-> Acessando " + storeName + ": " + url);

            APIResponse response = request.get(url);

            if (!response.ok()) {
                System.out.println("   [ERRO] Status " + response.status() + " em " + storeName);
                return null;
            }

            String html = response.text();
            Float preco = extrairPreco(html);

            if (preco == null) {
                System.out.println("   [AVISO] Preco nao encontrado no HTML de " + storeName);
            }

            return preco;

        } catch (Exception e) {
            System.out.println("   [ERRO] Falha ao acessar " + storeName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o primeiro valor monetario encontrado no HTML usando Regex.
     * Converte o formato brasileiro (3.699,00) para Float (3699.0).
     */
    private Float extrairPreco(String html) {
        Matcher matcher = PRICE_PATTERN.matcher(html);
        if (matcher.find()) {
            // Pega o grupo que capturou (grupo 1 com R$ ou grupo 2 sem)
            String valorBruto = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (valorBruto != null) {
                // Converte formato brasileiro: remove pontos de milhar, troca virgula por ponto
                String valorNormalizado = valorBruto.replace(".", "").replace(",", ".");
                try {
                    return Float.parseFloat(valorNormalizado);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
