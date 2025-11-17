package br.edu.ufam.icomp.devtitans.hellojavalib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.util.Log;

public class HelloJavaLib {

    private static final String TAG = "HelloJavaLib";

    // Instruções de sistema simplificadas para não estourar o contexto
    private static final String SYSTEM_INSTRUCTIONS =
        "Você é um assistente que resume notificações Android em português do Brasil de forma clara e concisa.\n" +
        "Agrupe notificações similares e destaque as mais importantes.\n" +
        "Seja breve e objetivo (máximo 150 palavras).\n\n";

    /**
     * Recebe texto de notificações e retorna um resumo gerado por IA local.
     */
    public String computePiValue(String inputText) {
        Log.d(TAG, "=== INICIANDO PROCESSAMENTO ===");

        // Valida entrada
        if (inputText == null || inputText.trim().isEmpty()) {
            return "⚠️ Nenhum texto fornecido para resumir.";
        }

        // Limita o tamanho do texto
        String processedInput = inputText;
        if (inputText.length() > 1500) {
            processedInput = inputText.substring(0, 1500) + "...";
            Log.d(TAG, "Texto truncado de " + inputText.length() + " para 1500 caracteres");
        }

        // Prompt simplificado
        String fullPrompt = SYSTEM_INSTRUCTIONS +
            "Notificações:\n" + processedInput +
            "\n\nResumo:";

        Log.d(TAG, "Tamanho do prompt: " + fullPrompt.length() + " caracteres");

        // Verifica se o diretório existe
        File workDir = new File("/data/local/tmp");
        if (!workDir.exists()) {
            Log.e(TAG, "Diretório /data/local/tmp não existe!");
            return "❌ Erro: Diretório de trabalho não encontrado.";
        }

        // Verifica se o executável existe
        File executable = new File(workDir, "litert_lm_main");
        if (!executable.exists()) {
            Log.e(TAG, "Executável litert_lm_main não encontrado em " + workDir.getAbsolutePath());
            return "❌ Erro: Modelo de IA não encontrado.";
        }

        // Verifica se o modelo existe
        File modelFile = new File(workDir, "gemma-3n-E2B-it-int4.litertlm");
        if (!modelFile.exists()) {
            Log.e(TAG, "Arquivo do modelo não encontrado: " + modelFile.getAbsolutePath());
            return "❌ Erro: Arquivo do modelo não encontrado.";
        }

        Log.d(TAG, "Arquivos verificados com sucesso");

        // Comando simplificado
        String[] commandToExecute = {
            "./litert_lm_main",
            "--backend=cpu",
            "--model_path=gemma-3n-E2B-it-int4.litertlm",
            "--input_prompt=" + fullPrompt,
            "--max_num_tokens=150",
            "--num_cpu_threads=2"
        };

        Log.d(TAG, "Comando: " + String.join(" ", commandToExecute));

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandToExecute);
            processBuilder.directory(workDir);

            Log.d(TAG, "Iniciando processo...");
            Process process = processBuilder.start();

            // Lê a saída padrão
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            // Lê a saída de erro em paralelo
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                Log.d(TAG, "OUTPUT: " + line);
            }

            // Lê erros
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
                Log.e(TAG, "ERROR: " + line);
            }

            Log.d(TAG, "Aguardando conclusão do processo...");

            // Timeout de 90 segundos
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                Log.e(TAG, "Timeout: processo foi terminado forçadamente");
                return "⏱️ Processamento demorou muito (>90s). Tente com menos notificações.";
            }

            int exitCode = process.exitValue();
            Log.d(TAG, "Processo finalizado com código: " + exitCode);

            if (exitCode != 0) {
                String errorMsg = "❌ Erro ao processar (código " + exitCode + ")";
                if (errorOutput.length() > 0) {
                    Log.e(TAG, "Detalhes do erro: " + errorOutput.toString());
                    errorMsg += "\nDetalhes: " + errorOutput.toString().substring(0,
                        Math.min(200, errorOutput.length()));
                }
                return errorMsg;
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage(), e);
            return "❌ Erro de I/O: " + e.getMessage();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            return "❌ Processo interrompido: " + e.getMessage();
        }

        String result = output.toString().trim();
        Log.d(TAG, "Resultado (tamanho: " + result.length() + "): " + result);

        if (result.isEmpty()) {
            return "📝 Modelo não gerou resposta. Verifique os logs.";
        }

        // Limpa a saída
        result = cleanModelOutput(result);

        return result;
    }

    /**
     * Limpa a saída do modelo
     */
    private String cleanModelOutput(String output) {
        if (output == null || output.isEmpty()) return "";

        // Remove possíveis tags técnicas
        output = output.replaceAll("<\\|.*?\\|>", "");
        output = output.replaceAll("###.*", "");
        output = output.replaceAll("\\[.*?\\]", "");

        // Remove linhas vazias excessivas
        output = output.replaceAll("\n{3,}", "\n\n");

        // Remove espaços excessivos
        output = output.replaceAll(" {2,}", " ");

        return output.trim();
    }

    /**
     * Versão sem parâmetros
     */
    public String computePiValue() {
        return "⚠️ Nenhum texto fornecido para resumir.";
    }

    /**
     * Método de diagnóstico
     */
    public String diagnose() {
        StringBuilder diag = new StringBuilder("=== DIAGNÓSTICO ===\n");

        File workDir = new File("/data/local/tmp");
        diag.append("Diretório existe: ").append(workDir.exists()).append("\n");

        if (workDir.exists()) {
            File[] files = workDir.listFiles();
            diag.append("Arquivos no diretório:\n");
            if (files != null) {
                for (File f : files) {
                    diag.append("  - ").append(f.getName()).append("\n");
                }
            }
        }

        File executable = new File(workDir, "litert_lm_main");
        diag.append("Executável existe: ").append(executable.exists()).append("\n");
        diag.append("Executável pode executar: ").append(executable.canExecute()).append("\n");

        File modelFile = new File(workDir, "gemma-3n-E2B-it-int4.litertlm");
        diag.append("Modelo existe: ").append(modelFile.exists()).append("\n");
        if (modelFile.exists()) {
            diag.append("Tamanho do modelo: ").append(modelFile.length() / 1024 / 1024).append(" MB\n");
        }

        return diag.toString();
    }

    /**
     * Teste simples
     */
    public String testSimple() {
        return computePiValue("WhatsApp - Maria: Olá, tudo bem?");
    }
}
