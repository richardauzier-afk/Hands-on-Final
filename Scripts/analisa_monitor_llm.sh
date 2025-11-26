#!/system/bin/sh

LOGFILE="/sdcard/monitor_llm.log"
RESULTS="/sdcard/monitor_llm_resultado.txt"
APP_LOGFILE="/data/data/com.example.notificationapp/logs/llm.log"

echo "=== RESUMO DO MONITOR LLM ===" > $RESULTS

# Temperaturas
echo "" >> $RESULTS
echo ">> Temperaturas (máxima, mínima, média por zona):" >> $RESULTS
grep "Zona" $LOGFILE | awk -F': ' '{print $2}' | awk '{print $NF/1000}' | \
awk '
    {zone[NR]=$0}
    END {
        max=-99999; min=99999; sum=0; count=0;
        for (i=1;i<=NR;i++) {
            if (zone[i]>max) max=zone[i];
            if (zone[i]<min) min=zone[i];
            sum+=zone[i]; count++;
        }
        if (count>0) printf "Máx: %.1f°C, Mín: %.1f°C, Média: %.1f°C\n", max, min, sum/count;
    }
' >> $RESULTS

# Load Average
echo "" >> $RESULTS
echo ">> Load Average (máximo, mínimo, média):" >> $RESULTS
grep "load average" $LOGFILE | awk -F'load average: ' '{print $2}' | awk '{print $1}' | \
awk '
    {la[NR]=$0}
    END {
        max=-99999; min=99999; sum=0; count=0;
        for (i=1;i<=NR;i++) {
            if (la[i]>max) max=la[i];
            if (la[i]<min) min=la[i];
            sum+=la[i]; count++;
        }
        if (count>0) printf "Máx: %.2f, Mín: %.2f, Média: %.2f\n", max, min, sum/count;
    }
' >> $RESULTS

# Memória livre
echo "" >> $RESULTS
echo ">> Memória Livre (máxima, mínima, média):" >> $RESULTS
grep "MemFree" $LOGFILE | awk '{print $2}' | \
awk '
    {mem[NR]=$0}
    END {
        max=-99999; min=99999; sum=0; count=0;
        for (i=1;i<=NR;i++) {
            if (mem[i]>max) max=mem[i];
            if (mem[i]<min) min=mem[i];
            sum+=mem[i]; count++;
        }
        if (count>0) printf "Máx: %d kB, Mín: %d kB, Média: %.0f kB\n", max, min, sum/count;
    }
' >> $RESULTS

# Espaço livre em /data
echo "" >> $RESULTS
echo ">> Espaço livre em /data (última medição):" >> $RESULTS
grep "/data" $LOGFILE | tail -1 >> $RESULTS

# Consumo de bateria
echo "" >> $RESULTS
echo ">> Consumo de bateria (primeira e última medição):" >> $RESULTS
grep "level:" $LOGFILE | head -1 >> $RESULTS
grep "level:" $LOGFILE | tail -1 >> $RESULTS

# Throttling
echo "" >> $RESULTS
echo ">> Ocorrência de throttling:" >> $RESULTS
grep -i "thrott" $LOGFILE | sort | uniq >> $RESULTS

# Uso de armazenamento do app
echo "" >> $RESULTS
echo ">> Uso de armazenamento do app (última medição):" >> $RESULTS
grep "Cache:" $LOGFILE | tail -1 >> $RESULTS
grep "Files:" $LOGFILE | tail -1 >> $RESULTS
grep "Logs:" $LOGFILE | tail -1 >> $RESULTS

# Latência e velocidade de tokens (se registrado nos logs do app)
echo "" >> $RESULTS
echo ">> Latência e velocidade de tokens (últimas 5 medições):" >> $RESULTS
if [ -f "$APP_LOGFILE" ]; then
    grep -i "lat" "$APP_LOGFILE" | tail -5 >> $RESULTS
    grep -i "token" "$APP_LOGFILE" | tail -5 >> $RESULTS
else
    echo "Log do app não encontrado." >> $RESULTS
fi

# Estabilidade (erros/falhas nos logs do app)
echo "" >> $RESULTS
echo ">> Erros/falhas no processamento (últimas 5 ocorrências):" >> $RESULTS
if [ -f "$APP_LOGFILE" ]; then
    grep -i "error\|fail" "$APP_LOGFILE" | tail -5 >> $RESULTS
else
    echo "Log do app não encontrado." >> $RESULTS
fi

echo "" >> $RESULTS
echo ">> Fim da análise." >> $RESULTS
