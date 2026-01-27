#!/bin/bash

OLLAMA_SSH_USER="ollama-client"
OLLAMA_SSH_HOST="srv-dpi-proj-ollmark-llm.univ-rouen.fr"
OLLAMA_LOCAL_PORT=11434

# Fonction pour tuer le tunnel existant s'il est mort
check_and_clean() {
    if lsof -Pi ":$OLLAMA_LOCAL_PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo " Port $OLLAMA_LOCAL_PORT occupé. Vérification de la connectivité..."
        if ! curl -m 2 -sf "http://127.0.0.1:$OLLAMA_LOCAL_PORT/api/tags" >/dev/null 2>&1; then
            echo "Le tunnel semble mort (pas de réponse de la VM). Nettoyage..."
            PID=$(lsof -Pi ":$OLLAMA_LOCAL_PORT" -sTCP:LISTEN -t)
            kill -9 "$PID"
            echo " Ancien tunnel (PID $PID) supprimé."
        else
            echo " Tunnel actif et fonctionnel."
            exit 0
        fi
    fi
}

check_and_clean

echo "Création du tunnel SSH vers VM..."

# -o ServerAliveInterval=15 : Envoie un signal toutes les 15 sec
# -o ServerAliveCountMax=3  : Si 3 signaux échouent (45 sec), SSH coupe tout seul
# -o ExitOnForwardFailure=yes : Si le port est pris, SSH quitte immédiatement (pas de zombie)
ssh -f -N \
    -o ServerAliveInterval=15 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -L "$OLLAMA_LOCAL_PORT:127.0.0.1:$OLLAMA_LOCAL_PORT" \
    "$OLLAMA_SSH_USER@$OLLAMA_SSH_HOST"

sleep 2

if curl -m 2 -sf "http://127.0.0.1:$OLLAMA_LOCAL_PORT/api/tags" >/dev/null 2>&1; then
    echo "Tunnel créé avec succès et VM accessible"
else
    echo "Échec : Le tunnel n'a pas pu s'établir."
    exit 1
fi