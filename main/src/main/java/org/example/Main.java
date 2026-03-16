package org.example;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String caminhoPlanilha = "CAMINHO DO ARQUIVO";

        System.out.println("Iniciando extração de dados...");

        List<String> cnpjs = Planilha.extrairCnpjs(caminhoPlanilha);
        System.out.println("Encontrados " + cnpjs.size() + " CNPJs.");

        // 2. Configura as contas do robozao, so colocar a conta criada no econodata aqui, o email somente, esque
        ScraperEconodata scraper = new ScraperEconodata();
        scraper.adicionarConta("seuemail@email.com" ,  "senha_4");



        Map<String, String> faturamentos = scraper.buscarFaturamentos(cnpjs);

        Planilha.gravarFaturamentos(caminhoPlanilha, faturamentos);

        System.out.println("Processo finalizado com sucesso!");
    }
}