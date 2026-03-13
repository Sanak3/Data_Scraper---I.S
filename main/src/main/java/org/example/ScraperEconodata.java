package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
        // A senha não será mais usada no site, mas mantemos o formato para não quebrar a sua Main
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

                // 1. Zera a tela na busca
                driver.get("https://www.econodata.com.br/consulta-empresa");

                // 2. Digita e clica em pesquisar
                WebElement barraPesquisa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Digite o nome da empresa ou CNPJ']")));
                barraPesquisa.clear();
                barraPesquisa.sendKeys(cnpj);
                driver.findElement(By.xpath("//button[contains(., 'Pesquisar')]")).click();

                // 3. Desce a tela levemente para o resultado aparecer bem (Evita que o rodapé cubra o clique)
                Thread.sleep(1500);
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");

                // 4. Clica na empresa nos resultados (procura o CNPJ na tela)
                WebElement linkEmpresa = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '" + cnpj + "')]")));
                linkEmpresa.click();

                // 5. Espera a página da empresa carregar e acha o "Faturamento Anual"
                WebElement labelFaturamento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Faturamento Anual')]")));

                // 6. Clica no PRIMEIRO botão Desbloquear (aquele abaixo do Faturamento Anual)
                WebElement primeiroBotaoDesbloquear = labelFaturamento.findElement(By.xpath("following::button[contains(., 'Desbloquear')][1]"));
                primeiroBotaoDesbloquear.click();

                // 7. Espera o modal do cachorro aparecer contendo o aviso de 1 crédito
                WebElement avisoCredito = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'consumir 1 crédito')]")));

                // 8. Clica no SEGUNDO botão Desbloquear dentro do modal
                WebElement segundoBotaoDesbloquear = avisoCredito.findElement(By.xpath("following::button[contains(., 'Desbloquear')]"));
                segundoBotaoDesbloquear.click();

                // 9. Espera a animação rodar e o texto "R$" aparecer no lugar do botão
                Thread.sleep(2000);
                WebElement valorFaturamento = labelFaturamento.findElement(By.xpath("following::*[contains(text(), 'R$')][1]"));

                String faturamentoExtraido = valorFaturamento.getText();
                resultados.put(cnpj, faturamentoExtraido);

                System.out.println("Sucesso! Faturamento: " + faturamentoExtraido);
                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("Erro ao buscar o CNPJ " + cnpj + ". Trocando de conta ou falha na extração.");
                if (driver != null) {
                    driver.quit();
                }
                driver = null;
                i--; // Tenta o mesmo CNPJ com a próxima conta
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
            driver.get("https://www.econodata.com.br/consulta-empresa");
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            Thread.sleep(2000);

            // Clica em Entre
            WebElement botaoEntre = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Entre')] | //button[contains(text(), 'Entre')] | //*[text()='Entre']")));
            botaoEntre.click();

            // Digita E-mail e clica em Continuar
            WebElement campoEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Insira seu e-mail']")));
            campoEmail.sendKeys(conta.email);
            driver.findElement(By.xpath("//button[contains(text(), 'Continuar')]")).click();

            //sistema pra esperar logar
            System.out.println("=========================================================");
            System.out.println("O Link foi enviado para: " + conta.email);
            System.out.println("Corre no email ai e confirma, tem 20 segundos");
            System.out.println("=========================================================");

            // Pausa de 20 segundos
            Thread.sleep(20000);

            System.out.println("Retomando a automação...");

        } catch (Exception e) {
            System.err.println("Erro no processo de login com " + conta.email + ": " + e.getMessage());
        }

        return driver;
    }

    private static class Conta {
        String email;
        String senha; //mantive por preguica de refazer a main
        Conta(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }
}