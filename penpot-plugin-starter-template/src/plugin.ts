penpot.ui.open("AI Code Generator", `?theme=${penpot.theme}`, {
  width: 600,
  height: 700
});

// Fonction pour exécuter le code JavaScript généré par l'IA
async function executeGeneratedCode(code: string): Promise<{success: boolean, error?: string}> {
  try {
    // Créer une fonction asynchrone à partir du code
    const asyncFunction = new Function('penpot', `
      return (async () => {
        ${code}
      })();
    `);
    
    // Exécuter le code avec l'objet penpot
    await asyncFunction(penpot);
    
    return { success: true };
  } catch (error) {
    console.error('Erreur lors de l\'exécution du code:', error);
    return { 
      success: false, 
      error: error instanceof Error ? error.message : 'Erreur inconnue' 
    };
  }
}

penpot.ui.onMessage<any>(async (message) => {
  if (message.type === 'execute-code') {
    const result = await executeGeneratedCode(message.code);
    penpot.ui.sendMessage({
      type: 'execution-result',
      result
    });
  }
});

// Update the theme in the iframe
penpot.on("themechange", (theme) => {
  penpot.ui.sendMessage({
    source: "penpot",
    type: "themechange",
    theme,
  });
});
