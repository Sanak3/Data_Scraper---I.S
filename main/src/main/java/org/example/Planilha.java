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
        try (FileInputStream fis = new FileInputStream(caminhoArquivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Pula a linha cabeçalho
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(4) != null) {
                    String cnpj = row.getCell(4).getStringCellValue();
                    cnpjs.add(cnpj);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler planilha: " + e.getMessage());
        }
        return cnpjs;
    }

    public static void gravarFaturamentos(String caminhoArquivo, Map<String, String> dadosFaturamento) {
        try (FileInputStream fis = new FileInputStream(caminhoArquivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    String cnpj = row.getCell(4).getStringCellValue();

                    if (dadosFaturamento.containsKey(cnpj)) {
                        Cell cellFaturamento = row.createCell(5);
                        cellFaturamento.setCellValue(dadosFaturamento.get(cnpj));
                    }
                }
            }

            // Grava em um arquivo novo para não corromper o original
            try (FileOutputStream fos = new FileOutputStream(caminhoArquivo.replace(".xlsx", "IS_fat2.xlsx"))) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            System.err.println("Erro ao gravar planilha: " + e.getMessage());
        }
    }
}