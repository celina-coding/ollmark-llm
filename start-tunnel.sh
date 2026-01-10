#!/bin/bash

OLLAMA_SSH_USER="ollama-client"
OLLAMA_SSH_HOST="srv-dpi-proj-ollmark-llm.univ-rouen.fr"
OLLAMA_LOCAL_PORT=11434

# Vérifier si le tunnel existe déjà
if lsof -Pi ":$OLLAMA_LOCAL_PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "✓ Tunnel SSH déjà actif sur le port $OLLAMA_LOCAL_PORT"
    exit 0
fi

echo "🔌 Création du tunnel SSH vers Ollama..."
ssh -f -N -L "$OLLAMA_LOCAL_PORT:127.0.0.1:$OLLAMA_LOCAL_PORT" \
    "$OLLAMA_SSH_USER@$OLLAMA_SSH_HOST"

sleep 2

# Vérifier
if curl -sf "http://127.0.0.1:$OLLAMA_LOCAL_PORT/api/tags" >/dev/null 2>&1; then
    echo "✅ Tunnel créé avec succès et Ollama accessible"
else
    echo "❌ Échec : Ollama non accessible"
    exit 1
fi