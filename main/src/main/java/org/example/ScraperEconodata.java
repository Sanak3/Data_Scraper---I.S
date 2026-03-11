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

    public void adicionarConta(String email, String senha) {
        filaContas.add(new Conta(email, senha));
    }

    public Map<String, String> buscarFaturamentos(List<String> cnpjs) {
        Map<String, String> resultados = new HashMap<>();
        WebDriver driver = null;
        Conta contaAtual = null;

        for (int i = 0; i < cnpjs.size(); i++) {
            String cnpj = cnpjs.get(i);

            if (driver == null) {
                contaAtual = filaContas.poll(); // Pega a próxima conta disponível
                if (contaAtual == null) {
                    System.out.println("Todas as contas foram esgotadas!");
                    break;
                }
                driver = iniciarNavegador(contaAtual);
            }

            try {
                System.out.println("Buscando CNPJ: " + cnpj + " usando conta: " + contaAtual.email);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

                // Substitua essa URL pela que você descobriu
                driver.get("https://www.econodata.com.br/consulta-empresa");

                // 2. Preenche a barra de pesquisa
                WebElement barraPesquisa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Digite o CNPJ ou Razão Social']"))); // Ajuste o placeholder se precisar
                barraPesquisa.clear();
                barraPesquisa.sendKeys(cnpj);

                // 3. Clica na opção do Autocomplete (o menu flutuante com o nome da empresa)
                WebElement opcaoAutocomplete = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '" + cnpj + "')]")));
                opcaoAutocomplete.click();

                // 4. Espera a ficha abrir e procura pelo label "Faturamento"
                WebElement labelFaturamento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Faturamento')]")));

                // 5. Pega o valor que está logo ao lado/abaixo da palavra Faturamento
                WebElement valorFaturamento = labelFaturamento.findElement(By.xpath("following-sibling::*"));

                String faturamentoExtraido = valorFaturamento.getText();
                resultados.put(cnpj, faturamentoExtraido);

                System.out.println("Sucesso! Faturamento encontrado: " + faturamentoExtraido);

                // Pausa para o sistema não achar que você é um robô metralhando requisições
                Thread.sleep(3000);

            } catch (Exception e) {
                System.out.println("Erro na conta " + contaAtual.email + " ou limite atingido. Trocando de conta...");
                driver.quit(); // Fecha o navegador atual
                driver = null; // Força a abertura de um novo na próxima repetição do loop
                i--; // Decrementa o i para tentar o MESMO CNPJ de novo com a nova conta
            }
        }

        if (driver != null) {
            driver.quit();
        }

        return resultados;
    }

    private WebDriver iniciarNavegador(Conta conta) {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // Descomente depois de testar para rodar invisível
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://app.econodata.com.br/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Login
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys(conta.email);
            driver.findElement(By.id("password")).sendKeys(conta.senha);
            driver.findElement(By.xpath("//button[contains(text(), 'Entrar')]")).click();

            // Pausa estratégica. Se tiver Captcha, você tem 15s para resolver manualmente no navegador
            System.out.println("Aguardando login concluir");
            Thread.sleep(10000);

        } catch (Exception e) {
            System.err.println("Erro ao tentar fazer login com " + conta.email);
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