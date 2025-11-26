#!/system/bin/sh

LOGFILE="/sdcard/monitor_llm.log"
APP_CACHE="/data/data/com.example.notificationapp/cache"
APP_FILES="/data/data/com.example.notificationapp/files"
APP_LOGS="/data/data/com.example.notificationapp/logs"
APP_LOGFILE="$APP_LOGS/llm.log"

echo "=== Monitor de Performance LLM (Android) ===" > $LOGFILE
echo "Inicio: $(date)" >> $LOGFILE

while true
do
    echo "-----------------------------" >> $LOGFILE
    echo "Timestamp: $(date)" >> $LOGFILE

    # Temperatura dos sensores
    echo "" >> $LOGFILE
    echo ">> TEMPERATURAS" >> $LOGFILE
    for z in /sys/class/thermal/thermal_zone*/temp; do
        ZONE=$(echo $z | sed 's/.*thermal_zone//;s/\/temp//')
        TYPE=$(cat /sys/class/thermal/thermal_zone$ZONE/type 2>/dev/null)
        TEMP=$(cat $z 2>/dev/null)
        echo "Zona $ZONE ($TYPE): $TEMP" >> $LOGFILE
    done

    # CPU
    echo "" >> $LOGFILE
    echo ">> CPU (top -n 1 - resumo)" >> $LOGFILE
    top -n 1 -b | head -n 10 >> $LOGFILE

    # Memória
    echo "" >> $LOGFILE
    echo ">> MEMORIA (/proc/meminfo)" >> $LOGFILE
    head -n 10 /proc/meminfo >> $LOGFILE

    # Load average
    echo "" >> $LOGFILE
    echo ">> LOAD AVERAGE" >> $LOGFILE
    uptime >> $LOGFILE

    # Throttling
    echo "" >> $LOGFILE
    echo ">> THERMAL THROTTLING" >> $LOGFILE
    dumpsys thermalservice | grep -i thrott >> $LOGFILE

    # Uso de armazenamento do app
    echo "" >> $LOGFILE
    echo ">> USO DE ARMAZENAMENTO DO APP (LLM)" >> $LOGFILE
    if [ -d "$APP_CACHE" ]; then
        echo "Cache:" >> $LOGFILE
        du -sh $APP_CACHE >> $LOGFILE
    fi
    if [ -d "$APP_FILES" ]; then
        echo "Files:" >> $LOGFILE
        du -sh $APP_FILES >> $LOGFILE
    fi
    if [ -d "$APP_LOGS" ]; then
        echo "Logs:" >> $LOGFILE
        du -sh $APP_LOGS >> $LOGFILE
    fi

    # Espaço livre em /data
    echo "" >> $LOGFILE
    echo "Espaço livre em /data:" >> $LOGFILE
    df -h /data >> $LOGFILE

    # Consumo de bateria
    echo "" >> $LOGFILE
    echo ">> CONSUMO DE BATERIA" >> $LOGFILE
    dumpsys battery >> $LOGFILE

    # Logs do app (para latência, velocidade de tokens, estabilidade)
    echo "" >> $LOGFILE
    echo ">> LOGS DO APP (últimas 10 linhas)" >> $LOGFILE
    if [ -f "$APP_LOGFILE" ]; then
        tail -n 10 "$APP_LOGFILE" >> $LOGFILE
    fi

    echo "" >> $LOGFILE
    sleep 1
done
