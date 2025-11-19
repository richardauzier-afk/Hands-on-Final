import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class HelloJavaLib {

    /**
     * ================================================
     *   🔧 PROMPT INSTRUCTIONS FOR GEMMA-2B (EDITABLE)
     * ================================================
     *
     *  ROLE:
     *    "You are an AI assistant specialized in summarizing notifications."
     *
     *  TASK:
     *    "Summarize the content clearly, concisely, and in natural English."
     *
     *  STYLE & RULES:
     *      • Keep the summary short (max 5 lines)
     *      • Use simple vocabulary
     *      • Keep only the essential information
     *      • Remove duplicates or repeated details
     *      • Do NOT invent or assume missing information
     *      • Avoid emojis, symbols, or markdown
     *      • Output ONLY the summary text (no explanations)
     *
     *  SPECIAL CASES:
     *      • If the input is empty → return: "No notifications to summarize."
     *
     *  These instructions are integrated into the final prompt below.
     */

    /**
     * Summarizes notification text using a local LLM (Gemma 2B).
     */
    public String computePiValue(String inputText) {

        // Safety: limit text size for Gemma-2B context window
        if (inputText.length() > 2000) {
            inputText = inputText.substring(0, 2000) + "...";
        }

        // =====================================
        // 🔥 FINAL PROMPT SENT TO GEMMA-2B
        // =====================================
        String prompt =
                "You are an AI assistant specialized in summarizing user notifications. "
                + "Summarize the following notifications in clear and concise English. "
                + "Keep the summary short (max 5 lines), avoid emojis, avoid adding new information, "
                + "and output ONLY the summary.\n\n"
                + "NOTIFICATIONS:\n"
                + inputText + "\n\n"
                + "SUMMARY:";

        String[] commandToExecute = {
            "./litert_lm_main",
            "--backend=cpu",
            "-model_path=gemma-3n-E2B-it-int4.litertlm",
            "--input_prompt=" + prompt,
            "--max_num_tokens=200",
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

            // Timeout (Gemma 2B can be slower on CPU)
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                return "⏱️ Timeout. Summary could not be generated.";
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                );

                output.append("\n--- ERROR (code ").append(exitCode).append(") ---\n");

                while ((line = errorReader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                return "❌ Error while generating summary.";
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }

        String result = output.toString().trim();
        return result.isEmpty() ? "No summary available." : result;
    }

    /**
     * Fallback version with no input
     */
    public String computePiValue() {
        return "⚠️ No text provided for summarization.";
    }
}

