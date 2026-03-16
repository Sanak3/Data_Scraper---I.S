# Data_Scraper---I.S

Criado para automatizar o processo de coleta de dados de empresas. 
Este robô lê uma lista de CNPJs em uma planilha Excel, navega automaticamente na plataforma de busca, lida com autenticação via *Magic Link* e extrai o faturamento anual, gerando uma planilha atualizada e enriquecida no final.

## 🛠️ Tecnologias Utilizadas
* **Java**
* **Selenium WebDriver:** Para automação e controle do navegador (Chrome).
* **Apache POI:** Para leitura e gravação inteligente dos dados no Excel.

## Como Configurar

1. **Planilha de Origem:** O robô espera que os CNPJs estejam na **Coluna D** e o Faturamento na **Coluna E**. 
   Na classe `Main.java`, atualize a variável com o caminho completo do seu arquivo `.xlsx`.
   
2. **Adicionando Contas:**
   A plataforma de consulta consome créditos por pesquisa. Cadastre os e-mails que serão utilizados na classe `Main.java` através do comando:
   `scraper.adicionarConta("seu_email@dominio.com", "senha");`

## Como Usar

1. Execute a classe `Main`.
2. O Chrome abrirá automaticamente e iniciará o fluxo de login digitando o primeiro e-mail da fila.
3. **Autenticação (Magic Link):** O script fará uma pausa e abrirá uma janela pop-up na sua tela.
4. Vá até a caixa de entrada do e-mail utilizado, clique com o botão direito no link de confirmação, copie e **cole o link na janela do pop-up**.
5. Dê "OK" e deixe o robô trabalhar. Ele fará todo o processo de rolagem de tela, quebra de banners de cookies e cliques necessários para revelar os dados.
6. Ao finalizar a fila (ou esgotar os créditos das contas), o sistema criará um novo arquivo com o sufixo `_enriquecido.xlsx` na mesma pasta do arquivo original, contendo todos os dados coletados.

## 💡 Dicas Importantes

* **E-mails Temporários:** Recomendo fortemente usar e-mails temporários (como YOPmail, Temp-Mail, etc.) para criar as contas, por serem mais fáceis e eficientes na hora de gerenciar as permissões e receber os links mágicos rapidamente.
* **Economia de Créditos:** O robô é inteligente. Se o arquivo de origem já tiver algum faturamento preenchido na Coluna E para um determinado CNPJ, ele pula essa linha automaticamente para não gastar créditos à toa.
* **Caminho do Arquivo:** Coloque o caminho do arquivo corretamente no código para que ele consiga encontrar a planilha original e criar o arquivo pronto com os dados novos. *(Nota para usuários de Mac/Linux: não esqueçam da `/` no início do caminho absoluto!)*
