package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;

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

                // 1. Vai direto para a URL da ferramenta
                driver.get("https://app.econodata.com.br/ferramentas/consulta-empresa");

                // 2. Preenche a barra de pesquisa
                WebElement barraPesquisa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Digite o nome da empresa ou CNPJ']")));
                barraPesquisa.clear();
                barraPesquisa.sendKeys(cnpj);

                // 3. Clica no botão azul "Pesquisar"
                WebElement botaoPesquisar = driver.findElement(By.xpath("//button[contains(., 'Pesquisar')]"));
                botaoPesquisar.click();

                // 4. Se o clique não for direto para a empresa e abrir uma lista/autocomplete
                WebElement opcaoAutocomplete = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '" + cnpj + "')]")));
                opcaoAutocomplete.click();

                // 5. Espera a ficha abrir e procura pelo label "Faturamento"
                WebElement labelFaturamento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Faturamento')]")));

                // 6. Pega o valor
                WebElement valorFaturamento = labelFaturamento.findElement(By.xpath("following-sibling::*"));

                String faturamentoExtraido = valorFaturamento.getText();
                resultados.put(cnpj, faturamentoExtraido);

                System.out.println("Sucesso! Faturamento: " + faturamentoExtraido);
                Thread.sleep(3000);

            } catch (Exception e) {
                System.out.println("Erro ao buscar o CNPJ " + cnpj + " com a conta " + contaAtual.email + ". Trocando de conta...");
                if (driver != null) {
                    driver.quit();
                }
                driver = null;
                i--;
            }
        }

        if (driver != null) {
            driver.quit();
        }

        return resultados;
    }

    private WebDriver iniciarNavegador(Conta conta) {
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://app.econodata.com.br/ferramentas/consulta-empresa");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // 1. Clica no botão "Entre"
            WebElement botaoEntre = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(translate(text(), 'ENTRE', 'entre'), 'entre')]")));
            botaoEntre.click();

            // 2. Preenche o E-mail
            WebElement campoEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Insira seu e-mail']")));
            campoEmail.sendKeys(conta.email);

            // 3. Clica em Continuar
            driver.findElement(By.xpath("//button[contains(text(), 'Continuar')]")).click();

            // 4. Espera o campo de Senha aparecer
            WebElement campoSenha = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@type='password']")));
            campoSenha.sendKeys(conta.senha);

            // 5. Aperta a tecla ENTER do teclado para confirmar a senha
            campoSenha.sendKeys(Keys.RETURN);

            System.out.println("Login preenchido. Aguardando 15s (Resolva o Captcha na tela se ele aparecer)...");
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