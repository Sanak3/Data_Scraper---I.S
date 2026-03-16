package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.JOptionPane;
import java.time.Duration;
import java.util.*;

public class ScraperEconodata {

    private final Queue<Conta> filaContas = new LinkedList<>();

    public void adicionarConta(String email, String senha) {
        filaContas.add(new Conta(email, senha));
        // a senha não será mais usada no site, mas mantive o formato para não quebrar a main
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

                // se falhar miseravelmente no login, pula pra proxima conta
                if (driver == null) {
                    i--;
                    continue;
                }
            }

            try {
                System.out.println("Buscando CNPJ: " + cnpj + " usando conta: " + contaAtual.email);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

                // 1. zera a tela na busca
                driver.get("https://www.econodata.com.br/consulta-empresa");
                Thread.sleep(2000); // respiro pra rede

                // limpa o banner de cookies se ele estiver na tela
                fecharBannerCookies(driver);

                // 2. digita e clica em pesquisar
                WebElement barraPesquisa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Digite o nome da empresa ou CNPJ']")));
                barraPesquisa.clear();
                barraPesquisa.sendKeys(cnpj);
                Thread.sleep(1000); // respiro após digitar
                driver.findElement(By.xpath("//button[contains(., 'Pesquisar')]")).click();

                // 3. desce a tela levemente
                Thread.sleep(2000); // respiro pra busca carregar
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");

                // 4. acha o resultado e força o clique via javascript
                System.out.println("aguardando resultado da busca aparecer...");
                WebElement linkEmpresa = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), '" + cnpj + "')]/following::a[1]")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", linkEmpresa);

                // 5. espera a página da empresa carregar
                System.out.println("entrando na ficha da empresa...");
                Thread.sleep(2000); // respiro pra carregar a ficha inteira
                WebElement primeiroBotaoDesbloquear = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("(//button[contains(., 'Desbloquear')])[1]")));

                // mais uma garantia de que cookies não voltaram a atrapalhar
                fecharBannerCookies(driver);

                // 6. rola a tela para o botão ficar exatamente no meio e clica fisicamente
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", primeiroBotaoDesbloquear);
                Thread.sleep(2000); // respiro após rolar a tela
                System.out.println("clicando no primeiro botão de desbloqueio...");
                primeiroBotaoDesbloquear.click();

                // 7. espera o modal do cachorro
                System.out.println("aguardando modal do cachorrinho...");
                WebElement tituloModal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Liberar os dados')]")));
                Thread.sleep(2000); // respiro pro modal estabilizar na tela

                // 8. acha o botão desbloquear abaixo do título e clica com js
                System.out.println("confirmando o uso do crédito...");
                WebElement segundoBotaoDesbloquear = tituloModal.findElement(By.xpath("following::button[contains(., 'Desbloquear')]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", segundoBotaoDesbloquear);

                // 9. espera ativamente pelo valor "r$" aparecer (sem chutar o tempo, espera ele ficar visível)
                System.out.println("aguardando a rede processar e o valor ser revelado...");
                WebElement valorFaturamento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Faturamento Anual')]/following::*[contains(text(), 'R$')][1]")));

                String faturamentoExtraido = valorFaturamento.getText();
                resultados.put(cnpj, faturamentoExtraido);

                System.out.println("sucesso! faturamento: " + faturamentoExtraido);
                Thread.sleep(2000); // respiro final antes de ir pro próximo cnpj

            } catch (Exception e) {
                System.out.println("erro ao processar o cnpj " + cnpj);
                System.out.println("motivo exato do erro: " + e.getMessage());
                if (driver != null) {
                    driver.quit();
                }
                driver = null;
                i--; // tenta o mesmo cnpj com a próxima conta
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

            // clica em entre
            WebElement botaoEntre = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Entre') and (self::button or self::a)] | //button[contains(text(), 'Entre')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botaoEntre);

            // digita e-mail
            WebElement campoEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Insira seu e-mail' or @type='email']")));
            campoEmail.sendKeys(conta.email);

            // clica em continuar
            WebElement botaoContinuar = driver.findElement(By.xpath("//button[contains(text(), 'Continuar')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botaoContinuar);

            System.out.println("esperando a tela do envelope carregar...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Aguardando confirmação') or contains(text(), 'Que bom te ver')]")));
            Thread.sleep(1500);

            System.out.println("=========================================================");
            System.out.println("o link foi enviado para: " + conta.email);
            System.out.println("aguardando você colar o link na janelinha que abriu...");
            System.out.println("=========================================================");

            String linkMagico = JOptionPane.showInputDialog(
                    null,
                    "o e-mail foi enviado para: " + conta.email + "\n\n1. vá no seu e-mail e copie o link do botão.\n2. cole o link abaixo e clique em ok:",
                    "aguardando login",
                    JOptionPane.WARNING_MESSAGE
            );

            if (linkMagico == null || linkMagico.trim().isEmpty()) {
                throw new Exception("operação cancelada ou link vazio inserido na caixa.");
            }

            System.out.println("link recebido! injetando login...");
            driver.get(linkMagico.trim());

            Thread.sleep(5000);

            fecharBannerCookies(driver);

            System.out.println("retomando a automação...");

        } catch (Exception e) {
            System.err.println("erro crítico no processo de login com " + conta.email + ": " + e.getMessage());
            if (driver != null) {
                driver.quit();
            }
            return null;
        }

        return driver;
    }

    private void fecharBannerCookies(WebDriver driver) {
        try {
            List<WebElement> botoesCookie = driver.findElements(By.xpath("//button[contains(translate(text(), 'ACEIT', 'aceit'), 'aceit') or contains(translate(text(), 'ENTEND', 'entend'), 'entend') or contains(translate(text(), 'CONCORD', 'concord'), 'concord') or contains(translate(text(), 'OK', 'ok'), 'ok')]"));
            for (WebElement btn : botoesCookie) {
                if (btn.isDisplayed()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                    Thread.sleep(1500);
                    break;
                }
            }
        } catch (Exception ignore) {
        }
    }

    private static class Conta {
        String email;
        String senha;
        // mantive por preguica de refazer a main
        Conta(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }
}