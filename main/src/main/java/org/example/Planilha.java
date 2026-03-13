package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Planilha {

    public static List<String> extrairCnpjs(String caminhoArquivo) {
        List<String> cnpjs = new ArrayList<>();
        int colunaDoCnpj = 3;       // Coluna D
        int colunaDoFaturamento = 4; // Coluna E

        try (FileInputStream fis = new FileInputStream(caminhoArquivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cellCnpj = row.getCell(colunaDoCnpj);
                    Cell cellFaturamento = row.getCell(colunaDoFaturamento);

                    String cnpj = cellCnpj != null ? formatter.formatCellValue(cellCnpj) : "";
                    String faturamentoAtual = cellFaturamento != null ? formatter.formatCellValue(cellFaturamento) : "";

                    // PULO DO GATO: Só adiciona na fila do robô se tiver CNPJ E o faturamento estiver VAZIO
                    if (!cnpj.trim().isEmpty() && faturamentoAtual.trim().isEmpty()) {
                        cnpjs.add(cnpj);
                    } else if (!faturamentoAtual.trim().isEmpty()) {
                        System.out.println("Pulando CNPJ " + cnpj + " (Já possui faturamento na planilha)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler planilha: " + e.getMessage());
        }
        return cnpjs;
    }

    public static void gravarFaturamentos(String caminhoArquivo, Map<String, String> dadosFaturamento) {
        int colunaDoCnpj = 3; // Coluna D , do cnpj
        int colunaParaSalvar = 4; // Coluna E , do faturamento

        try (FileInputStream fis = new FileInputStream(caminhoArquivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(colunaDoCnpj) != null) {
                    String cnpj = formatter.formatCellValue(row.getCell(colunaDoCnpj));

                    if (dadosFaturamento.containsKey(cnpj)) {
                        // Tenta pegar a célula existente, se não existir, cria uma nova
                        Cell cellFaturamento = row.getCell(colunaParaSalvar);
                        if (cellFaturamento == null) {
                            cellFaturamento = row.createCell(colunaParaSalvar);
                        }
                        cellFaturamento.setCellValue(dadosFaturamento.get(cnpj));
                    }
                }
            }

            // Grava o arquivo novo
            try (FileOutputStream fos = new FileOutputStream(caminhoArquivo.replace(".xlsx", "_enriquecido.xlsx"))) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            System.err.println("Erro ao gravar planilha: " + e.getMessage());
        }
    }
}