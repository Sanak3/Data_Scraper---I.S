package org.example;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String caminhoPlanilha = "IS_Faturamento.xlsx";

        System.out.println("Iniciando extração de dados...");

        List<String> cnpjs = Planilha.extrairCnpjs(caminhoPlanilha);
        System.out.println("Encontrados " + cnpjs.size() + " CNPJs.");

        // 2. Configura as contas do robozao
        ScraperEconodata scraper = new ScraperEconodata();
        scraper.adicionarConta("seu_email_1@gmail.com", "senha_1");
        scraper.adicionarConta("seu_email_2@gmail.com", "senha_2");
        scraper.adicionarConta("seu_email_3@gmail.com", "senha_3");

        Map<String, String> faturamentos = scraper.buscarFaturamentos(cnpjs);

        Planilha.gravarFaturamentos(caminhoPlanilha, faturamentos);

        System.out.println("Processo finalizado com sucesso!");
    }
}