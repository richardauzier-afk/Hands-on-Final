package devtitans.litertlib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
public class LiteRTLib {
    /**
     * Recebe texto de notificações e retorna um resumo gerado por IA local.
     *
     * @param inputText Texto das notificações concatenadas para resumir
     * @return Resumo inteligente das notificações ou mensagem de erro
     */
    public String computePiValue(String inputText) {
        // Limita o tamanho do texto para não estourar o contexto do modelo
        if (inputText.length() > 2000) {
            inputText = inputText.substring(0, 2000) + "...";
        }
        // Cria o prompt para o modelo
        String prompt =
        "Você é um assistente de IA especializado em resumir notificações de usuários. "
        + "Resuma as seguintes notificações em português de forma clara e concisa. "
        + "Mantenha o resumo curto (máximo 30 palavras), "
        + "não use emojis e retorne APENAS o texto resumido.\n\n"
        + "NOTIFICAÇÕES:\n"
        + inputText + "\n\n"
        + "RESUMO:";

        String[] commandToExecute = {
            "./litert_lm_main",
            "--backend=cpu",
            "--model_path=gemma-3n-E2B-it-int4.litertlm",
            "--input_prompt=" + prompt,
            "--max_num_tokens=200",  // Resumo curto
            "--num_cpu_threads=4"
        };
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandToExecute);
            processBuilder.directory(new File("/data/local/temp"));
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            // Timeout de 2 minutos para resumos
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                return "⏱️ Processamento demorou muito. Notificações não resumidas.";
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                );
                output.append("\n--- ERRO (código: ").append(exitCode).append(") ---\n");
                while ((line = errorReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return "❌ Erro ao processar resumo.";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Erro: " + e.getMessage();
        }
        String result = output.toString().trim();
        return result.isEmpty() ? "📝 Sem resumo disponível" : result;
    }
    /**
     * Versão sem parâmetros (para compatibilidade com código antigo)
     * Retorna mensagem indicando que precisa de texto
     */
    public String computePiValue() {
        return "⚠️ Nenhum texto fornecido para resumir.";
    }
}
