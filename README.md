# Monitoramento de Preços de Produtos

Sistema em Java que rastreia o preço de produtos em múltiplas lojas online, salva o histórico de preços e identifica onde o produto está mais barato.

---

## Tecnologias

- **Java 25** (com `--enable-preview`)
- **Hibernate 6 + JPA** — persistência com banco SQLite
- **Playwright (modo API/CURL)** — requisições HTTP puras, sem abrir navegador
- **JUnit 5 + Mockito** — testes automatizados

---

## Pré-requisitos

- Java 25 instalado em `C:\Program Files\Java\jdk-25.0.3`
- IntelliJ IDEA (o Maven embutido é utilizado nos comandos abaixo)

---

## Como compilar

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd" compile
```

---

## Como rodar os testes

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

Resultado esperado:

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os 6 cenários testados são:
1. Identifica o menor preço entre Amazon e Kabum
2. Funciona quando a Amazon é mais barata
3. Acumula histórico após múltiplas execuções do crawler
4. Não falha quando o produto não possui links
5. Ignora link quebrado e usa a loja válida
6. Não altera o preço se todas as lojas retornarem erro

---

## Como rodar a aplicação

```powershell
.\run_app.ps1
```

---

## Como usar o Crawler

### Passo 1 — Cadastrar um produto com links (faça isso uma vez)

No arquivo `src/Main.java`, descomente o bloco do **PASSO 1**:

```java
ProductLink linkAmazon = new ProductLink("Amazon", "https://www.amazon.com.br/...");
ProductLink linkKabum  = new ProductLink("Kabum",  "https://www.kabum.com.br/...");

Product ps5 = new Product(
    "SKU-PS5",
    "PlayStation 5",
    new Price(4000f, new Date()),
    new ArrayList<>(List.of(linkAmazon, linkKabum))
);
productService.save(ps5);
```

Rode a aplicação. Após salvar, **comente o bloco novamente** para não duplicar o produto.

### Passo 2 — Executar o Crawler

Com o produto já cadastrado, o **PASSO 3** do `Main.java` já está ativo:

```java
CrawlerService crawler = new CrawlerService(new PlaywrightApiScraper());
crawler.executar();
```

O crawler vai:
1. Buscar o preço em cada loja via requisição HTTP (sem abrir navegador)
2. Comparar os preços encontrados
3. Salvar o **menor preço** no histórico, junto com o nome da loja

### Exemplo de saída esperada no console

```
=== Produtos cadastrados ===
Product { sku='SKU-PS5', name='PlayStation 5', ... }

=== Iniciando execucao do Crawler ===

Iniciando crawler para o produto: PlayStation 5
-> Acessando Amazon: https://www.amazon.com.br/...
-> Preco encontrado na Amazon: R$ 3799.0
-> Acessando Kabum: https://www.kabum.com.br/...
-> Preco encontrado na Kabum: R$ 3699.0
==> Menor preco atualizado: R$ 3699.0 (Kabum)

=== Crawler finalizado ===

=== Produtos apos execucao do Crawler ===
Product { sku='SKU-PS5', name='PlayStation 5', price=3699.0 @ ... (Kabum), ... }
```

---

## Estrutura do projeto

```
src/
├── Main.java                        # Ponto de entrada
├── domain/
│   ├── Product.java                 # Entidade produto
│   ├── Price.java                   # Entidade preco (com nome da loja)
│   ├── ProductLink.java             # Entidade link de loja
│   └── EntityInterface.java
├── service/
│   ├── CrawlerService.java          # Logica central do crawler
│   ├── ProductService.java
│   ├── PriceService.java
│   └── BaseService.java
├── adapter/
│   ├── PriceScraperAdapter.java     # Interface do scraper (testavel)
│   ├── PlaywrightApiScraper.java    # Implementacao com Playwright (modo CURL)
│   └── DatabaseStorage.java
└── test/
    └── CrawlerServiceTest.java      # 6 testes unitarios com Mockito
```