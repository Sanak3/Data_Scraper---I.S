package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class ScraperEconodata {

    private final Queue<Conta> filaContas = new LinkedList<>();

    // Método para você adicionar as contas na classe Main
    public void adicionarConta(String email, String senha) {
        filaContas.add(new Conta(email, senha));
    }

    public Map<String, String> buscarFaturamentos(List<String> cnpjs) {
        Map<String, String> resultados = new HashMap<>();
        WebDriver driver = null;
        Conta contaAtual = null;

        for (int i = 0; i < cnpjs.size(); i++) {
            String cnpj = cnpjs.get(i);

            // Se o navegador estiver fechado (início do programa ou após um erro), pega uma nova conta e abre
            if (driver == null) {
                contaAtual = filaContas.poll();
                if (contaAtual == null) {
                    System.out.println("Todas as contas foram esgotadas!");
                    break;
                }
                driver = iniciarNavegador(contaAtual);
            }

            try {
                System.out.println("Buscando CNPJ: " + cnpj + " usando conta: " + contaAtual.email);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

                // 1. Zera a tela indo sempre para a URL principal de busca a cada CNPJ
                driver.get("https://app.econodata.com.br/ferramentas/consulta-empresa");

                // 2. Preenche a barra de pesquisa
                // ATENÇÃO: Confirme o placeholder com F12 no navegador
                WebElement barraPesquisa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Digite o nome da empresa ou CNPJ']")));
                barraPesquisa.clear();
                barraPesquisa.sendKeys(cnpj);

                // 3. Clica na opção do Autocomplete (o menu flutuante que aparece com a empresa)
                WebElement opcaoAutocomplete = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '" + cnpj + "')]")));
                opcaoAutocomplete.click();

                // 4. Espera a ficha abrir e procura pelo label "Faturamento" na tela
                WebElement labelFaturamento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Faturamento')]")));

                // 5. Pega o valor que está no elemento imediatamente após a palavra "Faturamento"
                WebElement valorFaturamento = labelFaturamento.findElement(By.xpath("following-sibling::*"));

                String faturamentoExtraido = valorFaturamento.getText();
                resultados.put(cnpj, faturamentoExtraido);

                System.out.println("Sucesso! Faturamento: " + faturamentoExtraido);

                // Pausa de 3 segundos para não metralhar o servidor deles e dar como robozao
                Thread.sleep(3000);

            } catch (Exception e) {
                System.out.println("Erro ao buscar o CNPJ " + cnpj + " com a conta " + contaAtual.email + ". Trocando de conta...");
                if (driver != null) {
                    driver.quit(); // Fecha o navegador atual
                }
                driver = null; // Força a abertura de um novo na próxima repetição
                i--; // Decrementa o 'i' para ele tentar esse mesmo CNPJ de novo com a próxima conta
            }
        }

        if (driver != null) {
            driver.quit();
        }

        return resultados;
    }

    private WebDriver iniciarNavegador(Conta conta) {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // Mantenha comentado enquanto estiver testando e lidando com Captchas
        WebDriver driver = new ChromeDriver(options);

        try {
            // Vai direto para a ferramenta (onde tem o botão de login solto)
            driver.get("https://app.econodata.com.br/ferramentas/consulta-empresa");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Clica no botão "Entre" (O 'translate' ignora se está maiúsculo ou minúsculo)
            WebElement botaoEntre = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(translate(text(), 'ENTRE', 'entre'), 'entre')]")));
            botaoEntre.click();

            // Preenche o Modal de Login
            // ATENÇÃO: Confirme no F12 se os campos do modal usam name="email" e name="password"
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(conta.email);
            driver.findElement(By.name("password")).sendKeys(conta.senha);

            // Clica no botão Entrar do modal
            driver.findElement(By.xpath("//button[contains(translate(text(), 'ENTRAR', 'entrar'), 'entrar')]")).click();

            // Pausa de 15 segundos: Dá tempo para o modal fechar ou para você resolver um reCAPTCHA manualmente
            System.out.println("Login acionado. Aguardando 15s");
            Thread.sleep(15000);

        } catch (Exception e) {
            System.err.println("Erro no processo de login com " + conta.email + ": " + e.getMessage());
        }

        return driver;
    }

    // Classe auxiliar interna para guardar credenciais
    private static class Conta {
        String email;
        String senha;
        Conta(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }
}